package org.enterpriseauditing.enterpriseauditing.service;

import org.enterpriseauditing.enterpriseauditing.util.DigitalSignatureUtil;
import org.springframework.stereotype.Service;

import java.security.KeyPair;

@Service
public class DigitalSignatureService {

    private final KeyPair keyPair;

    public DigitalSignatureService(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public String sign(String data) {
        return DigitalSignatureUtil.sign(
                data,
                keyPair.getPrivate()
        );
    }

    public boolean verify(
            String data,
            String digitalSignature) {

        return DigitalSignatureUtil.verify(
                data,
                digitalSignature,
                keyPair.getPublic()
        );
    }
}