package ru.mtuci.coursemanagement.license.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mtuci.coursemanagement.eds.EdsDetachedSigner;
import ru.mtuci.coursemanagement.eds.jcs.Rfc8785TicketCanon;
import ru.mtuci.coursemanagement.license.dto.Ticket;

@Service
@RequiredArgsConstructor
public class TicketSignatureService {

    private final EdsDetachedSigner detachedSigner;

    public String signTicket(Ticket ticket) throws Exception {
        return detachedSigner.signRawPayloadUtf8(Rfc8785TicketCanon.canonicalUtf8Octets(ticket));
    }

    public boolean verifyTicket(Ticket ticket, String electronicSignatureBase64) throws Exception {
        return detachedSigner.verifyRawPayloadUtf8(
                Rfc8785TicketCanon.canonicalUtf8Octets(ticket), electronicSignatureBase64);
    }
}
