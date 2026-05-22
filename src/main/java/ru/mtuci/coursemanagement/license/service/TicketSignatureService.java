package ru.mtuci.coursemanagement.license.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mtuci.coursemanagement.eds.EdsSigningMaterials;
import ru.mtuci.coursemanagement.eds.jcs.Rfc8785TicketCanon;
import ru.mtuci.coursemanagement.license.dto.Ticket;

import java.security.Signature;
import java.util.Base64;

/**
 * ЭЦП ответа лицензирования: RFC 8785 (JCS) по полям билета + RSA-SHA256 ({@link java.security.Signature}).
 */
@Service
@RequiredArgsConstructor
public class TicketSignatureService {

    private final EdsSigningMaterials materials;

    public String signTicket(Ticket ticket) throws Exception {
        byte[] payload = Rfc8785TicketCanon.canonicalUtf8Octets(ticket);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(materials.privateKey());
        sig.update(payload);
        return Base64.getEncoder().encodeToString(sig.sign());
    }

    public boolean verifyTicket(Ticket ticket, String electronicSignatureBase64) throws Exception {
        byte[] payload = Rfc8785TicketCanon.canonicalUtf8Octets(ticket);
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(materials.publicKey());
        verifier.update(payload);
        byte[] sig = Base64.getDecoder().decode(electronicSignatureBase64);
        return verifier.verify(sig);
    }
}
