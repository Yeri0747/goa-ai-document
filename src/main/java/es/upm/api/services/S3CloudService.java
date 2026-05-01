package es.upm.api.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

@Service
public class S3CloudService {
    private final S3Client s3Client;

    @Value("${aws.s3.bucket:goa-ai-documents}")
    private String bucketName;

    public S3CloudService(@Value("${aws.region:eu-west-1}") String region) {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    public String uploadFile(MultipartFile file) {
        String key = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, 
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return "https://" + bucketName + ".s3.amazonaws.com/" + key;
        } catch (IOException e) {
            throw new RuntimeException("Error uploading file to S3", e);
        }
    }
}
