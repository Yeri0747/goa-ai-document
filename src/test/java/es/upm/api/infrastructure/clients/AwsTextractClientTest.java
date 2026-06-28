package es.upm.api.infrastructure.clients;

import es.upm.api.data.entities.Invoice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.AnalyzeExpenseResponse;
import software.amazon.awssdk.services.textract.model.ExpenseDetection;
import software.amazon.awssdk.services.textract.model.ExpenseDocument;
import software.amazon.awssdk.services.textract.model.ExpenseField;
import software.amazon.awssdk.services.textract.model.ExpenseType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AwsTextractClientTest {

    @Mock
    private TextractClient textractClient;

    private AwsTextractClient awsTextractClient;

    @BeforeEach
    void setUp() {
        awsTextractClient = new AwsTextractClient("eu-west-1");
        ReflectionTestUtils.setField(awsTextractClient, "textractClient", textractClient);
    }

    @Test
    void extractInvoiceMapsTextractResponse() {
        byte[] bytes = "pdf".getBytes();
        AnalyzeExpenseResponse response = AnalyzeExpenseResponse.builder()
                .expenseDocuments(ExpenseDocument.builder()
                        .summaryFields(
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("VENDOR_NAME").build())
                                        .valueDetection(ExpenseDetection.builder().text("Vendor").build())
                                        .build(),
                                ExpenseField.builder()
                                        .type(ExpenseType.builder().text("TOTAL").build())
                                        .valueDetection(ExpenseDetection.builder().text("10.00").build())
                                        .build()
                        )
                        .build())
                .build();

        given(textractClient.analyzeExpense(any(software.amazon.awssdk.services.textract.model.AnalyzeExpenseRequest.class)))
                .willReturn(response);

        Invoice invoice = awsTextractClient.extractInvoice(bytes);

        assertThat(invoice.getVendorName()).isEqualTo("Vendor");
        assertThat(invoice.getTotal()).isEqualTo("10.00");
    }

    @Test
    void extractInvoiceRethrowsTextractExtractionException() {
        byte[] bytes = "pdf".getBytes();
        AnalyzeExpenseResponse response = AnalyzeExpenseResponse.builder()
                .expenseDocuments(new ExpenseDocument[]{})
                .build();

        given(textractClient.analyzeExpense(any(software.amazon.awssdk.services.textract.model.AnalyzeExpenseRequest.class)))
                .willReturn(response);

        assertThrows(TextractExtractionException.class, () -> awsTextractClient.extractInvoice(bytes));
    }

    @Test
    void extractInvoiceWrapsUnexpectedFailures() {
        byte[] bytes = "pdf".getBytes();

        given(textractClient.analyzeExpense(any(software.amazon.awssdk.services.textract.model.AnalyzeExpenseRequest.class)))
                .willThrow(new RuntimeException("AWS SDK Error"));

        TextractExtractionException exception = assertThrows(
                TextractExtractionException.class,
                () -> awsTextractClient.extractInvoice(bytes));

        assertThat(exception.getMessage()).contains("Error al procesar la factura con AWS Textract");
        assertThat(exception.getMessage()).contains("AWS SDK Error");
    }
}
