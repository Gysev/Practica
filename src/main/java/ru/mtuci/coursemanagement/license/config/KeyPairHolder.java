package ru.mtuci.coursemanagement.license.config;

import java.security.PrivateKey;
import java.security.PublicKey;

public record KeyPairHolder(PublicKey publicKey, PrivateKey privateKey) {
}
