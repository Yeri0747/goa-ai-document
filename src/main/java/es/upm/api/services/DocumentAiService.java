package es.upm.api.services;

import es.upm.api.data.daos.DocumentRepository;
import es.upm.api.data.entities.Document;
import es.upm.api.data.entities.DocumentCategory;
import es.upm.api.services.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
public class DocumentAiService {

    private final S3CloudService s3CloudService;
    private final DocumentRepository documentRepository;
    private final PdfExtractorService pdfExtractorService;
    private final OpenAiClassifierService openAiClassifierService;

    @Autowired
    public DocumentAiService(S3CloudService s3CloudService, 
                             DocumentRepository documentRepository,
                             PdfExtractorService pdfExtractorService,
                             OpenAiClassifierService openAiClassifierService) {
        this.s3CloudService = s3CloudService;
        this.documentRepository = documentRepository;
        this.pdfExtractorService = pdfExtractorService;
        this.openAiClassifierService = openAiClassifierService;
    }

    public Document uploadDocument(MultipartFile file, boolean autoclassify) {
        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new BadRequestException("Only PDF files are allowed");
        }

        es.upm.api.data.entities.DocumentCategory category = null;

        if (autoclassify) {
            String text = this.pdfExtractorService.extractTextFromPdf(file);
            category = this.openAiClassifierService.classifyText(text);
        }

        String fileUrl = this.s3CloudService.uploadFile(file);
        Document document = Document.builder()
                .name(file.getOriginalFilename())
                .sizeInfo(file.getSize())
                .url(fileUrl)
                .uploadDate(LocalDateTime.now())
                .category(category)
                .build();

        return this.documentRepository.save(document);
    }
}
