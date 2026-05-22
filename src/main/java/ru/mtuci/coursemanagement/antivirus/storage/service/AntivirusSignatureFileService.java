package ru.mtuci.coursemanagement.antivirus.storage.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.mtuci.coursemanagement.antivirus.storage.config.SignatureMinioProperties;
import ru.mtuci.coursemanagement.antivirus.storage.dto.PresignedUrlItem;
import ru.mtuci.coursemanagement.antivirus.storage.dto.SignatureFileUploadResponse;
import ru.mtuci.coursemanagement.antivirus.storage.model.AntivirusSignatureFile;
import ru.mtuci.coursemanagement.antivirus.storage.repository.AntivirusSignatureFileRepository;
import ru.mtuci.coursemanagement.antivirus.storage.util.SignatureObjectNaming;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@ConditionalOnProperty(prefix = "signature.minio", name = "enabled", havingValue = "true")
public class AntivirusSignatureFileService {

    private final MinioClient minioClient;
    private final AntivirusSignatureFileRepository repository;
    private final SignatureMinioProperties props;

    private static String actor(Authentication authentication) {
        return (authentication != null && authentication.isAuthenticated())
                ? authentication.getName()
                : "anonymous";
    }

    @Transactional
    public SignatureFileUploadResponse upload(MultipartFile multipart, Authentication authentication) {
        if (multipart == null || multipart.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пустой файл");
        }

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SHA-256 недоступен", e);
        }

        String objectKey = SignatureObjectNaming.buildObjectKey(multipart.getOriginalFilename());
        String normalizedOriginal =
                SignatureObjectNaming.sanitizeOriginalName(multipart.getOriginalFilename());
        long size = multipart.getSize();

        try (InputStream raw = multipart.getInputStream();
                DigestInputStream dis = new DigestInputStream(raw, digest)) {

            String contentType = StringUtils.hasText(multipart.getContentType())
                    ? multipart.getContentType()
                    : "application/octet-stream";

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(props.getBucket())
                            .object(objectKey)
                            .stream(dis, size, -1)
                            .contentType(contentType)
                            .build());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось сохранить файл в MinIO", e);
        }

        String shaHex = HexFormat.of().formatHex(digest.digest());

        Instant now = Instant.now();
        AntivirusSignatureFile row = new AntivirusSignatureFile();
        row.setCreatedAt(now);
        row.setOriginalFilename(normalizedOriginal);
        row.setContentType(
                multipart.getContentType() != null ? multipart.getContentType() : "application/octet-stream");
        row.setSizeBytes(size);
        row.setSha256Hex(shaHex);
        row.setObjectKey(objectKey);
        row.setBucketName(props.getBucket());
        row.setUploadedBy(actor(authentication));
        AntivirusSignatureFile saved = repository.save(row);

        return new SignatureFileUploadResponse(
                saved.getId(),
                saved.getOriginalFilename(),
                saved.getContentType(),
                saved.getSizeBytes(),
                saved.getSha256Hex(),
                saved.getCreatedAt(),
                saved.getObjectKey(),
                saved.getBucketName());
    }

    /** Pre-signed GET URLs для приватного бакета MinIO. */
    public List<PresignedUrlItem> presignedGetUrls(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Список id пуст");
        }

        long secLong = props.getPresignTtl().toSeconds();
        if (secLong < 1L) {
            secLong = 1L;
        }
        if (secLong > Integer.MAX_VALUE) {
            secLong = Integer.MAX_VALUE;
        }
        int seconds = (int) secLong;

        List<PresignedUrlItem> out = new ArrayList<>(ids.size());
        Instant now = Instant.now();
        Instant expiresAt = now.plus(seconds, ChronoUnit.SECONDS);

        for (Long id : ids) {
            if (id == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Значение id не может быть null");
            }

            AntivirusSignatureFile file = repository
                    .findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Файл не найден id=" + id));

            try {
                String url = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(file.getBucketName())
                                .object(file.getObjectKey())
                                .expiry(seconds, TimeUnit.SECONDS)
                                .build());

                out.add(new PresignedUrlItem(id, url, expiresAt));

            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось сгенерировать URL", e);
            }
        }

        return List.copyOf(out);
    }
}
