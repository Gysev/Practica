package ru.mtuci.coursemanagement.antivirus.dto;

import ru.mtuci.coursemanagement.antivirus.model.AntivirusSignatureHistory;

import java.time.Instant;

public record AntivirusSignatureHistoryDto(
        Long id,
        Long signatureId,
        String changeType,
        String beforeJson,
        String afterJson,
        Instant createdAt,
        String performedBy
) {
    public static AntivirusSignatureHistoryDto from(AntivirusSignatureHistory h) {
        return new AntivirusSignatureHistoryDto(
                h.getId(),
                h.getSignatureId(),
                h.getChangeType().name(),
                h.getBeforeJson(),
                h.getAfterJson(),
                h.getCreatedAt(),
                h.getPerformedBy());
    }
}
