package es.upm.api.infrastructure.support;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

@Service
public class FileDownloader {

    public byte[] downloadFile(String urlString) throws IOException {
        if (urlString == null || urlString.isBlank()) {
            throw new IOException("El URL no puede estar vacío");
        }
        try (InputStream in = new URL(urlString).openStream()) {
            return in.readAllBytes();
        }
    }
}
