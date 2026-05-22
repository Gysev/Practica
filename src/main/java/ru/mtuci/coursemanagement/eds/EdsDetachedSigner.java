package ru.mtuci.coursemanagement.eds;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Signature;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EdsDetachedSigner {

    private final EdsSigningMaterials materials;

    /** ЭЦП (RSA SHA256), Base64 PKCS#1. Удобна для текстовых/JSON канонических UTF-8 нагрузок. */
    public String signRawPayloadUtf8(byte[] canonicalUtf8Payload) throws Exception {
        byte[] pkcs1 = signBytesPkcs1(canonicalUtf8Payload);
        return Base64.getEncoder().encodeToString(pkcs1);
    }

    public boolean verifyRawPayloadUtf8(byte[] canonicalUtf8Payload, String electronicSignatureBase64)
            throws Exception {
        byte[] decoded = Base64.getDecoder().decode(electronicSignatureBase64);
        return verifyBytesPkcs1(canonicalUtf8Payload, decoded);
    }

    /**
     * Отсоединённая подпись произвольного бинарного блока SHA256withRSA (байты PKCS#1).
     * Для подписания бинарного манифеста выгрузки сигнатур (практика 1.5).
     */
    public byte[] signBytesPkcs1(byte[] payload) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(materials.privateKey());
        sig.update(payload);
        return sig.sign();
    }

    public boolean verifyBytesPkcs1(byte[] payload, byte[] pkcs1Signature) throws Exception {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(materials.publicKey());
        verifier.update(payload);
        return verifier.verify(pkcs1Signature);
    }
}
