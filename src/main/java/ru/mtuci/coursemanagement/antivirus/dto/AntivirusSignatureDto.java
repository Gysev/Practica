package ru.mtuci.coursemanagement.antivirus.dto;

import ru.mtuci.coursemanagement.antivirus.model.AntivirusSignature;

import java.time.Instant;

public record AntivirusSignatureDto(
        Long id,
        String name,
        String content,
        int version,
        String edsSignature,
        boolean deleted,
        Instant deletedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static AntivirusSignatureDto fromEntity(AntivirusSignature s) {
        return new AntivirusSignatureDto(
                s.getId(),
                s.getName(),
                s.getContent(),
                s.getVersion(),
                s.getEdsSignature(),
                s.isDeleted(),
                s.getDeletedAt(),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
