package es.upm.api.services;

import es.upm.api.data.daos.DocumentRepository;
import es.upm.api.data.entities.Document;
import es.upm.api.services.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
public class DocumentAiService {

    private final S3CloudService s3CloudService;
    private final DocumentRepository documentRepository;

    @Autowired
    public DocumentAiService(S3CloudService s3CloudService, DocumentRepository documentRepository) {
        this.s3CloudService = s3CloudService;
        this.documentRepository = documentRepository;
    }

    public Document uploadDocument(MultipartFile file) {
        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new BadRequestException("Only PDF files are allowed");
        }

        String fileUrl = this.s3CloudService.uploadFile(file);
        Document document = Document.builder()
                .name(file.getOriginalFilename())
                .sizeInfo(file.getSize())
                .url(fileUrl)
                .uploadDate(LocalDateTime.now())
                .build();

        return this.documentRepository.save(document);
    }
}
