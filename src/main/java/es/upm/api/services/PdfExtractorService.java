package es.upm.api.services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

@Service
public class PdfExtractorService {

    private static final int MAX_PAGES_TO_EXTRACT = 3;
    private static final int MAX_CHARS_TO_EXTRACT = 2000;

    public String extractTextFromPdf(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getInputStream().readAllBytes())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            
            // Limit extraction to the first MAX_PAGES_TO_EXTRACT pages
            pdfStripper.setStartPage(1);
            pdfStripper.setEndPage(MAX_PAGES_TO_EXTRACT);
            
            String text = pdfStripper.getText(document);
            
            if (text == null) {
                return "";
            }
            
            // Further limit the text to MAX_CHARS_TO_EXTRACT characters
            if (text.length() > MAX_CHARS_TO_EXTRACT) {
                return text.substring(0, MAX_CHARS_TO_EXTRACT);
            }
            
            return text.trim();
        } catch (IOException e) {
            throw new RuntimeException("Error extracting text from PDF", e);
        }
    }

    public String extractTextFromUrl(String urlString) {
        try (InputStream in = new URL(urlString).openStream();
             PDDocument document = Loader.loadPDF(in.readAllBytes())) {

            PDFTextStripper pdfStripper = new PDFTextStripper();

            pdfStripper.setStartPage(1);
            pdfStripper.setEndPage(MAX_PAGES_TO_EXTRACT);

            String text = pdfStripper.getText(document);

            if (text == null) {
                return "";
            }

            if (text.length() > MAX_CHARS_TO_EXTRACT) {
                return text.substring(0, MAX_CHARS_TO_EXTRACT);
            }

            return text.trim();

        } catch (IOException e) {
            throw new RuntimeException("Error extracting text from PDF URL: " + urlString, e);
        }
    }
}
