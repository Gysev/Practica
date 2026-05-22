package ru.mtuci.coursemanagement.antivirus.binary;

import ru.mtuci.coursemanagement.antivirus.binary.io.LeSink;
import ru.mtuci.coursemanagement.antivirus.model.AntivirusSignature;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.CRC32;

public final class AntivirusPayloadCodec {

    private AntivirusPayloadCodec() {}

    public record PayloadAndCrc(byte[] payload, long crc32IeeeUnsigned) {}

    /** Упаковка списка сигнатур в непрерывный payload (little-endian см. кодек). */
    public static PayloadAndCrc encode(List<AntivirusSignature> signatures) {
        LeSink sink = new LeSink();
        for (AntivirusSignature s : signatures) {
            writeRecord(sink, s);
        }
        byte[] payload = sink.toByteArray();
        CRC32 crc32 = new CRC32();
        crc32.update(payload);
        return new PayloadAndCrc(payload, crc32.getValue());
    }

    private static void writeRecord(LeSink out, AntivirusSignature s) {
        out.i64(s.getId());
        out.i32(s.getVersion());

        int flags = s.isDeleted() ? 1 : 0;
        out.u16(flags);

        out.i64(s.getUpdatedAt().toEpochMilli());

        byte[] nameUtf8 = s.getName().getBytes(StandardCharsets.UTF_8);
        byte[] contentUtf8 = s.getContent().getBytes(StandardCharsets.UTF_8);
        byte[] edsAscii = s.getEdsSignature().getBytes(StandardCharsets.US_ASCII);

        out.prefixedUtf8Bytes(nameUtf8);
        out.prefixedUtf8Bytes(contentUtf8);
        out.prefixedAsciiBytes(edsAscii);
    }
}
