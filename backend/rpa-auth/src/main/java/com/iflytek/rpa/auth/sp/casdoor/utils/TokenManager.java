package com.iflytek.rpa.auth.sp.casdoor.utils;

import com.iflytek.rpa.auth.exception.NoLoginException;
import com.iflytek.rpa.auth.utils.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @desc: Casdoor Token관리관리기기, 에서casdoor profile아래
 * @author: weilai <laiwei3@iflytek.com>
 * @create: 2025/12/11 10:29
 */
@Component
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "casdoor", matchIfMissing = true)
public class TokenManager {

    private static final Logger logger = LoggerFactory.getLogger(TokenManager.class);

    private static final String ACCESS_TOKEN_PREFIX = "auth:token:access:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:token:refresh:";

    /**
     * 가져오기사용자의AccessToken(사용서버호출Casdoor API)
     *
     * @param username 사용자명
     * @return AccessToken, 결과가찾을 수 없습니다반환null
     */
    public static String getAccessToken(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        String key = ACCESS_TOKEN_PREFIX + username;
        Object token = RedisUtils.get(key);
        return token != null ? token.toString() : null;
    }

    /**
     * 가져오기사용자의RefreshToken(사용서버새로고침token)
     *
     * @param username 사용자명
     * @return RefreshToken, 결과가찾을 수 없습니다반환null
     */
    public static String getRefreshToken(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        String key = REFRESH_TOKEN_PREFIX + username;
        Object token = RedisUtils.get(key);
        return token != null ? token.toString() : null;
    }

    /**
     * 저장사용자의token까지Redis
     *
     * @param username 사용자명
     * @param accessToken AccessToken
     * @param refreshToken RefreshToken
     * @param expireTime 경과시간(초)
     */
    public static void storeTokens(String username, String accessToken, String refreshToken, long expireTime) {
        if (username == null || username.trim().isEmpty()) {
            logger.warn("사용자명비어 있습니다, 불가저장token");
            return;
        }

        String accessTokenKey = ACCESS_TOKEN_PREFIX + username;
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + username;

        RedisUtils.set(accessTokenKey, accessToken, expireTime);
        RedisUtils.set(refreshTokenKey, refreshToken, expireTime);

        logger.debug("사용자 {} 의서버token완료저장까지Redis", username);
    }

    /**
     * 지우기사용자의token
     *
     * @param username 사용자명
     */
    public static void clearTokens(String username) {
        if (username == null || username.trim().isEmpty()) {
            logger.warn("사용자명비어 있습니다, 불가지우기token");
            return;
        }

        String accessTokenKey = ACCESS_TOKEN_PREFIX + username;
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + username;

        RedisUtils.del(accessTokenKey, refreshTokenKey);

        logger.debug("사용자 {} 의서버token완료에서Redis중지우기", username);
    }

    /**
     * 조회사용자의token여부저장에서
     *
     * @param username 사용자명
     * @return true결과가AccessToken저장에서, false아니요이면
     */
    public static boolean hasToken(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        String accessTokenKey = ACCESS_TOKEN_PREFIX + username;
        return RedisUtils.hasKey(accessTokenKey);
    }

    /**
     * 가져오기현재로그인사용자의AccessToken(방법법, 서비스사용)
     *
     * @return 현재사용자의AccessToken
     * @throws NoLoginException 결과가사용자로그인되지 않았습니다
     */
    public static String getCurrentUserAccessToken() throws NoLoginException {
        //        todo 으로아래코드!!!
        //        User currentUser = UserUtils.nowLoginUser();
        //        String accessToken = getAccessToken(currentUser.name);
        //
        //        if (accessToken == null) {
        //            throw new NoLoginException("사용자AccessToken찾을 수 없습니다, 요청다시 로그인");
        //        }
        //
        //        return accessToken;
        return null;
    }
}