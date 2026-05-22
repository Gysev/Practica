package ru.mtuci.coursemanagement.eds;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

/**
 * Материалы ЭЦП: закрытый ключ подписи, открытый ключ и (при наличии) сертификат X.509 из PKCS#12 / dev.
 */
public record EdsSigningMaterials(PrivateKey privateKey, PublicKey publicKey, X509Certificate signingCertificate) {
}
