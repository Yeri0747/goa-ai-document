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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DocumentAiServiceTest {

    @Mock
    private S3CloudClient s3CloudClient;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private PdfExtractor pdfExtractor;

    @Mock
    private OpenAiClassifierClient openAiClassifierClient;

    @Mock
    private AwsTextractClient awsTextractClient;

    @Mock
    private FileDownloader fileDownloader;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private DocumentAiService documentAiService;

    @Test
    void summarizeDocumentNotFound() {
        given(documentRepository.findById("missing-id")).willReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> documentAiService.summarizeDocument("missing-id"));

        verify(pdfExtractor, never()).extractTextFromUrl(any());
    }

    @Test
    void summarizeDocumentSuccess() {
        Document document = Document.builder()
                .id("doc-1")
                .url("https://example.com/doc.pdf")
                .build();
        given(documentRepository.findById("doc-1")).willReturn(Optional.of(document));
        given(pdfExtractor.extractTextFromUrl(document.getUrl())).willReturn("Texto extraído");
        given(openAiClassifierClient.summarizeText("Texto extraído")).willReturn("Resumen");
        given(documentRepository.save(document)).willReturn(document);

        Document result = documentAiService.summarizeDocument("doc-1");

        assertThat(result.getSummary()).isEqualTo("Resumen");
    }

    @Test
    void uploadDocumentRejectsNonPdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "content".getBytes());

        assertThrows(BadRequestException.class, () -> documentAiService.uploadDocument(file, false));
    }

    @Test
    void extractInvoiceDownloadFailure() throws IOException {
        String url = "https://example.com/missing.pdf";
        Document document = Document.builder().id("doc-1").url(url).build();
        given(invoiceRepository.findByDocumentId("doc-1")).willReturn(Optional.empty());
        given(documentRepository.findById("doc-1")).willReturn(Optional.of(document));
        doThrow(new IOException("connection failed")).when(fileDownloader).downloadFile(url);

        assertThrows(BadRequestException.class, () -> documentAiService.extractInvoice("doc-1"));
    }

    @Test
    void extractInvoiceTextractFailure() throws IOException {
        String url = "https://example.com/invoice.pdf";
        byte[] bytes = "pdf".getBytes();
        Document document = Document.builder().id("doc-1").url(url).build();
        given(invoiceRepository.findByDocumentId("doc-1")).willReturn(Optional.empty());
        given(documentRepository.findById("doc-1")).willReturn(Optional.of(document));
        doReturn(bytes).when(fileDownloader).downloadFile(url);
        given(awsTextractClient.extractInvoice(bytes))
                .willThrow(new TextractExtractionException("No se pudo extraer información de la factura"));

        assertThrows(BadRequestException.class, () -> documentAiService.extractInvoice("doc-1"));
    }

    @Test
    void extractInvoiceReturnsCachedInvoice() throws IOException {
        Invoice cached = Invoice.builder().id("inv-1").documentId("doc-1").vendorName("Cached Corp").build();
        given(invoiceRepository.findByDocumentId("doc-1")).willReturn(Optional.of(cached));

        Invoice result = documentAiService.extractInvoice("doc-1");

        assertThat(result).isEqualTo(cached);
        verify(fileDownloader, never()).downloadFile(any());
        verify(awsTextractClient, never()).extractInvoice(any());
    }

    @Test
    void extractInvoiceSuccess() throws IOException {
        String url = "https://example.com/invoice.pdf";
        byte[] bytes = "pdf".getBytes();
        Document document = Document.builder().id("doc-1").url(url).build();
        Invoice extracted = Invoice.builder()
                .vendorName("ACME Corp")
                .total("100.00")
                .currency("EUR")
                .build();
        Invoice saved = Invoice.builder()
                .id("inv-1")
                .documentId("doc-1")
                .vendorName("ACME Corp")
                .total("100.00")
                .currency("EUR")
                .build();

        given(invoiceRepository.findByDocumentId("doc-1")).willReturn(Optional.empty());
        given(documentRepository.findById("doc-1")).willReturn(Optional.of(document));
        doReturn(bytes).when(fileDownloader).downloadFile(url);
        given(awsTextractClient.extractInvoice(bytes)).willReturn(extracted);
        given(invoiceRepository.save(any(Invoice.class))).willReturn(saved);

        Invoice result = documentAiService.extractInvoice("doc-1");

        assertThat(result.getVendorName()).isEqualTo("ACME Corp");
        assertThat(result.getTotal()).isEqualTo("100.00");
        verify(awsTextractClient).extractInvoice(eq(bytes));
    }
}
