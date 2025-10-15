package com.minishop.paymentservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SignatureVerifierTest {

    private SignatureVerifier signatureVerifier;
    private final String secretKey = "RAOANNK01UMOU64R04J1Q17TGWY2QNWG";

    @BeforeEach
    void setUp() {
        signatureVerifier = new SignatureVerifier();
    }

    @Test
    void testHmacSHA512GeneratesConsistentHex() {
        String data = "vnp_Amount=1000000&vnp_Command=pay&vnp_TmnCode=DEMOTMN1";
        String hash1 = signatureVerifier.hmacSHA512(secretKey, data);
        String hash2 = signatureVerifier.hmacSHA512(secretKey, data);

        assertNotNull(hash1);
        assertEquals(128, hash1.length(), "HMAC-SHA512 hex output must be 128 characters");
        assertEquals(hash1, hash2);
    }

    @Test
    void testVerifyVnPaySignatureValid() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "1000000");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", "DEMOTMN1");
        params.put("vnp_TxnRef", "TXN20260814001");

        String expectedHash = signatureVerifier.hashAllFields(params, secretKey);
        params.put("vnp_SecureHash", expectedHash);

        boolean isValid = signatureVerifier.verifyVnPaySignature(params, secretKey);
        assertTrue(isValid, "Signature must be valid when matching expected hash");
    }

    @Test
    void testVerifyVnPaySignatureTamperedFails() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "1000000");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", "DEMOTMN1");
        params.put("vnp_TxnRef", "TXN20260814001");

        String expectedHash = signatureVerifier.hashAllFields(params, secretKey);
        params.put("vnp_SecureHash", expectedHash);

        // Tamper with parameter (e.g. change amount)
        params.put("vnp_Amount", "2000000");

        boolean isValid = signatureVerifier.verifyVnPaySignature(params, secretKey);
        assertFalse(isValid, "Signature must fail when parameter was tampered");
    }

    @Test
    void testVerifyVnPaySignatureMissingHashFails() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "1000000");

        boolean isValid = signatureVerifier.verifyVnPaySignature(params, secretKey);
        assertFalse(isValid, "Must return false when vnp_SecureHash is missing");
    }
}
