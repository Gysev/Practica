package ru.mtuci.coursemanagement.antivirus.dto;

import ru.mtuci.coursemanagement.antivirus.model.AntivirusSignature;

public record SnapshotDto(
        Long id,
        String name,
        String content,
        int version,
        boolean deleted,
        String edsSignature
) {

    public static SnapshotDto fromEntity(AntivirusSignature s) {
        return new SnapshotDto(
                s.getId(),
                s.getName(),
                s.getContent(),
                s.getVersion(),
                s.isDeleted(),
                s.getEdsSignature());
    }
}
