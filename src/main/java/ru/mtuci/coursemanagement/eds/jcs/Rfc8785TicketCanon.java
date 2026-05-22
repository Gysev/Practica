package ru.mtuci.coursemanagement.eds.jcs;

import ru.mtuci.coursemanagement.license.dto.Ticket;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Подписываемый payload билета как UTF-8 октеты по RFC 8785 (JCS).
 */
public final class Rfc8785TicketCanon {

    private Rfc8785TicketCanon() {
    }

    public static Map<String, Object> ticketPayloadMap(Ticket ticket) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("deviceId", ticket.deviceId());
        m.put("licenseActivationDate",
                ticket.licenseActivationDate() != null ? ticket.licenseActivationDate().toString() : null);
        m.put("licenseBlocked", ticket.licenseBlocked());
        m.put("licenseExpirationDate",
                ticket.licenseExpirationDate() != null ? ticket.licenseExpirationDate().toString() : null);
        m.put("serverDate", ticket.serverDate().toString());
        m.put("ticketLifetimeSeconds", ticket.ticketLifetimeSeconds());
        m.put("userId", ticket.userId());
        return m;
    }

    public static byte[] canonicalUtf8Octets(Ticket ticket) throws Exception {
        return EdsJsonCanon.canonicalUtf8(ticketPayloadMap(ticket));
    }

    /** Для диагностики и тестов: канонический JSON строкой. */
    public static String canonicalJsonUtf8(Ticket ticket) throws Exception {
        return new String(canonicalUtf8Octets(ticket), StandardCharsets.UTF_8);
    }
}
