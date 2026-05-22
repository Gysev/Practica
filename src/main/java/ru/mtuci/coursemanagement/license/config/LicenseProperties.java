package ru.mtuci.coursemanagement.license.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Длительность жизни тикета верификации и загрузка ключа RSA для ЭЦП.
 */
@ConfigurationProperties(prefix = "license")
@Validated
public class LicenseProperties {

    /** Сколько секунд считает тикет валидным на стороне клиента после выдачи. */
    private Duration ticketLifetime = Duration.ofMinutes(5);

    /** Настройки подписания ответов. */
    private Signing signing = new Signing();

    public Duration getTicketLifetime() {
        return ticketLifetime;
    }

    public void setTicketLifetime(Duration ticketLifetime) {
        this.ticketLifetime = ticketLifetime;
    }

    public Signing getSigning() {
        return signing;
    }

    public void setSigning(Signing signing) {
        this.signing = signing;
    }

    public static class Signing {

        /**
         * true — при старте генерируется пара RSA (только dev/тест). В проде задайте keystore ниже.
         */
        private boolean generateDevKey = true;

        private String keystoreLocation;
        private String keystorePassword;
        private String keyAlias = "license";

        public boolean isGenerateDevKey() {
            return generateDevKey;
        }

        public void setGenerateDevKey(boolean generateDevKey) {
            this.generateDevKey = generateDevKey;
        }

        public String getKeystoreLocation() {
            return keystoreLocation;
        }

        public void setKeystoreLocation(String keystoreLocation) {
            this.keystoreLocation = keystoreLocation;
        }

        public String getKeystorePassword() {
            return keystorePassword;
        }

        public void setKeystorePassword(String keystorePassword) {
            this.keystorePassword = keystorePassword;
        }

        public String getKeyAlias() {
            return keyAlias;
        }

        public void setKeyAlias(String keyAlias) {
            this.keyAlias = keyAlias;
        }
    }
}
