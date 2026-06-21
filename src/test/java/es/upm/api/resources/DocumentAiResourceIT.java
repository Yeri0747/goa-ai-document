package es.upm.api.resources;

import es.upm.api.data.daos.DocumentRepository;
import es.upm.api.data.daos.InvoiceRepository;
import es.upm.api.infrastructure.clients.S3CloudClient;
import es.upm.api.services.DocumentAiService;
import es.upm.api.infrastructure.clients.OpenAiClassifierClient;
import es.upm.api.infrastructure.support.PdfExtractor;
import es.upm.api.infrastructure.clients.AwsTextractClient;
import es.upm.api.infrastructure.support.FileDownloader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.textract.model.*;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class DocumentAiResourceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private DocumentAiService documentAiService;

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
    @WithMockUser(username = "admin", authorities = {"ROLE_admin"})
    void testUploadDocumentSuccess() throws Exception {
        BDDMockito.given(this.s3CloudClient.uploadFile(any()))
                .willReturn("https://mock-bucket.s3.amazonaws.com/test-file.pdf");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-file.pdf",
                "application/pdf",
                "mock content".getBytes()
        );

        mockMvc.perform(multipart(DocumentAiResource.DOCUMENT_AI + DocumentAiResource.DOCUMENTS)
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test-file.pdf"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_admin"})
    void testUploadDocumentSuccessWithAutoclassify() throws Exception {
        BDDMockito.given(this.s3CloudClient.uploadFile(any()))
                .willReturn("https://mock-bucket.s3.amazonaws.com/test-file.pdf");
        BDDMockito.given(this.pdfExtractor.extractTextFromPdf(any()))
                .willReturn("mock extracted text");
        BDDMockito.given(this.openAiClassifierClient.classifyText(any()))
                .willReturn(es.upm.api.data.entities.DocumentCategory.INVOICE);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-file.pdf",
                "application/pdf",
                "mock content".getBytes()
        );

        mockMvc.perform(multipart(DocumentAiResource.DOCUMENT_AI + DocumentAiResource.DOCUMENTS)
                .file(file)
                .param("autoclassify", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test-file.pdf"))
                .andExpect(jsonPath("$.category").value("INVOICE"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_admin"})
    void testUploadDocumentInvalidFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-file.txt",
                "text/plain",
                "mock content".getBytes()
        );

        mockMvc.perform(multipart(DocumentAiResource.DOCUMENT_AI + DocumentAiResource.DOCUMENTS)
                .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_admin"})
    void testGenerateSummarySuccess() throws Exception {
        es.upm.api.data.entities.Document doc = es.upm.api.data.entities.Document.builder()
                .name("test-resumen.pdf")
                .url("https://url-hardcoded.com/file.pdf")
                .build();
        doc = this.documentRepository.save(doc);

        BDDMockito.given(this.pdfExtractor.extractTextFromUrl(any()))
                .willReturn("Texto de prueba");
        BDDMockito.given(this.openAiClassifierClient.summarizeText(any()))
                .willReturn("Este es el resumen");

        mockMvc.perform(post(DocumentAiResource.DOCUMENT_AI + DocumentAiResource.DOCUMENTS + "/" + doc.getId() + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Este es el resumen"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_admin"})
    void testExtractInvoiceEndpointSuccess() throws Exception {
        String url = "https://mock-invoices.com/invoice-002.pdf";
        byte[] mockBytes = "mock pdf content".getBytes();

        es.upm.api.data.entities.Document document = es.upm.api.data.entities.Document.builder()
                .name("test-invoice.pdf")
                .url(url)
                .category(es.upm.api.data.entities.DocumentCategory.INVOICE)
                .build();
        document = this.documentRepository.save(document);

        BDDMockito.given(this.fileDownloader.downloadFile(url))
                .willReturn(mockBytes);

        AnalyzeExpenseResponse mockResponse = AnalyzeExpenseResponse.builder()
                .expenseDocuments(ExpenseDocument.builder()
                        .summaryFields(
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("VENDOR_NAME").build())
                                        .valueDetection(ExpenseDetection.builder().text("Amazon Web Services").build())
                                        .build(),
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("TOTAL").build())
                                        .valueDetection(ExpenseDetection.builder().text("45.67").build())
                                        .currency(ExpenseCurrency.builder().code("USD").build())
                                        .build()
                        )
                        .build())
                .build();

        BDDMockito.given(this.awsTextractClient.analyzeExpense(mockBytes))
                .willReturn(mockResponse);

        mockMvc.perform(get(DocumentAiResource.DOCUMENT_AI + DocumentAiResource.DOCUMENTS + "/" + document.getId() + DocumentAiResource.EXTRACT_INVOICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendorName").value("Amazon Web Services"))
                .andExpect(jsonPath("$.total").value("45.67"))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_admin"})
    void testExtractInvoiceEndpointValidationFailure() throws Exception {
        mockMvc.perform(get(DocumentAiResource.DOCUMENT_AI + DocumentAiResource.DOCUMENTS + "/non-existent-id" + DocumentAiResource.EXTRACT_INVOICE))
                .andExpect(status().isNotFound());
    }
}
