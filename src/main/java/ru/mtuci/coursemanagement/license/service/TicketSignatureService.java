package ru.mtuci.coursemanagement.license.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mtuci.coursemanagement.license.config.KeyPairHolder;
import ru.mtuci.coursemanagement.license.dto.Ticket;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * Формирует каноническое JSON-представление билета и подпись RSA-SHA256 (ЭЦП).
 */
@Service
@RequiredArgsConstructor
public class TicketSignatureService {

    private static final ObjectMapper CANON_JSON = new ObjectMapper()
            .findAndRegisterModules()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private final KeyPairHolder signingKeys;

    public String signTicket(Ticket ticket) throws Exception {
        byte[] payloadBytes = canonicalJsonUtf8Bytes(ticket);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(signingKeys.privateKey());
        sig.update(payloadBytes);
        return Base64.getEncoder().encodeToString(sig.sign());
    }

    /**
     * Каноническое тело подписи: TreeMap ключей ASCII + те же строковые представления времени что и в Instant.toString().
     */
    Map<String, Object> canonicalMap(Ticket ticket) {
        TreeMap<String, Object> canonical = new TreeMap<>();
        canonical.put("deviceId", ticket.deviceId());
        canonical.put("licenseActivationDate", ticket.licenseActivationDate() != null
                ? ticket.licenseActivationDate().toString()
                : "null");
        canonical.put("licenseBlocked", ticket.licenseBlocked());
        canonical.put("licenseExpirationDate", ticket.licenseExpirationDate() != null
                ? ticket.licenseExpirationDate().toString()
                : "null");
        canonical.put("serverDate", ticket.serverDate().toString());
        canonical.put("ticketLifetimeSeconds", ticket.ticketLifetimeSeconds());
        canonical.put("userId", ticket.userId());
        return canonical;
    }

    public byte[] canonicalJsonUtf8Bytes(Ticket ticket) throws Exception {
        return CANON_JSON.writeValueAsString(canonicalMap(ticket)).getBytes(StandardCharsets.UTF_8);
    }

    /** Проверка подписи (удобно для тестов). */
    public boolean verifyTicket(Ticket ticket, String electronicSignatureBase64) throws Exception {
        byte[] payloadBytes = canonicalJsonUtf8Bytes(ticket);
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(signingKeys.publicKey());
        verifier.update(payloadBytes);
        byte[] sig = Base64.getDecoder().decode(electronicSignatureBase64);
        return verifier.verify(sig);
    }
}
