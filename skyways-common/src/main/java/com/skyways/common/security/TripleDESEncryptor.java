package com.skyways.common.security;

import com.skyways.common.security.SecretManagerService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts PII (passport no, DOB, names, card last-4) using Triple-DES (DESede/CBC/PKCS5Padding).
 * Key is injected from SecretManagerService — never hardcoded.
 * Encoded output format: Base64(iv[8 bytes] + ciphertext)
 */
@Component
@ConditionalOnBean(SecretManagerService.class)
public class TripleDESEncryptor {

    private static final String ALGORITHM = "DESede";
    private static final String TRANSFORMATION = "DESede/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 8;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public TripleDESEncryptor(SecretManagerService secretManagerService) {
        try {
            String rawKey = secretManagerService.getSecret("TRIPLE_DES_KEY");
            byte[] keyBytes = Base64.getDecoder().decode(rawKey);
            DESedeKeySpec keySpec = new DESedeKeySpec(keyBytes);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            this.secretKey = keyFactory.generateSecret(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize 3-DES encryptor", e);
        }
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));

            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public String decrypt(String encoded) {
        if (encoded == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);

            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            return new String(cipher.doFinal(ciphertext), "UTF-8");
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }
}
