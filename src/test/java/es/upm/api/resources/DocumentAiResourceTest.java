package es.upm.api.resources;

import es.upm.api.data.entities.Document;
import es.upm.api.data.entities.DocumentCategory;
import es.upm.api.data.entities.Invoice;
import es.upm.api.exceptions.NotFoundException;
import es.upm.api.resources.httperrors.ApiExceptionHandler;
import es.upm.api.services.DocumentAiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DocumentAiResourceTest {

    @Mock
    private DocumentAiService documentAiService;

    @Mock
    private Environment environment;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DocumentAiResource resource = new DocumentAiResource(documentAiService);
        mockMvc = MockMvcBuilders.standaloneSetup(resource)
                .setControllerAdvice(new ApiExceptionHandler(environment))
                .build();
    }

    @Test
    void uploadDocumentDelegatesToService() throws Exception {
        Document document = Document.builder()
                .id("doc-1")
                .name("test.pdf")
                .category(DocumentCategory.INVOICE)
                .build();
        given(documentAiService.uploadDocument(any(), eq(false))).willReturn(document);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart(DocumentAiResource.DOCUMENT_AI + DocumentAiResource.DOCUMENTS)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("doc-1"))
                .andExpect(jsonPath("$.name").value("test.pdf"))
                .andExpect(jsonPath("$.category").value("INVOICE"));

        verify(documentAiService).uploadDocument(any(), eq(false));
    }

    @Test
    void generateSummaryDelegatesToService() throws Exception {
        Document document = Document.builder()
                .id("doc-1")
                .summary("Resumen generado")
                .build();
        given(documentAiService.summarizeDocument("doc-1")).willReturn(document);

        mockMvc.perform(post(DocumentAiResource.DOCUMENT_AI + DocumentAiResource.DOCUMENTS + "/doc-1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Resumen generado"));
    }

    @Test
    void generateSummaryNotFoundReturns404() throws Exception {
        given(documentAiService.summarizeDocument("missing"))
                .willThrow(new NotFoundException("Document not found"));

        mockMvc.perform(post(DocumentAiResource.DOCUMENT_AI + DocumentAiResource.DOCUMENTS + "/missing/summary"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NotFoundException"));
    }

    @Test
    void extractInvoiceDelegatesToService() throws Exception {
        Invoice invoice = Invoice.builder()
                .id("inv-1")
                .documentId("doc-1")
                .vendorName("ACME")
                .total("42.00")
                .currency("EUR")
                .build();
        given(documentAiService.extractInvoice("doc-1")).willReturn(invoice);

        mockMvc.perform(get(DocumentAiResource.DOCUMENT_AI + DocumentAiResource.DOCUMENTS + "/doc-1/invoice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendorName").value("ACME"))
                .andExpect(jsonPath("$.total").value("42.00"))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }
}
