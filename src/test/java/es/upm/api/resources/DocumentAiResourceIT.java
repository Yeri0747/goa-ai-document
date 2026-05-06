package es.upm.api.resources;

import es.upm.api.data.daos.DocumentRepository;
import es.upm.api.services.DocumentAiService;
import es.upm.api.services.OpenAiClassifierService;
import es.upm.api.services.PdfExtractorService;
import es.upm.api.services.S3CloudService;
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
    private S3CloudService s3CloudService;

    @MockitoBean
    private PdfExtractorService pdfExtractorService;

    @MockitoBean
    private OpenAiClassifierService openAiClassifierService;



    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_admin"})
    void testUploadDocumentSuccess() throws Exception {
        BDDMockito.given(this.s3CloudService.uploadFile(any()))
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
        BDDMockito.given(this.s3CloudService.uploadFile(any()))
                .willReturn("https://mock-bucket.s3.amazonaws.com/test-file.pdf");
        BDDMockito.given(this.pdfExtractorService.extractTextFromPdf(any()))
                .willReturn("mock extracted text");
        BDDMockito.given(this.openAiClassifierService.classifyText(any()))
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

        BDDMockito.given(this.pdfExtractorService.extractTextFromUrl(any()))
                .willReturn("Texto de prueba");
        BDDMockito.given(this.openAiClassifierService.summarizeText(any()))
                .willReturn("Este es el resumen");

        mockMvc.perform(post(DocumentAiResource.DOCUMENT_AI + DocumentAiResource.DOCUMENTS + "/" + doc.getId() + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Este es el resumen"));
    }

}
