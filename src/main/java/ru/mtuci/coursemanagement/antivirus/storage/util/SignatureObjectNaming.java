package ru.mtuci.coursemanagement.antivirus.storage.util;

import java.util.UUID;

/** Безопасное имя сегмента для object key в объектном хранилище. */
public final class SignatureObjectNaming {

    private SignatureObjectNaming() {}

    /** Нормализует исходное имя и размещает под префиксом UUID каталога. */
    public static String buildObjectKey(String originalFilename) {
        String base = sanitizeOriginalName(originalFilename);
        return UUID.randomUUID() + "/" + base;
    }

    /**
     * Оставляет относительное имя файла без каталогов, заменяя опасные символы.
     */
    public static String sanitizeOriginalName(String name) {
        if (name == null || name.isBlank()) {
            return "upload.bin";
        }
        String last = name;
        int slash = Math.max(last.lastIndexOf('/'), last.lastIndexOf('\\'));
        if (slash >= 0 && slash < last.length() - 1) {
            last = last.substring(slash + 1);
        }
        if (last.length() > 200) {
            last = last.substring(0, 200);
        }
        return last.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
