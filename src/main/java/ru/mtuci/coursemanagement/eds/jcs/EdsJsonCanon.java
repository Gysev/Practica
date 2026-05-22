package ru.mtuci.coursemanagement.eds.jcs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.erdtman.jcs.JsonCanonicalizer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * RFC 8785 (JCS): произвольный JSON-объект → канонические UTF-8 байты для ЭЦП.
 */
public final class EdsJsonCanon {

    static final ObjectMapper TO_JSON = new ObjectMapper()
            .findAndRegisterModules()
            .setSerializationInclusion(JsonInclude.Include.ALWAYS);

    private EdsJsonCanon() {
    }

    public static byte[] canonicalUtf8(Map<String, Object> objectFields) throws Exception {
        String json = TO_JSON.writeValueAsString(objectFields);
        return new JsonCanonicalizer(json.getBytes(StandardCharsets.UTF_8)).getEncodedUTF8();
    }

    /** Для точечных утилит/логирования без подписания. */
    public static ObjectMapper mapper() {
        return TO_JSON;
    }
}
