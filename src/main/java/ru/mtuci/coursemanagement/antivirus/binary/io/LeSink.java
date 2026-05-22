package ru.mtuci.coursemanagement.antivirus.binary.io;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Поточная запись multi-byte типов в **little-endian** (порядок байт как у Intel/x86).
 */
public final class LeSink {

    private final ByteArrayOutputStream out;

    public LeSink() {
        this(new ByteArrayOutputStream());
    }

    public LeSink(ByteArrayOutputStream out) {
        this.out = out;
    }

    public LeSink ascii(String s) {
        byte[] raw = s.getBytes(StandardCharsets.US_ASCII);
        bytes(raw);
        return this;
    }

    public LeSink utf8(String s) {
        byte[] raw = s.getBytes(StandardCharsets.UTF_8);
        u32(raw.length);
        bytes(raw);
        return this;
    }

    /** UTF-8 длины и тела уже известной длины. */
    public LeSink prefixedUtf8Bytes(byte[] raw) {
        u32(raw.length);
        bytes(raw);
        return this;
    }

    /** Длины и тело в US_ASCII / латиница для Base64-строк. */
    public LeSink prefixedAsciiBytes(byte[] raw) {
        u32(raw.length);
        bytes(raw);
        return this;
    }

    public LeSink bytes(byte[] b) {
        out.writeBytes(b);
        return this;
    }

    public LeSink bytes(byte[] b, int offset, int len) {
        out.write(b, offset, len);
        return this;
    }

    public LeSink magic(String fourAscii) {
        if (fourAscii.length() != 4) {
            throw new IllegalArgumentException("MAGIC должен быть ровно 4 ASCII-символа");
        }
        return ascii(fourAscii);
    }

    public LeSink i8(int v) {
        out.write(v & 0xff);
        return this;
    }

    public LeSink i16(short v) {
        int u = Short.toUnsignedInt(v);
        out.write(u & 0xff);
        out.write((u >>> 8) & 0xff);
        return this;
    }

    public LeSink u16(int unsigned) {
        if (Integer.compareUnsigned(unsigned, 65535) > 0) {
            throw new IllegalArgumentException();
        }
        out.write(unsigned & 0xff);
        out.write((unsigned >>> 8) & 0xff);
        return this;
    }

    public LeSink i32(int v) {
        out.write(v & 0xff);
        out.write((v >>> 8) & 0xff);
        out.write((v >>> 16) & 0xff);
        out.write((v >>> 24) & 0xff);
        return this;
    }

    public LeSink u32(long unsigned) {
        long v = unsigned & 0xffffffffL;
        out.write((int) (v & 0xff));
        out.write((int) ((v >>> 8) & 0xff));
        out.write((int) ((v >>> 16) & 0xff));
        out.write((int) ((v >>> 24) & 0xff));
        return this;
    }

    public LeSink i64(long v) {
        for (int i = 0; i < 8; i++) {
            out.write((int) (v & 0xff));
            v >>>= 8;
        }
        return this;
    }

    public byte[] toByteArray() {
        return out.toByteArray();
    }
}
