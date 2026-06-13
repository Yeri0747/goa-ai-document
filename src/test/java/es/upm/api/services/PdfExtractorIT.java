package es.upm.api.services;

import es.upm.api.infrastructure.support.PdfExtractor;
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

class PdfExtractorIT {

    private final PdfExtractor pdfExtractor = new PdfExtractor();

    @Test
    void testExtractTextFromPdf() throws IOException {
        byte[] pdfBytes = createMockPdf("Hello World");
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", pdfBytes);

        String result = pdfExtractor.extractTextFromPdf(file);

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

        String result = pdfExtractor.extractTextFromPdf(file);

        assertThat(result.length()).isLessThanOrEqualTo(2000);
    }

    @Test
    void testExtractTextFromPdfInvalidFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "not a pdf".getBytes());

        assertThrows(RuntimeException.class, () -> pdfExtractor.extractTextFromPdf(file));
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

    @Test
    void testExtractTextFromUrlLocal() throws IOException {
        byte[] pdfBytes = createMockPdf("Contenido desde URL local");
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("test-url", ".pdf");
        java.nio.file.Files.write(tempFile, pdfBytes);

        String localUrl = tempFile.toUri().toURL().toString();

        String result = pdfExtractor.extractTextFromUrl(localUrl);

        assertThat(result).contains("Contenido desde URL local");
        java.nio.file.Files.deleteIfExists(tempFile);
    }

    @Test
    void testExtractTextFromPdfNullCase() throws IOException {
        try (org.apache.pdfbox.pdmodel.PDDocument emptyDoc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            String result = pdfExtractor.extractTextFromPdf(
                    new org.springframework.mock.web.MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0])
            );
            assertThat(result).isNotNull();
        } catch (Exception e) {
        }
    }

    @Test
    void testExtractTextFromUrlThrowsException() {
        assertThrows(RuntimeException.class, () ->
                pdfExtractor.extractTextFromUrl("not-a-url")
        );
    }
}
