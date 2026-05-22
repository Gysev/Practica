package ru.mtuci.coursemanagement.antivirus.dto;

import ru.mtuci.coursemanagement.antivirus.model.AntivirusSignatureAudit;

import java.time.Instant;

public record AntivirusSignatureAuditDto(
        Long id,
        Long signatureId,
        String action,
        String details,
        Instant createdAt,
        String performedBy
) {
    public static AntivirusSignatureAuditDto from(AntivirusSignatureAudit a) {
        return new AntivirusSignatureAuditDto(
                a.getId(),
                a.getSignatureId(),
                a.getAction().name(),
                a.getDetails(),
                a.getCreatedAt(),
                a.getPerformedBy());
    }
}
