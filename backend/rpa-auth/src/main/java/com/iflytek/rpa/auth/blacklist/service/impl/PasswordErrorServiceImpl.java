package com.iflytek.rpa.auth.blacklist.service.impl;

import com.iflytek.rpa.auth.blacklist.config.BlacklistConfig;
import com.iflytek.rpa.auth.blacklist.exception.ShouldBeBlackException;
import com.iflytek.rpa.auth.blacklist.service.PasswordErrorService;
import com.iflytek.rpa.auth.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 비밀번호 오류 횟수 관리 서비스
 *
 * @author system
 * @date 2025-12-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordErrorServiceImpl implements PasswordErrorService {

    private final BlacklistConfig blacklistConfig;

    @Override
    public int recordPasswordError(String userId, String username) {
        String key = BlacklistConfig.getPasswordErrorKey(userId);

        long count = RedisUtils.incr(key, 1);

        if (count == 1) {
            RedisUtils.expire(key, blacklistConfig.getPasswordErrorExpire());
        }

        log.warn(
                "사용자 비밀번호 오류 기록, userId: {}, username: {}, count: {}/{}",
                userId,
                username,
                count,
                blacklistConfig.getPasswordErrorLimit());

        if (count >= blacklistConfig.getPasswordErrorLimit()) {
            log.error("비밀번호 오류 허용 횟수를 초과해 사용자 차단을 트리거합니다. userId: {}, username: {}, count: {}", userId, username, count);

            RedisUtils.del(key);

            throw new ShouldBeBlackException(
                    userId,
                    username,
                    "비밀번호 오류 허용 횟수 초과(" + blacklistConfig.getPasswordErrorLimit() + "회)",
                    ShouldBeBlackException.BlackType.PASSWORD_ERROR);
        }

        return (int) count;
    }

    @Override
    public void clearPasswordError(String userId) {
        String key = BlacklistConfig.getPasswordErrorKey(userId);
        RedisUtils.del(key);
        log.debug("비밀번호 오류 기록 삭제, userId: {}", userId);
    }

    @Override
    public int getPasswordErrorCount(String userId) {
        String key = BlacklistConfig.getPasswordErrorKey(userId);
        Object count = RedisUtils.get(key);
        return count != null ? Integer.parseInt(count.toString()) : 0;
    }
}
