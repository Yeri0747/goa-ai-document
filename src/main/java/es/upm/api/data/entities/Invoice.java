package es.upm.api.data.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "invoices")
public class Invoice {
    @Id
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
    private LocalDateTime extractionDate;
    private List<LineItem> lineItems;
}
