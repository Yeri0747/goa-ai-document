package es.upm.api.resources.dtos;

import es.upm.api.data.entities.Document;
import es.upm.api.data.entities.DocumentCategory;
import es.upm.api.data.entities.Invoice;
import es.upm.api.data.entities.LineItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentResponseDtoTest {

    @Test
    void mapsDocumentFields() {
        LocalDateTime uploadDate = LocalDateTime.of(2026, 6, 1, 10, 0);
        Document document = Document.builder()
                .id("doc-1")
                .name("invoice.pdf")
                .sizeInfo(2048L)
                .url("https://example.com/invoice.pdf")
                .uploadDate(uploadDate)
                .category(DocumentCategory.INVOICE)
                .summary("Resumen")
                .build();

        DocumentResponseDto dto = new DocumentResponseDto(document);

        assertThat(dto.getId()).isEqualTo("doc-1");
        assertThat(dto.getName()).isEqualTo("invoice.pdf");
        assertThat(dto.getSizeInfo()).isEqualTo(2048L);
        assertThat(dto.getUrl()).isEqualTo("https://example.com/invoice.pdf");
        assertThat(dto.getUploadDate()).isEqualTo(uploadDate);
        assertThat(dto.getCategory()).isEqualTo(DocumentCategory.INVOICE);
        assertThat(dto.getSummary()).isEqualTo("Resumen");
    }
}

class InvoiceExtractionResponseDtoTest {

    @Test
    void mapsInvoiceWithLineItems() {
        Invoice invoice = Invoice.builder()
                .id("inv-1")
                .documentId("doc-1")
                .vendorName("ACME")
                .total("99.99")
                .currency("EUR")
                .lineItems(List.of(
                        LineItem.builder().name("Item A").quantity("2").build()))
                .build();

        InvoiceExtractionResponseDto dto = new InvoiceExtractionResponseDto(invoice);

        assertThat(dto.getId()).isEqualTo("inv-1");
        assertThat(dto.getDocumentId()).isEqualTo("doc-1");
        assertThat(dto.getVendorName()).isEqualTo("ACME");
        assertThat(dto.getTotal()).isEqualTo("99.99");
        assertThat(dto.getCurrency()).isEqualTo("EUR");
        assertThat(dto.getLineItems()).hasSize(1);
        assertThat(dto.getLineItems().get(0).getName()).isEqualTo("Item A");
    }

    @Test
    void nullInvoiceLeavesDtoEmpty() {
        InvoiceExtractionResponseDto dto = new InvoiceExtractionResponseDto(null);

        assertThat(dto.getId()).isNull();
        assertThat(dto.getLineItems()).isNull();
    }
}

class LineItemResponseDtoTest {

    @Test
    void mapsLineItemFields() {
        LineItem lineItem = LineItem.builder()
                .name("Service")
                .quantity("3")
                .price("150.00")
                .unitPrice("50.00")
                .build();

        LineItemResponseDto dto = new LineItemResponseDto(lineItem);

        assertThat(dto.getName()).isEqualTo("Service");
        assertThat(dto.getQuantity()).isEqualTo("3");
        assertThat(dto.getPrice()).isEqualTo("150.00");
        assertThat(dto.getUnitPrice()).isEqualTo("50.00");
    }

    @Test
    void nullLineItemLeavesDtoEmpty() {
        LineItemResponseDto dto = new LineItemResponseDto(null);

        assertThat(dto.getName()).isNull();
        assertThat(dto.getQuantity()).isNull();
    }
}
