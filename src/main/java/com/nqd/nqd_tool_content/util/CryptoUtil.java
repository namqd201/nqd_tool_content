package com.nqd.nqd_tool_content.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class CryptoUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // bits

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<Integer, SecretKey> keyring = new HashMap<>();
    private final int activeKeyVersion;

    public CryptoUtil(
            @Value("${security.encryption.active-key-version:1}") int activeKeyVersion,
            @Value("${security.encryption.keys.v1:0123456789abcdef0123456789abcdef}") String keyV1
    ) {
        this.activeKeyVersion = activeKeyVersion;
        // Key v1 (AES-256 requires 32 bytes)
        byte[] keyBytes = ensure32Bytes(keyV1);
        this.keyring.put(1, new SecretKeySpec(keyBytes, "AES"));
    }

    private byte[] ensure32Bytes(String rawKey) {
        byte[] bytes = rawKey.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 32) {
            return bytes;
        }
        byte[] out = new byte[32];
        System.arraycopy(bytes, 0, out, 0, Math.min(bytes.length, 32));
        return out;
    }

    /**
     * Encrypt plaintext with AES-256-GCM.
     * Output format: v{keyVersion}:{base64(iv + ciphertext)}
     *
     * @param plaintext String to encrypt
     * @param aad       Associated Data (e.g. "rowId:columnName")
     * @return Encrypted formatted string
     */
    public String encrypt(String plaintext, String aad) {
        if (plaintext == null) {
            return null;
        }
        try {
            SecretKey key = keyring.get(activeKeyVersion);
            if (key == null) {
                throw new IllegalStateException("Active key version not found: " + activeKeyVersion);
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            if (aad != null && !aad.isBlank()) {
                cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            }

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            String encoded = Base64.getEncoder().encodeToString(byteBuffer.array());
            return "v" + activeKeyVersion + ":" + encoded;
        } catch (Exception e) {
            log.error("Failed to encrypt data", e);
            throw new RuntimeException("Encryption error", e);
        }
    }

    /**
     * Decrypt formatted ciphertext string: v{keyVersion}:{base64(iv + ciphertext)}
     *
     * @param encryptedPayload Formatted ciphertext
     * @param aad              Associated Data (must match AAD used during encryption)
     * @return Decrypted plaintext
     */
    public String decrypt(String encryptedPayload, String aad) {
        if (encryptedPayload == null) {
            return null;
        }
        try {
            int colonIndex = encryptedPayload.indexOf(':');
            if (colonIndex <= 1 || !encryptedPayload.startsWith("v")) {
                throw new IllegalArgumentException("Invalid encrypted payload format: " + encryptedPayload);
            }

            int keyVersion = Integer.parseInt(encryptedPayload.substring(1, colonIndex));
            String base64Content = encryptedPayload.substring(colonIndex + 1);

            SecretKey key = keyring.get(keyVersion);
            if (key == null) {
                throw new IllegalStateException("Key version not found in keyring: " + keyVersion);
            }

            byte[] decoded = Base64.getDecoder().decode(base64Content);
            if (decoded.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Payload too short");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            if (aad != null && !aad.isBlank()) {
                cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            }

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt data", e);
            throw new RuntimeException("Decryption error", e);
        }
    }

    public int getActiveKeyVersion() {
        return activeKeyVersion;
    }
}
