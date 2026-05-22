package ru.mtuci.coursemanagement.antivirus.storage.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "signature.minio", name = "enabled", havingValue = "true")
public class SignatureMinioBeans {

    @Bean
    public MinioClient minioClient(SignatureMinioProperties p) {
        if (!StringUtils.hasText(p.getAccessKey()) || !StringUtils.hasText(p.getSecretKey())) {
            throw new IllegalStateException(
                    "При signature.minio.enabled=true задайте signature.minio.access-key и secret-key "
                            + "(или MINIO_ACCESS_KEY / MINIO_SECRET_KEY). Нельзя использовать root MinIO приложением.");
        }
        return MinioClient.builder()
                .endpoint(p.getEndpoint())
                .region(p.getRegion())
                .credentials(p.getAccessKey(), p.getSecretKey())
                .build();
    }

    @Bean
    ApplicationRunner signatureMinioEnsureBucket(MinioClient client, SignatureMinioProperties p) {
        return (ApplicationArguments args) -> {
            String bucket = p.getBucket();
            boolean ok = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!ok) {
                log.warn("Бакет «{}» не найден — создаём приложением (для минимальных инсталляций).", bucket);
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        };
    }
}
