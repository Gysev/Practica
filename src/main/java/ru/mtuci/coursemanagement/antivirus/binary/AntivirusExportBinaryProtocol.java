package ru.mtuci.coursemanagement.antivirus.binary;

/** Константы бинарного протокола выгрузки (практика 1.5). */
public final class AntivirusExportBinaryProtocol {

    /** Версия макета подписанного префикса манифеста (байты перед полем ЭЦП). */
    public static final int MANIFEST_UNSIGNED_FORMAT_VERSION = 1;

    public static final String MANIFEST_MAGIC = "AVBM";

    /** Полная размер без удалённых (как JSON /export/full). */
    public static final byte EXPORT_KIND_FULL_ACTIVE = 1;

    /** Инкремент: {@code updatedAt > sinceExclusive}, включая deleted. */
    public static final byte EXPORT_KIND_INCREMENTAL = 2;

    /**
     * Размер блока данных манифеста, который участвует в ЭЦП: magic (4)
     * + formatVersion LE (uint16) + exportKind LE (uint8) + pad (uint8)
     * + generatedEpochMs LE (uint64) + sinceEpochMs LE (uint64), 0 если full
     * + recordCount LE (uint32) + payloadSize LE (uint64) + payloadCrc32 LE (uint32, IEEE как long).
     */
    public static final int UNSIGNED_MANIFEST_BYTES_FOR_SIGNING =
            4 + 2 + 1 + 1 + 8 + 8 + 4 + 8 + 4;

    /** Полный манифест = подписанный префикс + длина PKCS#1 (uint32 LE) + сигнатура. */

    private AntivirusExportBinaryProtocol() {}
}
