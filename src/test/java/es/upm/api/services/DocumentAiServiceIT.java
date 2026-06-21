package es.upm.api.services;

import es.upm.api.data.daos.DocumentRepository;
import es.upm.api.data.daos.InvoiceRepository;
import es.upm.api.data.entities.Document;
import es.upm.api.data.entities.Invoice;
import es.upm.api.infrastructure.clients.AwsTextractClient;
import es.upm.api.infrastructure.clients.OpenAiClassifierClient;
import es.upm.api.infrastructure.clients.S3CloudClient;
import es.upm.api.infrastructure.support.FileDownloader;
import es.upm.api.infrastructure.support.PdfExtractor;
import es.upm.api.services.exceptions.BadRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.textract.model.*;

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
        void testExtractInvoiceSuccess() {
                String url = "https://mock-invoices.com/invoice-001.pdf";
                byte[] mockBytes = "mock pdf content".getBytes();

                BDDMockito.given(this.fileDownloader.downloadFile(url))
                                .willReturn(mockBytes);

                AnalyzeExpenseResponse mockResponse = AnalyzeExpenseResponse.builder()
                                .expenseDocuments(ExpenseDocument.builder()
                                                .summaryFields(
                                                                ExpenseField.builder()
                                                                                .type(ExpenseType.builder()
                                                                                                .text("VENDOR_NAME")
                                                                                                .build())
                                                                                .valueDetection(ExpenseDetection
                                                                                                .builder()
                                                                                                .text("ACME Corp")
                                                                                                .build())
                                                                                .build(),
                                                                ExpenseField.builder()
                                                                                .type(ExpenseType.builder().text(
                                                                                                "INVOICE_RECEIPT_DATE")
                                                                                                .build())
                                                                                .valueDetection(ExpenseDetection
                                                                                                .builder()
                                                                                                .text("2026-06-21")
                                                                                                .build())
                                                                                .build(),
                                                                ExpenseField.builder()
                                                                                .type(ExpenseType.builder().text(
                                                                                                "INVOICE_RECEIPT_ID")
                                                                                                .build())
                                                                                .valueDetection(ExpenseDetection
                                                                                                .builder()
                                                                                                .text("INV-987")
                                                                                                .build())
                                                                                .build(),
                                                                ExpenseField.builder()
                                                                                .type(ExpenseType.builder()
                                                                                                .text("TOTAL").build())
                                                                                .valueDetection(ExpenseDetection
                                                                                                .builder()
                                                                                                .text("150.00").build())
                                                                                .currency(ExpenseCurrency.builder()
                                                                                                .code("EUR").build())
                                                                                .build(),
                                                                ExpenseField.builder()
                                                                                .type(ExpenseType.builder().text("TAX")
                                                                                                .build())
                                                                                .valueDetection(ExpenseDetection
                                                                                                .builder().text("21.00")
                                                                                                .build())
                                                                                .build())
                                                .lineItemGroups(
                                                                LineItemGroup.builder()
                                                                                .lineItems(
                                                                                                LineItemFields.builder()
                                                                                                                .lineItemExpenseFields(
                                                                                                                                ExpenseField.builder()
                                                                                                                                                .type(ExpenseType
                                                                                                                                                                .builder()
                                                                                                                                                                .text("ITEM")
                                                                                                                                                                .build())
                                                                                                                                                .valueDetection(ExpenseDetection
                                                                                                                                                                .builder()
                                                                                                                                                                .text("Consulting Services")
                                                                                                                                                                .build())
                                                                                                                                                .build(),
                                                                                                                                ExpenseField.builder()
                                                                                                                                                .type(ExpenseType
                                                                                                                                                                .builder()
                                                                                                                                                                .text("QUANTITY")
                                                                                                                                                                .build())
                                                                                                                                                .valueDetection(ExpenseDetection
                                                                                                                                                                .builder()
                                                                                                                                                                .text("1")
                                                                                                                                                                .build())
                                                                                                                                                .build(),
                                                                                                                                ExpenseField.builder()
                                                                                                                                                .type(ExpenseType
                                                                                                                                                                .builder()
                                                                                                                                                                .text("UNIT_PRICE")
                                                                                                                                                                .build())
                                                                                                                                                .valueDetection(ExpenseDetection
                                                                                                                                                                .builder()
                                                                                                                                                                .text("123.97")
                                                                                                                                                                .build())
                                                                                                                                                .build(),
                                                                                                                                ExpenseField.builder()
                                                                                                                                                .type(ExpenseType
                                                                                                                                                                .builder()
                                                                                                                                                                .text("PRICE")
                                                                                                                                                                .build())
                                                                                                                                                .valueDetection(ExpenseDetection
                                                                                                                                                                .builder()
                                                                                                                                                                .text("123.97")
                                                                                                                                                                .build())
                                                                                                                                                .build())
                                                                                                                .build())
                                                                                .build())
                                                .build())
                                .build();

                BDDMockito.given(this.awsTextractClient.analyzeExpense(mockBytes))
                                .willReturn(mockResponse);

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
        void testExtractInvoiceDownloadFailure() {
                String url = "https://mock-invoices.com/missing.pdf";
                BDDMockito.given(this.fileDownloader.downloadFile(url))
                                .willThrow(new BadRequestException("Error al descargar el archivo desde la URL"));

                // For download failure, create document and call by id
                Document missingDoc = Document.builder().url(url).build();
                Document savedMissing = this.documentRepository.save(missingDoc);
                assertThrows(BadRequestException.class, () -> this.documentAiService.extractInvoice(savedMissing.getId()));
        }

        @Test
        void testExtractInvoiceTextractFailure() {
                String url = "https://mock-invoices.com/invoice-error.pdf";
                byte[] mockBytes = "mock pdf content".getBytes();

                BDDMockito.given(this.fileDownloader.downloadFile(url))
                                .willReturn(mockBytes);
                BDDMockito.given(this.awsTextractClient.analyzeExpense(mockBytes))
                                .willThrow(new RuntimeException("AWS SDK Error"));

                Document errorDoc = Document.builder().url(url).build();
                Document savedError = this.documentRepository.save(errorDoc);
                assertThrows(BadRequestException.class, () -> this.documentAiService.extractInvoice(savedError.getId()));
        }
}
