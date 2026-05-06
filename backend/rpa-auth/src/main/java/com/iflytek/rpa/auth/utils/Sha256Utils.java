package com.iflytek.rpa.auth.utils;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * SHA256 도구유형
 * @author lihang
 * @date 2025-11-25
 */
public class Sha256Utils {

    /**
     * SHA256 HMAC 암호화
     * @param secret 키
     * @param data 대기암호화데이터
     * @return 암호화후의십육제어문자열
     */
    public static String sha256Hmac(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA256 HMAC 암호화실패", e);
        }
    }

    /**
     * 문자배열변환십육제어문자열
     * @param bytes 문자배열
     * @return 십육제어문자열
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}