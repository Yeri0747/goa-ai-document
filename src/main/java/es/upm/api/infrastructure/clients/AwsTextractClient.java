package es.upm.api.infrastructure.clients;

import es.upm.api.data.entities.Invoice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.AnalyzeExpenseRequest;
import software.amazon.awssdk.services.textract.model.AnalyzeExpenseResponse;
import software.amazon.awssdk.services.textract.model.Document;

@Service
public class AwsTextractClient {
    private final TextractClient textractClient;

    public AwsTextractClient(@Value("${aws.region:eu-west-1}") String region) {
        this.textractClient = TextractClient.builder()
                .region(Region.of(region))
                .build();
    }

    public Invoice extractInvoice(byte[] fileBytes) {
        try {
            return TextractExpenseMapper.map(analyzeExpense(fileBytes));
        } catch (TextractExtractionException e) {
            throw e;
        } catch (Exception e) {
            throw new TextractExtractionException(
                    "Error al procesar la factura con AWS Textract: " + e.getMessage(), e);
        }
    }

    AnalyzeExpenseResponse analyzeExpense(byte[] fileBytes) {
        AnalyzeExpenseRequest request = AnalyzeExpenseRequest.builder()
                .document(Document.builder()
                        .bytes(SdkBytes.fromByteArray(fileBytes))
                        .build())
                .build();
        return this.textractClient.analyzeExpense(request);
    }
}
