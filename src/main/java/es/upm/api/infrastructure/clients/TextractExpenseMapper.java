package es.upm.api.infrastructure.clients;

import es.upm.api.data.entities.Invoice;
import es.upm.api.data.entities.LineItem;
import software.amazon.awssdk.services.textract.model.AnalyzeExpenseResponse;
import software.amazon.awssdk.services.textract.model.ExpenseDocument;

import java.util.ArrayList;
import java.util.List;

final class TextractExpenseMapper {

    private TextractExpenseMapper() {
    }

    static Invoice map(AnalyzeExpenseResponse response) {
        if (response.expenseDocuments() == null || response.expenseDocuments().isEmpty()) {
            throw new TextractExtractionException("No se pudo extraer información de la factura");
        }

        ExpenseDocument expenseDoc = response.expenseDocuments().get(0);
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
                            default:
                                break;
                        }
                    }
                }
                if (field.currency() != null && field.currency().code() != null) {
                    currency = field.currency().code();
                }
            }
        }

        List<LineItem> lineItems = mapLineItems(expenseDoc);

        return Invoice.builder()
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
                .lineItems(lineItems)
                .build();
    }

    private static List<LineItem> mapLineItems(ExpenseDocument expenseDoc) {
        List<LineItem> lineItems = new ArrayList<>();
        if (expenseDoc.lineItemGroups() == null) {
            return lineItems;
        }

        for (var group : expenseDoc.lineItemGroups()) {
            if (group.lineItems() == null) {
                continue;
            }
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
                                    default:
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
        return lineItems;
    }
}
