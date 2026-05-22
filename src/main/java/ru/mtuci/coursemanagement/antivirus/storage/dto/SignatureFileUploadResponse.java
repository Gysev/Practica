package ru.mtuci.coursemanagement.antivirus.storage.dto;

import java.time.Instant;

public record SignatureFileUploadResponse(
        Long id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String sha256Hex,
        Instant uploadedAt,
        String objectKey,
        String bucket) {}
