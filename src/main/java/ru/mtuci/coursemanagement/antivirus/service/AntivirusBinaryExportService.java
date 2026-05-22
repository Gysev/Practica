package ru.mtuci.coursemanagement.antivirus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mtuci.coursemanagement.antivirus.binary.AntivirusExportBinaryProtocol;
import ru.mtuci.coursemanagement.antivirus.binary.AntivirusManifestCodec;
import ru.mtuci.coursemanagement.antivirus.binary.AntivirusPayloadCodec;
import ru.mtuci.coursemanagement.antivirus.binary.MultipartMixedWriter;
import ru.mtuci.coursemanagement.antivirus.model.AntivirusSignature;
import ru.mtuci.coursemanagement.antivirus.repository.AntivirusSignatureRepository;
import ru.mtuci.coursemanagement.eds.EdsDetachedSigner;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Формирует бинарную выдачу сигнатур (манифест + payload) как {@link org.springframework.http.MediaType#MULTIPART_MIXED}.
 */
@Service
@RequiredArgsConstructor
public class AntivirusBinaryExportService {

    private final AntivirusSignatureRepository signatureRepository;
    private final EdsDetachedSigner edsDetachedSigner;

    public record BinaryMultipartEnvelope(byte[] body, String boundary) {}

    @Transactional(readOnly = true)
    public BinaryMultipartEnvelope multipartFullExport() throws Exception {
        Instant generated = Instant.now();
        List<AntivirusSignature> list = signatureRepository.findAllByDeletedFalseOrderByIdAsc();
        AntivirusPayloadCodec.PayloadAndCrc encoded = AntivirusPayloadCodec.encode(list);
        byte[] manifest = AntivirusManifestCodec.buildSignedManifest(
                AntivirusExportBinaryProtocol.EXPORT_KIND_FULL_ACTIVE,
                generated,
                0L,
                list.size(),
                encoded,
                edsDetachedSigner);

        String boundary = "avboundary_" + UUID.randomUUID();
        byte[] raw = MultipartMixedWriter.compose(boundary, manifest, encoded.payload());
        return new BinaryMultipartEnvelope(raw, boundary);
    }

    @Transactional(readOnly = true)
    public BinaryMultipartEnvelope multipartIncrementalExport(Instant sinceExclusive) throws Exception {
        Instant generated = Instant.now();
        List<AntivirusSignature> list =
                signatureRepository.findAllByUpdatedAtAfterOrderByUpdatedAtAsc(sinceExclusive);
        AntivirusPayloadCodec.PayloadAndCrc encoded = AntivirusPayloadCodec.encode(list);

        byte[] manifest = AntivirusManifestCodec.buildSignedManifest(
                AntivirusExportBinaryProtocol.EXPORT_KIND_INCREMENTAL,
                generated,
                sinceExclusive.toEpochMilli(),
                list.size(),
                encoded,
                edsDetachedSigner);

        String boundary = "avboundary_" + UUID.randomUUID();
        byte[] raw = MultipartMixedWriter.compose(boundary, manifest, encoded.payload());
        return new BinaryMultipartEnvelope(raw, boundary);
    }
}
