package ru.mtuci.coursemanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.mtuci.coursemanagement.antivirus.binary.AntivirusExportBinaryProtocol;
import ru.mtuci.coursemanagement.eds.EdsDetachedSigner;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "app.seed-demo-users=true")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AntivirusBinaryExportIntegrationTest {

    private static final byte[] CRLFCRLF = "\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EdsDetachedSigner edsDetachedSigner;

    @Test
    void binaryMultipartFullVerifiesEdsCrcAndStructure() throws Exception {
        String token = bearerToken("teacher", "password");
        String uniq = "bin-" + UUID.randomUUID();

        mockMvc.perform(post("/api/antivirus-signatures")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + uniq + "\",\"content\":\"z\"}"))
                .andExpect(status().isOk());

        MultipartBodies bodies = bodiesFromMvc(getBinaryFull(token));

        assertManifestAndPayloadConsistency(bodies, AntivirusExportBinaryProtocol.EXPORT_KIND_FULL_ACTIVE);

        edsDetachedSigner.verifyBytesPkcs1(bodies.manifestPrefixUnsigned(), bodies.pkcs1Signature());
        assertPayloadContainsRecordNamed(bodies.payload, uniq, false);
    }

    @Test
    void incrementalMultipartFlagsDeletedSignature() throws Exception {
        String token = bearerToken("teacher", "password");
        String uniq = "bin-del-" + UUID.randomUUID();

        MvcResult createRes = mockMvc.perform(post("/api/antivirus-signatures")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + uniq + "\",\"content\":\"x\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode created = objectMapper.readTree(createRes.getResponse().getContentAsString());
        long id = created.get("id").asLong();

        Instant afterCreate = Instant.parse(created.get("updatedAt").asText()).truncatedTo(ChronoUnit.MILLIS);
        mockMvc.perform(delete("/api/antivirus-signatures/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        Instant sinceExclusive = afterCreate.minusMillis(1);
        MockHttpServletResponse rsp =
                mockMvc.perform(get("/api/antivirus-signatures/export/binary/incremental")
                                .header("Authorization", "Bearer " + token)
                                .param("since", sinceExclusive.toString()))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();

        MultipartBodies bodies = MultipartBodies.parse(rsp);
        ParsedManifest mh = ParsedManifest.scanUnsignedPrefix(bodies.manifest);
        assertThat(mh.exportKind).isEqualTo(AntivirusExportBinaryProtocol.EXPORT_KIND_INCREMENTAL);
        edsDetachedSigner.verifyBytesPkcs1(bodies.manifestPrefixUnsigned(), bodies.pkcs1Signature());
        assertManifestAndPayloadConsistency(bodies, AntivirusExportBinaryProtocol.EXPORT_KIND_INCREMENTAL);
        assertPayloadContainsRecordNamed(bodies.payload, uniq, true);
    }

    private MvcResult getBinaryFull(String token) throws Exception {
        return mockMvc.perform(get("/api/antivirus-signatures/export/binary/full")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
    }

    private MultipartBodies bodiesFromMvc(MvcResult res) {
        return MultipartBodies.parse(res.getResponse());
    }

    private void assertManifestAndPayloadConsistency(MultipartBodies bodies, byte expectedExportKind) {
        assertThat(new String(Arrays.copyOfRange(bodies.manifest, 0, 4), StandardCharsets.US_ASCII))
                .isEqualTo(AntivirusExportBinaryProtocol.MANIFEST_MAGIC);
        ParsedManifest mh = ParsedManifest.scanUnsignedPrefix(bodies.manifest);
        assertThat(mh.exportKind).isEqualTo(expectedExportKind);

        CRC32 crc = new CRC32();
        crc.update(bodies.payload);
        assertThat(mh.recordCount).isGreaterThanOrEqualTo(1);
        assertThat(mh.payloadSize).as("длины payload совпадают").isEqualTo(bodies.payload.length);
        assertThat(crc.getValue()).isEqualTo(mh.crc32Unsigned);
    }

    private record MultipartBodies(byte[] manifest, byte[] payload) {

        byte[] manifestPrefixUnsigned() {
            return Arrays.copyOfRange(manifest, 0, AntivirusExportBinaryProtocol.UNSIGNED_MANIFEST_BYTES_FOR_SIGNING);
        }

        byte[] pkcs1Signature() {
            ByteBuffer tail = ByteBuffer.wrap(
                            manifest,
                            AntivirusExportBinaryProtocol.UNSIGNED_MANIFEST_BYTES_FOR_SIGNING,
                            manifest.length
                                    - AntivirusExportBinaryProtocol.UNSIGNED_MANIFEST_BYTES_FOR_SIGNING)
                    .order(ByteOrder.LITTLE_ENDIAN);
            int len = tail.getInt();
            byte[] sig = new byte[len];
            tail.get(sig);
            assertThat(tail.hasRemaining()).isFalse();
            return sig;
        }

        static MultipartBodies parse(MockHttpServletResponse rsp) {
            String ct = rsp.getContentType();
            assertThat(ct).isNotBlank();
            assertThat(ct.toLowerCase(Locale.ROOT)).contains("multipart/mixed");
            Matcher m = Pattern.compile("(?i)boundary=([^;\\s]+)").matcher(ct);
            assertThat(m.find()).isTrue();
            String boundary = m.group(1).trim();

            byte[] raw = rsp.getContentAsByteArray();

            byte[] opener = ("--" + boundary + "\r\n").getBytes(StandardCharsets.US_ASCII);
            assertThat(regionStartsWith(raw, 0, opener)).isTrue();
            int firstBodyBegin = indexOf(raw, CRLFCRLF, opener.length) + CRLFCRLF.length;

            byte[] sep = ("\r\n--" + boundary + "\r\n").getBytes(StandardCharsets.US_ASCII);
            int sepIdx = indexOf(raw, sep, firstBodyBegin);
            byte[] manifest = Arrays.copyOfRange(raw, firstBodyBegin, sepIdx);

            int secondHeadersStart = sepIdx + sep.length;
            int secondBodyBegin = indexOf(raw, CRLFCRLF, secondHeadersStart) + CRLFCRLF.length;

            byte[] closer = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII);
            int closerIdx = indexOf(raw, closer, secondBodyBegin);
            byte[] payload = Arrays.copyOfRange(raw, secondBodyBegin, closerIdx);

            return new MultipartBodies(manifest, payload);
        }
    }

    private record ParsedManifest(byte exportKind, int recordCount, long payloadSize, long crc32Unsigned) {

        static ParsedManifest scanUnsignedPrefix(byte[] manifest) {
            ByteBuffer bb =
                    ByteBuffer.wrap(manifest, 0, AntivirusExportBinaryProtocol.UNSIGNED_MANIFEST_BYTES_FOR_SIGNING)
                            .order(ByteOrder.LITTLE_ENDIAN);
            bb.position(6); // после magic и format_version LE
            byte kind = bb.get();
            bb.get(); // резерв

            bb.getLong();
            bb.getLong();

            int rec = bb.getInt();
            long psz = bb.getLong();
            int crc = bb.getInt();
            return new ParsedManifest(kind, rec, psz, Integer.toUnsignedLong(crc));
        }
    }

    private static void assertPayloadContainsRecordNamed(byte[] payload, String name, boolean deletedExpected) {
        byte[] needle = name.getBytes(StandardCharsets.UTF_8);
        ByteBuffer bb = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        while (bb.hasRemaining()) {
            bb.getLong();
            bb.getInt();
            int flags = Short.toUnsignedInt(bb.getShort());
            bb.getLong();
            byte[] nm = takePrefixed(bb);
            takePrefixed(bb);
            takePrefixed(bb);

            if (Arrays.equals(nm, needle)) {
                assertThat((flags & 1) == 1).isEqualTo(deletedExpected);
                return;
            }
        }
        throw new AssertionError("В payload нет записи с именем \"" + name + "\"");
    }

    private static byte[] takePrefixed(ByteBuffer bb) {
        int len = bb.getInt();
        assertThat(len).isGreaterThanOrEqualTo(0);
        byte[] chunk = new byte[len];
        bb.get(chunk);
        return chunk;
    }

    private static int indexOf(byte[] data, byte[] needle, int from) {
        outer:
        for (int i = from; i + needle.length <= data.length; i++) {
            for (int k = 0; k < needle.length; k++) {
                if (data[i + k] != needle[k]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static boolean regionStartsWith(byte[] data, int offset, byte[] prefix) {
        if (offset + prefix.length > data.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private String bearerToken(String user, String pass) throws Exception {
        MvcResult res =
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}"))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode node = objectMapper.readTree(res.getResponse().getContentAsString());
        return node.get("accessToken").asText();
    }
}
