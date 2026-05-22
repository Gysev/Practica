package ru.mtuci.coursemanagement.antivirus.crypto;

import ru.mtuci.coursemanagement.antivirus.model.AntivirusSignature;
import ru.mtuci.coursemanagement.eds.jcs.EdsJsonCanon;

import java.util.Map;
import java.util.TreeMap;

public final class AntivirusEdsPayload {

    private AntivirusEdsPayload() {
    }

    public static Map<String, Object> signFieldMap(AntivirusSignature s) {
        return signFieldMap(s.getId(), s.getName(), s.getContent(), s.getVersion());
    }

    public static Map<String, Object> signFieldMap(Long signatureId, String name, String content, int version) {
        Map<String, Object> m = new TreeMap<>();
        m.put("content", content);
        m.put("name", name);
        m.put("signatureId", signatureId);
        m.put("version", version);
        return m;
    }

    public static byte[] canonicalUtf8(Long signatureId, String name, String content, int version) throws Exception {
        return EdsJsonCanon.canonicalUtf8(signFieldMap(signatureId, name, content, version));
    }

    public static byte[] canonicalUtf8(AntivirusSignature s) throws Exception {
        return canonicalUtf8(s.getId(), s.getName(), s.getContent(), s.getVersion());
    }
}
