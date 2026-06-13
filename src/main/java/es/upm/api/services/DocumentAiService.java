package es.upm.api.services;

import es.upm.api.data.daos.DocumentRepository;
import es.upm.api.data.entities.Document;
import es.upm.api.data.entities.DocumentCategory;
import es.upm.api.infrastructure.clients.OpenAiClassifierClient;
import es.upm.api.infrastructure.clients.S3CloudClient;
import es.upm.api.infrastructure.support.PdfExtractor;
import es.upm.api.services.exceptions.BadRequestException;
import es.upm.api.services.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
public class DocumentAiService {

    private final S3CloudClient s3CloudClient;
    private final DocumentRepository documentRepository;
    private final PdfExtractor pdfExtractor;
    private final OpenAiClassifierClient openAiClassifierClient;

    @Autowired
    public DocumentAiService(S3CloudClient s3CloudClient, 
                             DocumentRepository documentRepository,
                             PdfExtractor pdfExtractor,
                             OpenAiClassifierClient openAiClassifierClient) {
        this.s3CloudClient = s3CloudClient;
        this.documentRepository = documentRepository;
        this.pdfExtractor = pdfExtractor;
        this.openAiClassifierClient = openAiClassifierClient;
    }

    public Document uploadDocument(MultipartFile file, boolean autoclassify) {
        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new BadRequestException("Only PDF files are allowed");
        }

        es.upm.api.data.entities.DocumentCategory category = null;

        if (autoclassify) {
            String text = this.pdfExtractor.extractTextFromPdf(file);
            category = this.openAiClassifierClient.classifyText(text);
        }

        String fileUrl = this.s3CloudClient.uploadFile(file);
        Document document = Document.builder()
                .name(file.getOriginalFilename())
                .sizeInfo(file.getSize())
                .url(fileUrl)
                .uploadDate(LocalDateTime.now())
                .category(category)
                .build();

        return this.documentRepository.save(document);
    }

    public Document summarizeDocument(String id) {
        Document document = this.documentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Document not found"));

        String text = this.pdfExtractor.extractTextFromUrl(document.getUrl());
        String summary = this.openAiClassifierClient.summarizeText(text);

        document.setSummary(summary);
        return this.documentRepository.save(document);
    }
}
