package es.upm.api.infrastructure.support;

import es.upm.api.exceptions.BadRequestException;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;

@Service
public class FileDownloader {

    public byte[] downloadFile(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            throw new BadRequestException("El URL no puede estar vacío");
        }
        try (InputStream in = new URL(urlString).openStream()) {
            return in.readAllBytes();
        } catch (Exception e) {
            throw new BadRequestException("Error al descargar el archivo desde la URL: " + urlString);
        }
    }
}
