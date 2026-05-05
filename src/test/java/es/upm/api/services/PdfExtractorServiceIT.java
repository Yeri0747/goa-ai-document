package es.upm.api.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfExtractorServiceIT {

    private final PdfExtractorService pdfExtractorService = new PdfExtractorService();

    @Test
    void testExtractTextFromPdf() throws IOException {
        byte[] pdfBytes = createMockPdf("Hello World");
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", pdfBytes);

        String result = pdfExtractorService.extractTextFromPdf(file);

        assertThat(result).contains("Hello World");
    }

    @Test
    void testExtractTextFromPdfTruncation() throws IOException {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            longText.append("Word").append(i).append(" ");
        }
        byte[] pdfBytes = createMockPdf(longText.toString());
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", pdfBytes);

        String result = pdfExtractorService.extractTextFromPdf(file);

        assertThat(result.length()).isLessThanOrEqualTo(2000);
    }

    @Test
    void testExtractTextFromPdfInvalidFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "not a pdf".getBytes());

        assertThrows(RuntimeException.class, () -> pdfExtractorService.extractTextFromPdf(file));
    }

    private byte[] createMockPdf(String content) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText(content);
                contentStream.endText();
            }

            document.save(baos);
            return baos.toByteArray();
        }
    }
}
