package es.upm.api.resources;

import es.upm.api.data.daos.DocumentRepository;
import es.upm.api.infrastructure.clients.S3CloudClient;
import es.upm.api.services.DocumentAiService;
import es.upm.api.infrastructure.clients.OpenAiClassifierClient;
import es.upm.api.infrastructure.support.PdfExtractor;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
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
    private DocumentAiService documentAiService;

    @MockitoBean
    private S3CloudClient s3CloudClient;

    @MockitoBean
    private PdfExtractor pdfExtractor;

    @MockitoBean
    private OpenAiClassifierClient openAiClassifierClient;



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

}
