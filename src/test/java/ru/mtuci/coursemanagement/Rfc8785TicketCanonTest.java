package ru.mtuci.coursemanagement;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.mtuci.coursemanagement.eds.jcs.Rfc8785TicketCanon;
import ru.mtuci.coursemanagement.license.dto.Ticket;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

class Rfc8785TicketCanonTest {

    /**
     * JCS производит воспроизводимые октеты: те же входные поля → те же UTF-8 байты.
     */
    @Test
    void canonicalizationIsDeterministic() throws Exception {
        Ticket t = new Ticket(
                Instant.parse("2026-01-01T12:00:00Z"),
                300L,
                Instant.parse("2025-06-01T00:00:00Z"),
                Instant.parse("2026-12-31T23:59:59Z"),
                42L,
                "device-abc",
                false);
        byte[] a = Rfc8785TicketCanon.canonicalUtf8Octets(t);
        byte[] b = Rfc8785TicketCanon.canonicalUtf8Octets(t);
        Assertions.assertThat(a).isEqualTo(b);
        Assertions.assertThat(new String(a, StandardCharsets.UTF_8)).contains("device-abc");
    }
}
