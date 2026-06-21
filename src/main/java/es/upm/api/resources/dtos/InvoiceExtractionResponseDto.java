package es.upm.api.resources.dtos;

import es.upm.api.data.entities.Invoice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceExtractionResponseDto {
    private String id;
    private String documentId;
    private String vendorName;
    private String invoiceDate;
    private String invoiceId;
    private String dueDate;
    private String receiverName;
    private String receiverTaxId;
    private String subtotal;
    private String taxAmount;
    private String total;
    private String currency;
    private List<LineItemResponseDto> lineItems;

    public InvoiceExtractionResponseDto(Invoice invoice) {
        if (invoice != null) {
            BeanUtils.copyProperties(invoice, this);
            if (invoice.getLineItems() != null) {
                this.lineItems = invoice.getLineItems().stream()
                        .map(LineItemResponseDto::new)
                        .collect(Collectors.toList());
            }
        }
    }
}
