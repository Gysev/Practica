package ru.mtuci.coursemanagement.antivirus.storage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "antivirus_signature_files",
        indexes = {@Index(name = "idx_antivirus_signature_files_sha", columnList = "sha256Hex"),
                @Index(name = "idx_antivirus_signature_files_created", columnList = "createdAt")})
@Getter
@Setter
@NoArgsConstructor
public class AntivirusSignatureFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 512)
    private String originalFilename;

    @Column(nullable = false, length = 256)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    /** SHA-256 содержимого файла в hex — «сигнатура содержимого» для задачи 1.6. */
    @Column(nullable = false, length = 64)
    private String sha256Hex;

    @Column(nullable = false, length = 768)
    private String objectKey;

    @Column(nullable = false, length = 256)
    private String bucketName;

    @Column(nullable = false, length = 128)
    private String uploadedBy;
}
