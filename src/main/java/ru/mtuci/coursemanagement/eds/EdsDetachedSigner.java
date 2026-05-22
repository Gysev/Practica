package ru.mtuci.coursemanagement.eds;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Signature;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EdsDetachedSigner {

    private final EdsSigningMaterials materials;

    /** ЭЦП (RSA SHA256), Base64 PKCS#1. */
    public String signRawPayloadUtf8(byte[] canonicalUtf8Payload) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(materials.privateKey());
        sig.update(canonicalUtf8Payload);
        return Base64.getEncoder().encodeToString(sig.sign());
    }

    public boolean verifyRawPayloadUtf8(byte[] canonicalUtf8Payload, String electronicSignatureBase64) throws Exception {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(materials.publicKey());
        verifier.update(canonicalUtf8Payload);
        byte[] decoded = Base64.getDecoder().decode(electronicSignatureBase64);
        return verifier.verify(decoded);
    }
}
