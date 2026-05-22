package ru.mtuci.coursemanagement.antivirus.storage.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.mtuci.coursemanagement.antivirus.storage.dto.PresignedUrlItem;
import ru.mtuci.coursemanagement.antivirus.storage.dto.PresignedUrlsRequest;
import ru.mtuci.coursemanagement.antivirus.storage.dto.SignatureFileUploadResponse;
import ru.mtuci.coursemanagement.antivirus.storage.service.AntivirusSignatureFileService;

import java.util.List;

/**
 * REST для задания 1.6: загрузка файла с расчётом SHA-256 и выдача pre-signed GET для приватного MinIO.
 * Включается только при {@code signature.minio.enabled=true}; доступ см. {@code SecurityConfiguration}.
 */
@RestController
@RequestMapping("/api/admin/antivirus-signature-files")
@RequiredArgsConstructor
@Validated
@ConditionalOnProperty(prefix = "signature.minio", name = "enabled", havingValue = "true")
public class AntivirusSignatureFileAdminController {

    private final AntivirusSignatureFileService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SignatureFileUploadResponse upload(
            @RequestParam("file") MultipartFile file, Authentication authentication) {
        return service.upload(file, authentication);
    }

    @PostMapping(
            path = "/presigned-download-urls",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PresignedUrlItem> presignedDownloadUrls(@Valid @RequestBody PresignedUrlsRequest body) {
        return service.presignedGetUrls(body.ids());
    }
}
