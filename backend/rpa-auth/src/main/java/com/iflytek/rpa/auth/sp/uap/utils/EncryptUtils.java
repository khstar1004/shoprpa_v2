package com.iflytek.rpa.auth.sp.uap.utils;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 권한 부여복호화도구유형
 * 사용복호화테넌트까지시간
 */
@Slf4j
@Component
public class EncryptUtils {

    /**
     * RSA암호화법
     */
    private static final String ALGORITHM = "RSA";

    /**
     * RSA키(Base64형식)
     * 사용복호화상업의권한 부여정보
     * 키의키상업있음, 사용완료권한 부여
     */
    private static final String PUBLIC_KEY_BASE64 =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoZpsnI0MN53n0rcJNOcPdYOh6jDwcH2EmdfF07Js9Zt0S6ZoX2C5lXJcHg+MDjthVZRKPecMm2vEeBh/1ah/lA+CC/zNW3V0nz0vKuQzS5XXM2bMSYyHqU6UwqIXvQxhuSPrOOUuvE4KyPO/ZsZrU9tkaCdygfu20hOzEBKtrbRoEz8Bwhn2bCdBjG0SjFZyumNj7UQ9G+K69urY+lH4lLaY7nwDUboiDjKIbgGTpIYJGzeIsivtlMQVSIBvvSky8GVBDOxqtJ7Q4CH+lzICClyNK3QsOk2y214RCom5AM34iv6VvaWmAQc4Ciy+n4vMFjha9KWLjn582BjVLxm2qQIDAQAB";

    /**
     * RSA키객체
     */
    private static PublicKey publicKey;

    /**
     * RSA키
     * 에서Base64코드의키문자열로드키객체
     */
    static {
        try {
            // 에서Base64문자열로드키
            byte[] publicKeyBytes = Base64.getDecoder().decode(PUBLIC_KEY_BASE64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            publicKey = keyFactory.generatePublic(keySpec);

            log.info("RSA키완료");
        } catch (Exception e) {
            log.error("RSA키실패", e);
            throw new RuntimeException("RSA키실패", e);
        }
    }

    /**
     * 사용키복호화권한 부여정보
     * 비고: 사용키복호화, 예원인로상업사용키암호화
     * 예RSA의일사용법, 사용인증권한 부여정보의
     *
     * @param encryptedText 암호화후의Base64문자열(상업사용키암호화)
     * @return 복호화후의문서(까지시간, 형식: yyyy-MM-dd)
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            throw new RuntimeException("인증 정보 복호화 실패: 입력비워 둘 수 없습니다");
        }

        long startNanos = System.nanoTime();
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, publicKey);

            // 분복호화(지원길이텍스트)
            String[] encryptedBlocks = encryptedText.split("\\|");
            StringBuilder decryptedText = new StringBuilder();

            for (String encryptedBlock : encryptedBlocks) {
                byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBlock);
                byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
                decryptedText.append(new String(decryptedBytes, StandardCharsets.UTF_8));
            }

            return decryptedText.toString();
        } catch (Exception e) {
            log.error("인증 정보 복호화 실패, 비밀문서: {}", encryptedText, e);
            throw new RuntimeException("인증 정보 복호화 실패: " + e.getMessage(), e);
        } finally {
            long costMicros = (System.nanoTime() - startNanos) / 1_000;
            log.info("권한 부여정보복호화시 {} 초", costMicros);
        }
    }
}