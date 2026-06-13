package es.upm.api.services;

import es.upm.api.data.daos.DocumentRepository;
import es.upm.api.data.entities.Document;
import es.upm.api.infrastructure.clients.S3CloudClient;
import es.upm.api.infrastructure.clients.OpenAiClassifierClient;
import es.upm.api.infrastructure.support.PdfExtractor;
import es.upm.api.services.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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

    @MockitoBean
    private S3CloudClient s3CloudClient;

    @MockitoBean
    private PdfExtractor pdfExtractor;

    @MockitoBean
    private OpenAiClassifierClient openAiClassifierClient;

    @Test
    void testUploadDocument() {
        BDDMockito.given(this.s3CloudClient.uploadFile(any()))
                .willReturn("https://mock-bucket.s3.amazonaws.com/test-file.pdf");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-file.pdf",
                "application/pdf",
                "mock content".getBytes()
        );

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
                "mock content".getBytes()
        );

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
                "mock content".getBytes()
        );

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
}
