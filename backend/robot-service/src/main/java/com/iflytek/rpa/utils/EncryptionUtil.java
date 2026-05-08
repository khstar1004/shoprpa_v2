package com.iflytek.rpa.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtil {
    private static final String ALGORITHM = "AES";

    /**
     * 암호화문자열
     * @param plaintext 문서
     * @return 코드후의비밀문서
     */
    public static String encrypt(String plaintext, String AES_KEY) throws Exception {
        if (plaintext == null) {
            return null;
        }
        SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }
    /**
     * 복호화문자열
     * @param ciphertext Base64코드의비밀문서
     * @return 문서
     * @throws Exception
     */
    public static String decrypt(String ciphertext, String AES_KEY) throws Exception {
        if (ciphertext == null) {
            return null;
        }
        SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decoded = Base64.getDecoder().decode(ciphertext);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
