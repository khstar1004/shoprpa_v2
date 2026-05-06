package com.iflytek.rpa.auth.blacklist.service.impl;

import com.iflytek.rpa.auth.blacklist.config.BlacklistConfig;
import com.iflytek.rpa.auth.blacklist.exception.ShouldBeBlackException;
import com.iflytek.rpa.auth.blacklist.service.PasswordErrorService;
import com.iflytek.rpa.auth.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 비밀번호오류계획데이터서비스
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

        // 증가오류계획데이터
        long count = RedisUtils.incr(key, 1);

        // 경과시간()
        if (count == 1) {
            RedisUtils.expire(key, blacklistConfig.getPasswordErrorExpire());
        }

        log.warn(
                "사용자비밀번호오류, userId: {}, username: {}, 현재오류데이터: {}/{}",
                userId,
                username,
                count,
                blacklistConfig.getPasswordErrorLimit());

        // 조회여부까지값
        if (count >= blacklistConfig.getPasswordErrorLimit()) {
            log.error("사용자비밀번호오류데이터까지값, 트리거, userId: {}, username: {}, 오류데이터: {}", userId, username, count);

            // 지우기계획데이터
            RedisUtils.del(key);

            // 출력예외
            throw new ShouldBeBlackException(
                    userId,
                    username,
                    "비밀번호오류데이터경과다중(" + blacklistConfig.getPasswordErrorLimit() + ")",
                    ShouldBeBlackException.BlackType.PASSWORD_ERROR);
        }

        return (int) count;
    }

    @Override
    public void clearPasswordError(String userId) {
        String key = BlacklistConfig.getPasswordErrorKey(userId);
        RedisUtils.del(key);
        log.debug("지우기비밀번호오류 기록, userId: {}", userId);
    }

    @Override
    public int getPasswordErrorCount(String userId) {
        String key = BlacklistConfig.getPasswordErrorKey(userId);
        Object count = RedisUtils.get(key);
        return count != null ? Integer.parseInt(count.toString()) : 0;
    }
}