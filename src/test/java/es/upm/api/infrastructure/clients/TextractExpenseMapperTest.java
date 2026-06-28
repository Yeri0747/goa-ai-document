package es.upm.api.infrastructure.clients;

import es.upm.api.data.entities.Invoice;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.textract.model.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextractExpenseMapperTest {

    @Test
    void mapCompleteInvoice() {
        AnalyzeExpenseResponse response = AnalyzeExpenseResponse.builder()
                .expenseDocuments(ExpenseDocument.builder()
                        .summaryFields(
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("VENDOR_NAME").build())
                                        .valueDetection(ExpenseDetection.builder().text("ACME Corp").build())
                                        .build(),
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("INVOICE_RECEIPT_DATE").build())
                                        .valueDetection(ExpenseDetection.builder().text("2026-06-21").build())
                                        .build(),
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("INVOICE_RECEIPT_ID").build())
                                        .valueDetection(ExpenseDetection.builder().text("INV-987").build())
                                        .build(),
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("TOTAL").build())
                                        .valueDetection(ExpenseDetection.builder().text("150.00").build())
                                        .currency(ExpenseCurrency.builder().code("EUR").build())
                                        .build(),
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("TAX").build())
                                        .valueDetection(ExpenseDetection.builder().text("21.00").build())
                                        .build()
                        )
                        .lineItemGroups(
                                LineItemGroup.builder()
                                        .lineItems(
                                                LineItemFields.builder()
                                                        .lineItemExpenseFields(
                                                                ExpenseField.builder()
                                                                        .type(ExpenseType.builder().text("ITEM").build())
                                                                        .valueDetection(ExpenseDetection.builder().text("Consulting Services").build())
                                                                        .build(),
                                                                ExpenseField.builder()
                                                                        .type(ExpenseType.builder().text("QUANTITY").build())
                                                                        .valueDetection(ExpenseDetection.builder().text("1").build())
                                                                        .build(),
                                                                ExpenseField.builder()
                                                                        .type(ExpenseType.builder().text("UNIT_PRICE").build())
                                                                        .valueDetection(ExpenseDetection.builder().text("123.97").build())
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .build())
                .build();

        Invoice invoice = TextractExpenseMapper.map(response);

        assertThat(invoice.getVendorName()).isEqualTo("ACME Corp");
        assertThat(invoice.getInvoiceDate()).isEqualTo("2026-06-21");
        assertThat(invoice.getInvoiceId()).isEqualTo("INV-987");
        assertThat(invoice.getTotal()).isEqualTo("150.00");
        assertThat(invoice.getTaxAmount()).isEqualTo("21.00");
        assertThat(invoice.getCurrency()).isEqualTo("EUR");
        assertThat(invoice.getLineItems()).hasSize(1);
        assertThat(invoice.getLineItems().get(0).getName()).isEqualTo("Consulting Services");
        assertThat(invoice.getLineItems().get(0).getQuantity()).isEqualTo("1");
    }

    @Test
    void mapMultipleLineItems() {
        AnalyzeExpenseResponse response = AnalyzeExpenseResponse.builder()
                .expenseDocuments(ExpenseDocument.builder()
                        .summaryFields(
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("VENDOR_NAME").build())
                                        .valueDetection(ExpenseDetection.builder().text("TechCorp").build())
                                        .build(),
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("TOTAL").build())
                                        .valueDetection(ExpenseDetection.builder().text("500.00").build())
                                        .currency(ExpenseCurrency.builder().code("EUR").build())
                                        .build()
                        )
                        .lineItemGroups(
                                LineItemGroup.builder()
                                        .lineItems(
                                                LineItemFields.builder()
                                                        .lineItemExpenseFields(
                                                                ExpenseField.builder()
                                                                        .type(ExpenseType.builder().text("ITEM").build())
                                                                        .valueDetection(ExpenseDetection.builder().text("Software License").build())
                                                                        .build(),
                                                                ExpenseField.builder()
                                                                        .type(ExpenseType.builder().text("QUANTITY").build())
                                                                        .valueDetection(ExpenseDetection.builder().text("1").build())
                                                                        .build()
                                                        )
                                                        .build(),
                                                LineItemFields.builder()
                                                        .lineItemExpenseFields(
                                                                ExpenseField.builder()
                                                                        .type(ExpenseType.builder().text("ITEM").build())
                                                                        .valueDetection(ExpenseDetection.builder().text("Support Services").build())
                                                                        .build(),
                                                                ExpenseField.builder()
                                                                        .type(ExpenseType.builder().text("QUANTITY").build())
                                                                        .valueDetection(ExpenseDetection.builder().text("2").build())
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .build())
                .build();

        Invoice invoice = TextractExpenseMapper.map(response);

        assertThat(invoice.getVendorName()).isEqualTo("TechCorp");
        assertThat(invoice.getLineItems()).hasSize(2);
        assertThat(invoice.getLineItems().get(0).getName()).isEqualTo("Software License");
        assertThat(invoice.getLineItems().get(1).getName()).isEqualTo("Support Services");
    }

    @Test
    void mapEmptyExpenseDocumentsThrows() {
        AnalyzeExpenseResponse response = AnalyzeExpenseResponse.builder()
                .expenseDocuments(new ExpenseDocument[]{})
                .build();

        assertThrows(TextractExtractionException.class, () -> TextractExpenseMapper.map(response));
    }

    @Test
    void mapAllSummaryFields() {
        AnalyzeExpenseResponse response = AnalyzeExpenseResponse.builder()
                .expenseDocuments(ExpenseDocument.builder()
                        .summaryFields(
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("VENDOR_NAME").build())
                                        .valueDetection(ExpenseDetection.builder().text("Global Supplies Inc").build())
                                        .build(),
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("INVOICE_RECEIPT_DATE").build())
                                        .valueDetection(ExpenseDetection.builder().text("2026-06-15").build())
                                        .build(),
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("INVOICE_RECEIPT_ID").build())
                                        .valueDetection(ExpenseDetection.builder().text("INV-2026-0815").build())
                                        .build(),
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("DUE_DATE").build())
                                        .valueDetection(ExpenseDetection.builder().text("2026-07-15").build())
                                        .build(),
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("RECEIVER_NAME").build())
                                        .valueDetection(ExpenseDetection.builder().text("Our Company Ltd").build())
                                        .build(),
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("RECEIVER_TAX_ID").build())
                                        .valueDetection(ExpenseDetection.builder().text("ES12345678A").build())
                                        .build(),
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("SUBTOTAL").build())
                                        .valueDetection(ExpenseDetection.builder().text("800.00").build())
                                        .build(),
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("TAX").build())
                                        .valueDetection(ExpenseDetection.builder().text("168.00").build())
                                        .build(),
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("TOTAL").build())
                                        .valueDetection(ExpenseDetection.builder().text("968.00").build())
                                        .currency(ExpenseCurrency.builder().code("EUR").build())
                                        .build()
                        )
                        .build())
                .build();

        Invoice invoice = TextractExpenseMapper.map(response);

        assertThat(invoice.getVendorName()).isEqualTo("Global Supplies Inc");
        assertThat(invoice.getInvoiceDate()).isEqualTo("2026-06-15");
        assertThat(invoice.getInvoiceId()).isEqualTo("INV-2026-0815");
        assertThat(invoice.getDueDate()).isEqualTo("2026-07-15");
        assertThat(invoice.getReceiverName()).isEqualTo("Our Company Ltd");
        assertThat(invoice.getReceiverTaxId()).isEqualTo("ES12345678A");
        assertThat(invoice.getSubtotal()).isEqualTo("800.00");
        assertThat(invoice.getTaxAmount()).isEqualTo("168.00");
        assertThat(invoice.getTotal()).isEqualTo("968.00");
        assertThat(invoice.getCurrency()).isEqualTo("EUR");
    }
}
