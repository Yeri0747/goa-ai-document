package es.upm.api.services;

import es.upm.api.data.daos.DocumentRepository;
import es.upm.api.data.daos.InvoiceRepository;
import es.upm.api.data.entities.Document;
import es.upm.api.data.entities.DocumentCategory;
import es.upm.api.data.entities.Invoice;
import es.upm.api.data.entities.LineItem;
import es.upm.api.infrastructure.clients.AwsTextractClient;
import es.upm.api.infrastructure.clients.OpenAiClassifierClient;
import es.upm.api.infrastructure.clients.S3CloudClient;
import es.upm.api.infrastructure.support.FileDownloader;
import es.upm.api.infrastructure.support.PdfExtractor;
import es.upm.api.exceptions.BadRequestException;
import es.upm.api.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.textract.model.AnalyzeExpenseResponse;

import java.time.LocalDateTime;
import java.util.List;
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

    public Invoice extractInvoice(String documentId) {
        Optional<Invoice> cachedInvoice = this.invoiceRepository.findByDocumentId(documentId);
        if (cachedInvoice.isPresent()) {
            return cachedInvoice.get();
        }

        Document document = this.documentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));
        String url = document.getUrl();

        byte[] fileBytes = this.fileDownloader.downloadFile(url);
        AnalyzeExpenseResponse response;
        try {
            response = this.awsTextractClient.analyzeExpense(fileBytes);
        } catch (Exception e) {
            throw new BadRequestException("Error al procesar la factura con AWS Textract: " + e.getMessage());
        }

        if (response.expenseDocuments() == null || response.expenseDocuments().isEmpty()) {
            throw new BadRequestException("No se pudo extraer información de la factura");
        }

        var expenseDoc = response.expenseDocuments().get(0);
        String vendorName = null;
        String invoiceDate = null;
        String invoiceId = null;
        String dueDate = null;
        String receiverName = null;
        String receiverTaxId = null;
        String subtotal = null;
        String taxAmount = null;
        String total = null;
        String currency = null;

        if (expenseDoc.summaryFields() != null) {
            for (var field : expenseDoc.summaryFields()) {
                if (field.type() != null) {
                    String fieldType = field.type().text();
                    String fieldValue = field.valueDetection() != null ? field.valueDetection().text() : null;

                    if (fieldValue != null) {
                        switch (fieldType) {
                            case "VENDOR_NAME":
                                vendorName = fieldValue;
                                break;
                            case "INVOICE_RECEIPT_DATE":
                                invoiceDate = fieldValue;
                                break;
                            case "INVOICE_RECEIPT_ID":
                                invoiceId = fieldValue;
                                break;
                            case "DUE_DATE":
                                dueDate = fieldValue;
                                break;
                            case "RECEIVER_NAME":
                                receiverName = fieldValue;
                                break;
                            case "RECEIVER_TAX_ID":
                            case "TAX_PAYER_ID":
                                receiverTaxId = fieldValue;
                                break;
                            case "SUBTOTAL":
                                subtotal = fieldValue;
                                break;
                            case "TAX":
                                taxAmount = fieldValue;
                                break;
                            case "TOTAL":
                                total = fieldValue;
                                break;
                        }
                    }
                }
                if (field.currency() != null && field.currency().code() != null) {
                    currency = field.currency().code();
                }
            }
        }

        List<LineItem> lineItems = new java.util.ArrayList<>();
        if (expenseDoc.lineItemGroups() != null) {
            for (var group : expenseDoc.lineItemGroups()) {
                if (group.lineItems() != null) {
                    for (var itemFields : group.lineItems()) {
                        String name = null;
                        String quantity = null;
                        String price = null;
                        String unitPrice = null;

                        if (itemFields.lineItemExpenseFields() != null) {
                            for (var field : itemFields.lineItemExpenseFields()) {
                                if (field.type() != null) {
                                    String fieldType = field.type().text();
                                    String fieldValue = field.valueDetection() != null ? field.valueDetection().text() : null;
                                    if (fieldValue != null) {
                                        switch (fieldType) {
                                            case "ITEM":
                                                name = fieldValue;
                                                break;
                                            case "QUANTITY":
                                                quantity = fieldValue;
                                                break;
                                            case "PRICE":
                                                price = fieldValue;
                                                break;
                                            case "UNIT_PRICE":
                                                unitPrice = fieldValue;
                                                break;
                                        }
                                    }
                                }
                            }
                        }
                        lineItems.add(LineItem.builder()
                                .name(name)
                                .quantity(quantity)
                                .price(price)
                                .unitPrice(unitPrice)
                                .build());
                    }
                }
            }
        }

        Invoice invoice = Invoice.builder()
                .documentId(documentId)
                .vendorName(vendorName)
                .invoiceDate(invoiceDate)
                .invoiceId(invoiceId)
                .dueDate(dueDate)
                .receiverName(receiverName)
                .receiverTaxId(receiverTaxId)
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .total(total)
                .currency(currency)
                .extractionDate(LocalDateTime.now())
                .lineItems(lineItems)
                .build();

        return this.invoiceRepository.save(invoice);
    }
}
