package org.enterpriseauditing.enterpriseauditing.util;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

public final class DigitalSignatureUtil {

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private DigitalSignatureUtil() {
        // Utility class
    }

    /**
     * Creates a digital signature using the private key.
     */
    public static String sign(String data, PrivateKey privateKey) {

        try {
            Signature signature =
                    Signature.getInstance(SIGNATURE_ALGORITHM);

            signature.initSign(privateKey);

            signature.update(
                    data.getBytes(StandardCharsets.UTF_8)
            );

            byte[] signedBytes =
                    signature.sign();

            return Base64.getEncoder()
                    .encodeToString(signedBytes);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to create digital signature",
                    e
            );
        }
    }

    /**
     * Verifies a digital signature using the public key.
     */
    public static boolean verify(
            String data,
            String digitalSignature,
            PublicKey publicKey) {

        try {
            Signature signature =
                    Signature.getInstance(SIGNATURE_ALGORITHM);

            signature.initVerify(publicKey);

            signature.update(
                    data.getBytes(StandardCharsets.UTF_8)
            );

            byte[] signatureBytes =
                    Base64.getDecoder()
                            .decode(digitalSignature);

            return signature.verify(signatureBytes);

        } catch (Exception e) {
            return false;
        }
    }
}