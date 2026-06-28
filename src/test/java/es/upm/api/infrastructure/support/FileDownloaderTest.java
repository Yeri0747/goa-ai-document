package es.upm.api.infrastructure.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FileDownloaderTest {

  @Test
  void downloadFileRejectsBlankUrl() {
    FileDownloader fileDownloader = new FileDownloader();

    assertThrows(IOException.class, () -> fileDownloader.downloadFile(null));
    assertThrows(IOException.class, () -> fileDownloader.downloadFile(""));
    assertThrows(IOException.class, () -> fileDownloader.downloadFile("   "));
  }
}
