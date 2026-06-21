package es.upm.api.resources;

import es.upm.api.resources.dtos.DocumentResponseDto;
import es.upm.api.resources.dtos.InvoiceExtractionResponseDto;
import es.upm.api.services.DocumentAiService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(DocumentAiResource.DOCUMENT_AI)
public class DocumentAiResource {

    public static final String DOCUMENT_AI = "/document-ai";
    public static final String DOCUMENTS = "/documents";
    public static final String EXTRACT_INVOICE = "/invoice";

    private final DocumentAiService documentAiService;

    @Autowired
    public DocumentAiResource(DocumentAiService documentAiService) {
        this.documentAiService = documentAiService;
    }

    @PostMapping(DOCUMENTS)
    public DocumentResponseDto uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "autoclassify", defaultValue = "false") boolean autoclassify) {
        return new DocumentResponseDto(this.documentAiService.uploadDocument(file, autoclassify));
    }

    @PostMapping(DOCUMENTS + "/{id}/summary")
    public DocumentResponseDto generateSummary(@PathVariable String id) {
        return new DocumentResponseDto(this.documentAiService.summarizeDocument(id));
    }

    @GetMapping(DOCUMENTS + "/{id}" + EXTRACT_INVOICE)
    public InvoiceExtractionResponseDto extractInvoice(@PathVariable String id) {
        return new InvoiceExtractionResponseDto(this.documentAiService.extractInvoice(id));
    }
}
