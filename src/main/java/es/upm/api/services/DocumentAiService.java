package es.upm.api.services;

import es.upm.api.data.daos.DocumentRepository;
import es.upm.api.data.daos.InvoiceRepository;
import es.upm.api.data.entities.Document;
import es.upm.api.data.entities.DocumentCategory;
import es.upm.api.data.entities.Invoice;
import es.upm.api.infrastructure.clients.AwsTextractClient;
import es.upm.api.infrastructure.clients.OpenAiClassifierClient;
import es.upm.api.infrastructure.clients.S3CloudClient;
import es.upm.api.infrastructure.clients.TextractExtractionException;
import es.upm.api.infrastructure.support.FileDownloader;
import es.upm.api.infrastructure.support.PdfExtractor;
import es.upm.api.exceptions.BadRequestException;
import es.upm.api.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class DocumentAiService {

    private final S3CloudClient s3CloudClient;
    private final DocumentRepository documentRepository;
    private final PdfExtractor pdfExtractor;
    private final OpenAiClassifierClient openAiClassifierClient;
    private final AwsTextractClient awsTextractClient;
    private final FileDownloader fileDownloader;
    private final InvoiceRepository invoiceRepository;

    @Autowired
    public DocumentAiService(S3CloudClient s3CloudClient,
                             DocumentRepository documentRepository,
                             PdfExtractor pdfExtractor,
                             OpenAiClassifierClient openAiClassifierClient,
                             AwsTextractClient awsTextractClient,
                             FileDownloader fileDownloader,
                             InvoiceRepository invoiceRepository) {
        this.s3CloudClient = s3CloudClient;
        this.documentRepository = documentRepository;
        this.pdfExtractor = pdfExtractor;
        this.openAiClassifierClient = openAiClassifierClient;
        this.awsTextractClient = awsTextractClient;
        this.fileDownloader = fileDownloader;
        this.invoiceRepository = invoiceRepository;
    }

    public Document uploadDocument(MultipartFile file, boolean autoclassify) {
        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new BadRequestException("Only PDF files are allowed");
        }

        DocumentCategory category = null;

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

    public Invoice extractInvoice(String documentId) {
        Optional<Invoice> cachedInvoice = this.invoiceRepository.findByDocumentId(documentId);
        if (cachedInvoice.isPresent()) {
            return cachedInvoice.get();
        }

        Document document = this.documentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));
        String url = document.getUrl();

        byte[] fileBytes;
        try {
            fileBytes = this.fileDownloader.downloadFile(url);
        } catch (IOException e) {
            throw new BadRequestException("Error al descargar el archivo desde la URL: " + url);
        }

        Invoice extractedInvoice;
        try {
            extractedInvoice = this.awsTextractClient.extractInvoice(fileBytes);
        } catch (TextractExtractionException e) {
            throw new BadRequestException(e.getMessage());
        }

        Invoice invoice = Invoice.builder()
                .documentId(documentId)
                .vendorName(extractedInvoice.getVendorName())
                .invoiceDate(extractedInvoice.getInvoiceDate())
                .invoiceId(extractedInvoice.getInvoiceId())
                .dueDate(extractedInvoice.getDueDate())
                .receiverName(extractedInvoice.getReceiverName())
                .receiverTaxId(extractedInvoice.getReceiverTaxId())
                .subtotal(extractedInvoice.getSubtotal())
                .taxAmount(extractedInvoice.getTaxAmount())
                .total(extractedInvoice.getTotal())
                .currency(extractedInvoice.getCurrency())
                .extractionDate(LocalDateTime.now())
                .lineItems(extractedInvoice.getLineItems())
                .build();

        return this.invoiceRepository.save(invoice);
    }
}
