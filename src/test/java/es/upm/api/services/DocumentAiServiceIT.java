package es.upm.api.services;

import es.upm.api.data.daos.DocumentRepository;
import es.upm.api.data.daos.InvoiceRepository;
import es.upm.api.data.entities.Document;
import es.upm.api.data.entities.Invoice;
import es.upm.api.infrastructure.clients.AwsTextractClient;
import es.upm.api.infrastructure.clients.OpenAiClassifierClient;
import es.upm.api.infrastructure.clients.S3CloudClient;
import es.upm.api.infrastructure.clients.TextractExtractionException;
import es.upm.api.infrastructure.support.FileDownloader;
import es.upm.api.infrastructure.support.PdfExtractor;
import es.upm.api.exceptions.BadRequestException;
import es.upm.api.exceptions.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ActiveProfiles("test")
class DocumentAiServiceIT {

        @Autowired
        private DocumentAiService documentAiService;

        @Autowired
        private DocumentRepository documentRepository;

        @Autowired
        private InvoiceRepository invoiceRepository;

        @MockitoBean
        private S3CloudClient s3CloudClient;

        @MockitoBean
        private PdfExtractor pdfExtractor;

        @MockitoBean
        private OpenAiClassifierClient openAiClassifierClient;

        @MockitoBean
        private AwsTextractClient awsTextractClient;

        @MockitoBean
        private FileDownloader fileDownloader;

        @AfterEach
        void tearDown() {
                this.invoiceRepository.deleteAll();
                this.documentRepository.deleteAll();
        }

        @Test
        void testUploadDocument() {
                BDDMockito.given(this.s3CloudClient.uploadFile(any()))
                                .willReturn("https://mock-bucket.s3.amazonaws.com/test-file.pdf");

                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test-file.pdf",
                                "application/pdf",
                                "mock content".getBytes());

                Document savedDoc = this.documentAiService.uploadDocument(file, false);

                assertThat(savedDoc).isNotNull();
                assertThat(savedDoc.getId()).isNotNull();
                assertThat(savedDoc.getName()).isEqualTo("test-file.pdf");
                assertThat(savedDoc.getUrl()).isEqualTo("https://mock-bucket.s3.amazonaws.com/test-file.pdf");
                assertThat(savedDoc.getCategory()).isNull();

                // Verify it was actually saved in DB
                Document dbDoc = this.documentRepository.findById(savedDoc.getId()).orElse(null);
                assertThat(dbDoc).isNotNull();
                assertThat(dbDoc.getName()).isEqualTo("test-file.pdf");
        }

        @Test
        void testUploadDocumentWithAutoclassify() {
                BDDMockito.given(this.s3CloudClient.uploadFile(any()))
                                .willReturn("https://mock-bucket.s3.amazonaws.com/test-file.pdf");
                BDDMockito.given(this.pdfExtractor.extractTextFromPdf(any()))
                                .willReturn("contract text");
                BDDMockito.given(this.openAiClassifierClient.classifyText(any()))
                                .willReturn(es.upm.api.data.entities.DocumentCategory.CONTRACT);

                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test-file.pdf",
                                "application/pdf",
                                "mock content".getBytes());

                Document savedDoc = this.documentAiService.uploadDocument(file, true);

                assertThat(savedDoc).isNotNull();
                assertThat(savedDoc.getCategory()).isEqualTo(es.upm.api.data.entities.DocumentCategory.CONTRACT);
        }

        @Test
        void testUploadDocumentInvalidType() {
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test-file.txt",
                                "text/plain",
                                "mock content".getBytes());

                assertThrows(BadRequestException.class, () -> this.documentAiService.uploadDocument(file, false));
        }

        @Test
        void testSummarizeDocumentFlow() {
                Document doc = Document.builder()
                                .name("test.pdf")
                                .url("https://url-fake.com/doc.pdf")
                                .build();
                Document saved = documentRepository.save(doc);

                BDDMockito.given(pdfExtractor.extractTextFromUrl(any()))
                                .willReturn("Texto extraído");
                BDDMockito.given(openAiClassifierClient.summarizeText("Texto extraído"))
                                .willReturn("Resumen final");

                Document result = documentAiService.summarizeDocument(saved.getId());

                assertThat(result.getSummary()).isEqualTo("Resumen final");

                assertThat(documentRepository.findById(saved.getId()).get().getSummary())
                                .isEqualTo("Resumen final");
        }

        @Test
        void testSummarizeDocumentNotFound() {
                NotFoundException exception = assertThrows(
                                NotFoundException.class,
                                () -> this.documentAiService.summarizeDocument("non-existent-id")
                );
                assertThat(exception.getMessage()).contains("Document not found");
        }

        @Test
        void testExtractInvoiceSuccess() throws IOException {
                String url = "https://mock-invoices.com/invoice-001.pdf";
                byte[] mockBytes = "mock pdf content".getBytes();

                BDDMockito.doReturn(mockBytes).when(this.fileDownloader).downloadFile(url);

                Invoice mockExtracted = Invoice.builder()
                                .vendorName("ACME Corp")
                                .invoiceDate("2026-06-21")
                                .invoiceId("INV-987")
                                .total("150.00")
                                .taxAmount("21.00")
                                .currency("EUR")
                                .lineItems(List.of(es.upm.api.data.entities.LineItem.builder()
                                                .name("Consulting Services")
                                                .quantity("1")
                                                .unitPrice("123.97")
                                                .price("123.97")
                                                .build()))
                                .build();

                BDDMockito.given(this.awsTextractClient.extractInvoice(mockBytes))
                                .willReturn(mockExtracted);

                // First extraction should hit downloader and AWS Client
                // Create a Document with URL and save to DB, then call by documentId
                Document doc = Document.builder().url(url).build();
                Document saved = this.documentRepository.save(doc);
                Invoice result = this.documentAiService.extractInvoice(saved.getId());

                assertThat(result).isNotNull();
                assertThat(result.getVendorName()).isEqualTo("ACME Corp");
                assertThat(result.getInvoiceDate()).isEqualTo("2026-06-21");
                assertThat(result.getInvoiceId()).isEqualTo("INV-987");
                assertThat(result.getTotal()).isEqualTo("150.00");
                assertThat(result.getTaxAmount()).isEqualTo("21.00");
                assertThat(result.getCurrency()).isEqualTo("EUR");
                assertThat(result.getLineItems()).hasSize(1);
                assertThat(result.getLineItems().get(0).getName()).isEqualTo("Consulting Services");
                assertThat(result.getLineItems().get(0).getQuantity()).isEqualTo("1");

                // Verify it was stored in Mongo by documentId
                Optional<Invoice> savedInvoice = this.invoiceRepository.findByDocumentId(result.getDocumentId());
                assertThat(savedInvoice).isPresent();

                // Second extraction should hit cache/repository (mock call count should not
                // increase)
                Invoice cachedResult = this.documentAiService.extractInvoice(result.getDocumentId());
                assertThat(cachedResult.getId()).isEqualTo(result.getId());

                BDDMockito.verify(this.fileDownloader, BDDMockito.times(1)).downloadFile(url);
        }

        @Test
        void testExtractInvoiceDownloadFailure() throws IOException {
                String url = "https://mock-invoices.com/missing.pdf";
                BDDMockito.doThrow(new IOException("connection failed")).when(this.fileDownloader).downloadFile(url);

                // For download failure, create document and call by id
                Document missingDoc = Document.builder().url(url).build();
                Document savedMissing = this.documentRepository.save(missingDoc);
                assertThrows(BadRequestException.class, () -> this.documentAiService.extractInvoice(savedMissing.getId()));
        }

        @Test
        void testExtractInvoiceTextractFailure() throws IOException {
                String url = "https://mock-invoices.com/invoice-error.pdf";
                byte[] mockBytes = "mock pdf content".getBytes();

                BDDMockito.doReturn(mockBytes).when(this.fileDownloader).downloadFile(url);
                BDDMockito.given(this.awsTextractClient.extractInvoice(mockBytes))
                                .willThrow(new TextractExtractionException(
                                                "Error al procesar la factura con AWS Textract: AWS SDK Error"));

                Document errorDoc = Document.builder().url(url).build();
                Document savedError = this.documentRepository.save(errorDoc);
                assertThrows(BadRequestException.class, () -> this.documentAiService.extractInvoice(savedError.getId()));
        }

        @Test
        void testExtractInvoiceDocumentNotFound() {
                String nonExistentId = "non-existent-document-id";
                NotFoundException exception = assertThrows(
                                NotFoundException.class,
                                () -> this.documentAiService.extractInvoice(nonExistentId)
                );
                assertThat(exception.getMessage()).contains("Document not found");
        }

        @Test
        void testExtractInvoiceEmptyExpenseDocuments() throws IOException {
                String url = "https://mock-invoices.com/empty-invoice.pdf";
                byte[] mockBytes = "mock pdf content".getBytes();

                Document doc = Document.builder().url(url).build();
                Document saved = this.documentRepository.save(doc);

                BDDMockito.doReturn(mockBytes).when(this.fileDownloader).downloadFile(url);

                BDDMockito.given(this.awsTextractClient.extractInvoice(mockBytes))
                                .willThrow(new TextractExtractionException("No se pudo extraer información de la factura"));

                assertThrows(BadRequestException.class, () -> this.documentAiService.extractInvoice(saved.getId()));
        }

        @Test
        void testExtractInvoiceWithMultipleLineItems() throws IOException {
                String url = "https://mock-invoices.com/multi-items.pdf";
                byte[] mockBytes = "mock pdf content".getBytes();

                Document doc = Document.builder().url(url).build();
                Document saved = this.documentRepository.save(doc);

                BDDMockito.doReturn(mockBytes).when(this.fileDownloader).downloadFile(url);

                Invoice mockExtracted = Invoice.builder()
                                .vendorName("TechCorp")
                                .total("500.00")
                                .currency("EUR")
                                .lineItems(List.of(
                                                es.upm.api.data.entities.LineItem.builder()
                                                                .name("Software License")
                                                                .quantity("1")
                                                                .unitPrice("250.00")
                                                                .build(),
                                                es.upm.api.data.entities.LineItem.builder()
                                                                .name("Support Services")
                                                                .quantity("2")
                                                                .price("250.00")
                                                                .build()))
                                .build();

                BDDMockito.given(this.awsTextractClient.extractInvoice(mockBytes))
                                .willReturn(mockExtracted);

                Invoice result = this.documentAiService.extractInvoice(saved.getId());

                assertThat(result).isNotNull();
                assertThat(result.getVendorName()).isEqualTo("TechCorp");
                assertThat(result.getTotal()).isEqualTo("500.00");
                assertThat(result.getLineItems()).hasSize(2);
                assertThat(result.getLineItems().get(0).getName()).isEqualTo("Software License");
                assertThat(result.getLineItems().get(1).getName()).isEqualTo("Support Services");
        }

        @Test
        void testExtractInvoiceWithAllFields() throws IOException {
                String url = "https://mock-invoices.com/complete-invoice.pdf";
                byte[] mockBytes = "mock pdf content".getBytes();

                Document doc = Document.builder().url(url).build();
                Document saved = this.documentRepository.save(doc);

                BDDMockito.doReturn(mockBytes).when(this.fileDownloader).downloadFile(url);

                Invoice mockExtracted = Invoice.builder()
                                .vendorName("Global Supplies Inc")
                                .invoiceDate("2026-06-15")
                                .invoiceId("INV-2026-0815")
                                .dueDate("2026-07-15")
                                .receiverName("Our Company Ltd")
                                .receiverTaxId("ES12345678A")
                                .subtotal("800.00")
                                .taxAmount("168.00")
                                .total("968.00")
                                .currency("EUR")
                                .build();

                BDDMockito.given(this.awsTextractClient.extractInvoice(mockBytes))
                                .willReturn(mockExtracted);

                Invoice result = this.documentAiService.extractInvoice(saved.getId());

                assertThat(result).isNotNull();
                assertThat(result.getVendorName()).isEqualTo("Global Supplies Inc");
                assertThat(result.getInvoiceDate()).isEqualTo("2026-06-15");
                assertThat(result.getInvoiceId()).isEqualTo("INV-2026-0815");
                assertThat(result.getDueDate()).isEqualTo("2026-07-15");
                assertThat(result.getReceiverName()).isEqualTo("Our Company Ltd");
                assertThat(result.getReceiverTaxId()).isEqualTo("ES12345678A");
                assertThat(result.getSubtotal()).isEqualTo("800.00");
                assertThat(result.getTaxAmount()).isEqualTo("168.00");
                assertThat(result.getTotal()).isEqualTo("968.00");
                assertThat(result.getCurrency()).isEqualTo("EUR");
        }

        @Test
        void testExtractInvoiceCachingBehavior() throws IOException {
                String url = "https://mock-invoices.com/cached-invoice.pdf";
                byte[] mockBytes = "mock pdf content".getBytes();

                Document doc = Document.builder().url(url).build();
                Document saved = this.documentRepository.save(doc);

                BDDMockito.doReturn(mockBytes).when(this.fileDownloader).downloadFile(url);

                Invoice mockExtracted = Invoice.builder()
                                .vendorName("Cached Corp")
                                .total("99.99")
                                .currency("USD")
                                .build();

                BDDMockito.given(this.awsTextractClient.extractInvoice(mockBytes))
                                .willReturn(mockExtracted);

                // First call - should hit AWS
                Invoice firstResult = this.documentAiService.extractInvoice(saved.getId());
                assertThat(firstResult.getVendorName()).isEqualTo("Cached Corp");

                // Second call - should be cached
                Invoice secondResult = this.documentAiService.extractInvoice(saved.getId());
                assertThat(secondResult.getId()).isEqualTo(firstResult.getId());
                assertThat(secondResult.getVendorName()).isEqualTo("Cached Corp");

                // Verify AWS was only called once
                BDDMockito.verify(this.awsTextractClient, BDDMockito.times(1)).extractInvoice(mockBytes);
        }
}
