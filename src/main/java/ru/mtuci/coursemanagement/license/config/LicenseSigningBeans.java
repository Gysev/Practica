package ru.mtuci.coursemanagement.license.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * PKCS#12 (прод), иначе — однаобщая пара RSA 2048 при {@code generate-dev-key=true}.
 */
@Configuration
@EnableConfigurationProperties(LicenseProperties.class)
public class LicenseSigningBeans {

    private static volatile KeyPairHolder cachedDevPair;

    @Bean
    public KeyPairHolder licenseSigningKeypair(LicenseProperties props) throws GeneralSecurityException, IOException {
        LicenseProperties.Signing s = props.getSigning();
        if (hasPkcs12(s)) {
            return loadPkcs12(s);
        }
        if (s.isGenerateDevKey()) {
            return devKeyPairHolder();
        }
        throw new IllegalStateException(
                "Задайте license.signing.keystore-location + keystore-password "
                        + "или включите license.signing.generate-dev-key=true для разработки");
    }

    private static KeyPairHolder devKeyPairHolder() throws GeneralSecurityException {
        KeyPairHolder h = cachedDevPair;
        if (h != null) {
            return h;
        }
        synchronized (LicenseSigningBeans.class) {
            if (cachedDevPair == null) {
                KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
                g.initialize(2048);
                KeyPair kp = g.generateKeyPair();
                cachedDevPair = new KeyPairHolder(kp.getPublic(), kp.getPrivate());
            }
            return cachedDevPair;
        }
    }

    private static KeyPairHolder loadPkcs12(LicenseProperties.Signing s)
            throws GeneralSecurityException, IOException {
        DefaultResourceLoader rl = new DefaultResourceLoader();
        char[] pwd = s.getKeystorePassword().toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = rl.getResource(s.getKeystoreLocation()).getInputStream()) {
            ks.load(is, pwd);
        }
        PrivateKey privateKey = (PrivateKey) ks.getKey(s.getKeyAlias(), pwd);
        PublicKey publicKey = ks.getCertificate(s.getKeyAlias()).getPublicKey();
        return new KeyPairHolder(publicKey, privateKey);
    }

    private static boolean hasPkcs12(LicenseProperties.Signing s) {
        return StringUtils.hasText(s.getKeystoreLocation()) && StringUtils.hasText(s.getKeystorePassword());
    }
}
