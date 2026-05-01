package es.upm.api.configurations;

import es.upm.api.data.daos.DocumentRepository;
import es.upm.api.data.entities.Document;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
@Profile({"dev", "test"})
public class DatabaseSeederDev {

    public static final String DOC_ID_1 = "doc-0001";
    public static final String DOC_ID_2 = "doc-0002";

    private final DocumentRepository documentRepository;

    @PostConstruct
    public void init() {
        this.deleteAllAndInitializeAndSeedDataBase();
    }

    public void deleteAllAndInitializeAndSeedDataBase() {
        this.deleteAllAndInitialize();
        this.seedDataBaseJava();
    }

    private void deleteAllAndInitialize() {
        this.documentRepository.deleteAll();
        log.warn("------- Delete All -----------");
    }

    private void seedDataBaseJava() {
        log.warn("------- Initial Load from JAVA ---------------------------------------------------------------");

        Document[] documents = {
                Document.builder()
                        .id(DOC_ID_1)
                        .name("test1.pdf")
                        .sizeInfo(1024L)
                        .url("https://bucket.s3.amazonaws.com/test1.pdf")
                        .uploadDate(LocalDateTime.now())
                        .build(),
                Document.builder()
                        .id(DOC_ID_2)
                        .name("test2.pdf")
                        .sizeInfo(2048L)
                        .url("https://bucket.s3.amazonaws.com/test2.pdf")
                        .uploadDate(LocalDateTime.now())
                        .build()
        };

        this.documentRepository.saveAll(List.of(documents));
        log.warn("        ------- documents ------------------------------------------------------------------");
    }
}
