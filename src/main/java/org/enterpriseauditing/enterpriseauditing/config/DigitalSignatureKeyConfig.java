package org.enterpriseauditing.enterpriseauditing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

@Configuration
public class DigitalSignatureKeyConfig {

    @Bean
    public KeyPair auditSigningKeyPair()
            throws NoSuchAlgorithmException {

        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("RSA");

        generator.initialize(2048);

        return generator.generateKeyPair();
    }
}