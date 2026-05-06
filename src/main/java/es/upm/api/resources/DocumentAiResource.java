package es.upm.api.resources;

import es.upm.api.resources.dtos.DocumentDto;
import es.upm.api.services.DocumentAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(DocumentAiResource.DOCUMENT_AI)
public class DocumentAiResource {

    public static final String DOCUMENT_AI = "/document-ai";
    public static final String DOCUMENTS = "/documents";

    private final DocumentAiService documentAiService;

    @Autowired
    public DocumentAiResource(DocumentAiService documentAiService) {
        this.documentAiService = documentAiService;
    }

    @PostMapping(DOCUMENTS)
    public DocumentDto uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "autoclassify", defaultValue = "false") boolean autoclassify) {
        return new DocumentDto(this.documentAiService.uploadDocument(file, autoclassify));
    }

    @PostMapping(DOCUMENTS + "/{id}/summary")
    public DocumentDto generateSummary(@PathVariable String id) {
        return new DocumentDto(this.documentAiService.summarizeDocument(id));
    }
}
