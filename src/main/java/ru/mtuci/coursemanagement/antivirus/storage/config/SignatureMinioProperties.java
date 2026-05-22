package ru.mtuci.coursemanagement.antivirus.storage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "signature.minio")
public class SignatureMinioProperties {

    /** Включить MinIO + REST подсистему файлов (задание 1.6). */
    private boolean enabled = false;

    private String endpoint = "http://127.0.0.1:9000";

    /** Регион для подписей V4 (часто фиксируют us-east-1 для MinIO). */
    private String region = "us-east-1";

    /** Отдельный сервисный ключ (не root супервизора контейнера). */
    private String accessKey = "";

    private String secretKey = "";

    private String bucket = "antivirus-signature-files";

    private Duration presignTtl = Duration.ofMinutes(15);
}
