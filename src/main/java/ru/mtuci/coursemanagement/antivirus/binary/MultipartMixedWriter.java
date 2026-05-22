package ru.mtuci.coursemanagement.antivirus.binary;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Сборка телa HTTP как {@code multipart/mixed}: две части — манифест и binary payload сигнатур.
 */
public final class MultipartMixedWriter {

    private MultipartMixedWriter() {}

    public static byte[] compose(String boundaryToken, byte[] manifestBytes, byte[] payloadBytes)
            throws java.io.IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        emitPart(raw, boundaryToken, "manifest.bin", manifestBytes);

        ascii(raw, "\r\n--");
        ascii(raw, boundaryToken);
        ascii(raw, "\r\n");
        emitHeaders(raw, "signature-payload.bin");
        ascii(raw, "\r\n\r\n");
        raw.writeBytes(payloadBytes);

        ascii(raw, "\r\n--");
        ascii(raw, boundaryToken);
        ascii(raw, "--\r\n");
        return raw.toByteArray();
    }

    private static void emitPart(
            ByteArrayOutputStream raw, String boundaryToken, String filename, byte[] body)
            throws java.io.IOException {
        ascii(raw, "--");
        ascii(raw, boundaryToken);
        ascii(raw, "\r\n");
        emitHeaders(raw, filename);
        ascii(raw, "\r\n\r\n");
        raw.writeBytes(body);
    }

    private static void emitHeaders(ByteArrayOutputStream raw, String filename) throws java.io.IOException {
        ascii(raw, "Content-Type: application/octet-stream\r\n");
        ascii(raw, "Content-Disposition: inline; filename=\"");
        ascii(raw, filename);
        ascii(raw, "\"");
    }

    private static void ascii(ByteArrayOutputStream raw, String s) throws java.io.IOException {
        raw.write(s.getBytes(StandardCharsets.US_ASCII));
    }
}
