package ru.mtuci.coursemanagement.antivirus.binary;

import ru.mtuci.coursemanagement.antivirus.binary.io.LeSink;
import ru.mtuci.coursemanagement.eds.EdsDetachedSigner;

import java.time.Instant;

public final class AntivirusManifestCodec {

    private AntivirusManifestCodec() {}

    public static byte[] buildSignedManifest(
            byte exportKind,
            Instant generatedAt,
            long sinceExclusiveMillisOrZero,
            int recordCount,
            AntivirusPayloadCodec.PayloadAndCrc payload,
            EdsDetachedSigner eds)
            throws Exception {

        byte[] signingBlock = signingBlock(
                exportKind, generatedAt, sinceExclusiveMillisOrZero, recordCount, payload);
        byte[] pkcs1 = eds.signBytesPkcs1(signingBlock);

        LeSink full = new LeSink();
        full.bytes(signingBlock);
        full.u32(pkcs1.length);
        full.bytes(pkcs1);
        return full.toByteArray();
    }

    /** Блок данных, который подписывается SHA256withRSA. */
    public static byte[] signingBlock(
            byte exportKind,
            Instant generatedAt,
            long sinceExclusiveMillisOrZero,
            int recordCount,
            AntivirusPayloadCodec.PayloadAndCrc payload) {

        LeSink blk = new LeSink();
        blk.magic(AntivirusExportBinaryProtocol.MANIFEST_MAGIC);
        blk.u16(AntivirusExportBinaryProtocol.MANIFEST_UNSIGNED_FORMAT_VERSION);

        blk.i8(Byte.toUnsignedInt(exportKind));

        blk.i8(0); // зарезервировано под расширения

        blk.i64(generatedAt.toEpochMilli());
        blk.i64(sinceExclusiveMillisOrZero);
        blk.i32(recordCount);
        blk.i64(payload.payload().length);
        blk.u32(payload.crc32IeeeUnsigned());

        byte[] signing = blk.toByteArray();
        if (signing.length != AntivirusExportBinaryProtocol.UNSIGNED_MANIFEST_BYTES_FOR_SIGNING) {
            throw new IllegalStateException("Unexpected manifest signing block length " + signing.length);
        }
        return signing;
    }
}
