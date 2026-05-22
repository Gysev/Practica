package ru.mtuci.coursemanagement.eds.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.StringUtils;
import ru.mtuci.coursemanagement.eds.EdsSigningMaterials;
import ru.mtuci.coursemanagement.eds.internal.EdsDevSelfSignedCertificate;
import ru.mtuci.coursemanagement.license.config.LicenseProperties;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

/**
 * Загрузка PKCS#12 (прод и CI через Secrets) или dev-пары RSA + самоподписанный сертификат.
 */
@Configuration
public class EdsSigningBeans {

    private static volatile EdsSigningMaterials cachedDevMaterials;

    @Bean
    public EdsSigningMaterials edsSigningMaterials(LicenseProperties props) throws Exception {
        LicenseProperties.Signing s = props.getSigning();
        if (hasPkcs12(s)) {
            return loadPkcs12(s);
        }
        if (s.isGenerateDevKey()) {
            return devSigningMaterials();
        }
        throw new IllegalStateException(
                "EDS: задайте license.signing.keystore-location + keystore-password "
                        + "или license.signing.generate-dev-key=true для разработки.");
    }

    private static EdsSigningMaterials devSigningMaterials() throws Exception {
        EdsSigningMaterials m = cachedDevMaterials;
        if (m != null) {
            return m;
        }
        synchronized (EdsSigningBeans.class) {
            if (cachedDevMaterials == null) {
                KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
                g.initialize(2048);
                KeyPair kp = g.generateKeyPair();
                X509Certificate cert =
                        EdsDevSelfSignedCertificate.create(kp, "CN=Practica EDS Dev, OU=Licensing, O=Course");
                cachedDevMaterials = new EdsSigningMaterials(kp.getPrivate(), kp.getPublic(), cert);
            }
            return cachedDevMaterials;
        }
    }

    private static EdsSigningMaterials loadPkcs12(LicenseProperties.Signing s)
            throws GeneralSecurityException, IOException {
        DefaultResourceLoader rl = new DefaultResourceLoader();
        char[] pwd = s.getKeystorePassword().toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = rl.getResource(s.getKeystoreLocation()).getInputStream()) {
            ks.load(is, pwd);
        }
        PrivateKey privateKey = (PrivateKey) ks.getKey(s.getKeyAlias(), pwd);
        PublicKey publicKey = ks.getCertificate(s.getKeyAlias()).getPublicKey();
        X509Certificate cert = (X509Certificate) ks.getCertificate(s.getKeyAlias());
        return new EdsSigningMaterials(privateKey, publicKey, cert);
    }

    private static boolean hasPkcs12(LicenseProperties.Signing s) {
        return StringUtils.hasText(s.getKeystoreLocation()) && StringUtils.hasText(s.getKeystorePassword());
    }
}
