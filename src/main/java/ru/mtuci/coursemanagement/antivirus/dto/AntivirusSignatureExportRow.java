package ru.mtuci.coursemanagement.antivirus.dto;

import ru.mtuci.coursemanagement.antivirus.model.AntivirusSignature;

import java.time.Instant;

/** Строка для выгрузок (полной и инкрементальной с отметкой deleted). */
public record AntivirusSignatureExportRow(
        Long id,
        String name,
        String content,
        int version,
        String edsSignature,
        boolean deleted,
        Instant updatedAt
) {
    public static AntivirusSignatureExportRow fromEntity(AntivirusSignature s) {
        return new AntivirusSignatureExportRow(
                s.getId(),
                s.getName(),
                s.getContent(),
                s.getVersion(),
                s.getEdsSignature(),
                s.isDeleted(),
                s.getUpdatedAt());
    }
}
