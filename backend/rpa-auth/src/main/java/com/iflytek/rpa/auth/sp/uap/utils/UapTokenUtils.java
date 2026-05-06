package com.iflytek.rpa.auth.sp.uap.utils;

import com.iflytek.rpa.auth.conf.condition.ConditionalOnSaaSOrUAP;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * UAP Token 도구유형
 * 사용및UAP내용의JDK순서열방식저장및가져오기token
 *
 * @author lihang
 * @date 2025-11-30
 */
@Slf4j
@Component
@ConditionalOnSaaSOrUAP
public class UapTokenUtils {

    /**
     * accessToken 경과시간: 2시간(7200초)
     */
    public static final long ACCESS_TOKEN_EXPIRE_SECONDS = 7200L;

    /**
     * refreshToken 경과시간: 7(604800초)
     */
    public static final long REFRESH_TOKEN_EXPIRE_SECONDS = 604800L;

    /**
     * refreshToken 값: 1(86400초)
     *  refreshToken  TTL 소값시, 행
     */
    public static final long REFRESH_TOKEN_RENEWAL_THRESHOLD_SECONDS = 86400L;

    private static RedisTemplate<String, Object> uapRedisTemplate;

    /**
     * 비고입력UAP내용의RedisTemplate
     */
    @Autowired
    @Qualifier("uapCompatibleRedisTemplate")
    public void setUapRedisTemplate(RedisTemplate<String, Object> template) {
        UapTokenUtils.uapRedisTemplate = template;
    }

    /**
     * 저장 accessToken 까지 Redis
     *
     * @param sessionId session ID
     * @param accessToken 방문브랜드
     * @param expireSeconds 경과시간(초)
     */
    public static void saveAccessToken(String sessionId, String accessToken, long expireSeconds) {
        try {
            String key = "uap:user:access:token:" + sessionId;
            uapRedisTemplate.opsForValue().set(key, accessToken, expireSeconds, TimeUnit.SECONDS);
            log.info("완료저장 accessToken 까지 Redis, key: {}, 경과시간: {}초", key, expireSeconds);
        } catch (Exception e) {
            log.error("저장 accessToken 실패", e);
        }
    }

    /**
     * 저장 refreshToken 까지 Redis
     *
     * @param sessionId session ID
     * @param refreshToken 새로고침브랜드
     * @param expireSeconds 경과시간(초)
     */
    public static void saveRefreshToken(String sessionId, String refreshToken, long expireSeconds) {
        try {
            String key = "uap:user:refresh:token:" + sessionId;
            uapRedisTemplate.opsForValue().set(key, refreshToken, expireSeconds, TimeUnit.SECONDS);
            log.info("완료저장 refreshToken 까지 Redis, key: {}, 경과시간: {}초", key, expireSeconds);
        } catch (Exception e) {
            log.error("저장 refreshToken 실패", e);
        }
    }

    /**
     * 가져오기 accessToken
     *
     * @param sessionId session ID
     * @return accessToken
     */
    public static String getAccessToken(String sessionId) {
        try {
            String key = "uap:user:access:token:" + sessionId;
            Object value = uapRedisTemplate.opsForValue().get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.error("가져오기 accessToken 실패", e);
            return null;
        }
    }

    /**
     * 가져오기 refreshToken
     *
     * @param sessionId session ID
     * @return refreshToken
     */
    public static String getRefreshToken(String sessionId) {
        try {
            String key = "uap:user:refresh:token:" + sessionId;
            Object value = uapRedisTemplate.opsForValue().get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.error("가져오기 refreshToken 실패", e);
            return null;
        }
    }

    /**
     * 가져오기 refreshToken 의경과시간(TTL)
     *
     * @param sessionId session ID
     * @return 경과시간(초), -1 테이블 key 찾을 수 없습니다, -2 테이블 key 저장된 있음경과시간
     */
    public static long getRefreshTokenTTL(String sessionId) {
        try {
            String key = "uap:user:refresh:token:" + sessionId;
            Long ttl = uapRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (ttl == null) {
                return -1; // key 찾을 수 없습니다
            }
            return ttl;
        } catch (Exception e) {
            log.error("가져오기 refreshToken TTL 실패", e);
            return -1;
        }
    }

    /**
     *  refreshToken(지연길이 TTL, 아니요 value)
     * 에서 refreshToken 저장된  TTL 소값시행
     *
     * @param sessionId session ID
     * @return 여부완료
     */
    public static boolean renewRefreshToken(String sessionId) {
        try {
            String key = "uap:user:refresh:token:" + sessionId;

            // 조회 key 여부저장에서
            if (!Boolean.TRUE.equals(uapRedisTemplate.hasKey(key))) {
                log.debug("refreshToken 찾을 수 없습니다, 필요하지 않습니다, sessionId: {}", sessionId);
                return false;
            }

            // 가져오기현재 TTL
            Long currentTTL = uapRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (currentTTL == null || currentTTL < 0) {
                log.debug("refreshToken TTL 없음, 불가, sessionId: {}, TTL: {}", sessionId, currentTTL);
                return false;
            }

            // 에서 TTL 소값시
            if (currentTTL < REFRESH_TOKEN_RENEWAL_THRESHOLD_SECONDS) {
                uapRedisTemplate.expire(key, REFRESH_TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
                log.info(
                        "완료 refreshToken, sessionId: {}, 기존시간: {}초, 후: {}초",
                        sessionId,
                        currentTTL,
                        REFRESH_TOKEN_EXPIRE_SECONDS);
                return true;
            } else {
                log.debug("refreshToken 시간, 필요하지 않습니다, sessionId: {}, 시간: {}초", sessionId, currentTTL);
                return false;
            }
        } catch (Exception e) {
            log.error(" refreshToken 실패, sessionId: {}", sessionId, e);
            return false;
        }
    }

    /**
     * 삭제사용자의모든 token
     *
     * @param sessionId session ID
     */
    public static void deleteTokens(String sessionId) {
        try {
            String accessTokenKey = "uap:user:access:token:" + sessionId;
            String refreshTokenKey = "uap:user:refresh:token:" + sessionId;
            uapRedisTemplate.delete(accessTokenKey);
            uapRedisTemplate.delete(refreshTokenKey);
            log.info("삭제됨 sessionId: {} 의모든 token", sessionId);
        } catch (Exception e) {
            log.error("삭제 token 실패", e);
        }
    }
}