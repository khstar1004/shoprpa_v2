package com.iflytek.rpa.auth.blacklist.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.rpa.auth.blacklist.config.BlacklistConfig;
import com.iflytek.rpa.auth.blacklist.dao.UserBlacklistDao;
import com.iflytek.rpa.auth.blacklist.dto.BlacklistCacheDto;
import com.iflytek.rpa.auth.blacklist.entity.UserBlacklist;
import com.iflytek.rpa.auth.blacklist.service.BlackListService;
import com.iflytek.rpa.auth.utils.RedisUtils;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 차단 목록 서비스 구현
 *
 * @author system
 * @date 2025-12-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlackListServiceImpl implements BlackListService {

    private final UserBlacklistDao userBlacklistDao;
    private final BlacklistConfig blacklistConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String LOCK_PREFIX = "LOCK:BL:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserBlacklist add(String userId, String username, String reason, String operator) {
        log.info("사용자 차단 추가 시작, userId: {}, username: {}, reason: {}, operator: {}", userId, username, reason, operator);

        String lockKey = LOCK_PREFIX + userId;
        try {
            boolean lockSuccess = tryLock(lockKey, 10);
            if (!lockSuccess) {
                log.warn("사용자 차단 잠금 획득 실패, userId: {}", userId);
                throw new RuntimeException("요청이 처리 중입니다. 잠시 후 다시 시도하세요.");
            }

            UserBlacklist existingBlacklist = userBlacklistDao.findActiveBlacklist(userId);

            UserBlacklist blacklist;
            if (existingBlacklist != null) {
                log.info(
                        "이미 차단 중인 사용자입니다. 차단 단계를 갱신합니다. currentLevel: {}, count: {}",
                        existingBlacklist.getBanLevel(),
                        existingBlacklist.getBanCount());

                int newLevel = existingBlacklist.getBanLevel() + 1;
                int newCount = existingBlacklist.getBanCount() + 1;
                Long newDuration = blacklistConfig.getDurationByLevel(newLevel);

                existingBlacklist.setBanLevel(newLevel);
                existingBlacklist.setBanCount(newCount);
                existingBlacklist.setBanDuration(newDuration);
                existingBlacklist.setBanReason(reason);
                existingBlacklist.setStartTime(LocalDateTime.now());
                existingBlacklist.setEndTime(LocalDateTime.now().plusSeconds(newDuration));
                existingBlacklist.setOperator(operator);

                userBlacklistDao.updateById(existingBlacklist);
                blacklist = existingBlacklist;

                log.info("사용자 차단 갱신 완료, newLevel: {}, count: {}, endTime: {}", newLevel, newCount, blacklist.getEndTime());
            } else {
                Long duration = blacklistConfig.getDurationByLevel(1);
                blacklist = UserBlacklist.builder()
                        .userId(userId)
                        .username(username)
                        .banReason(reason)
                        .banLevel(1)
                        .banCount(1)
                        .banDuration(duration)
                        .startTime(LocalDateTime.now())
                        .endTime(LocalDateTime.now().plusSeconds(duration))
                        .status(1)
                        .operator(operator)
                        .build();

                userBlacklistDao.insert(blacklist);

                log.info("사용자 차단 추가 완료, endTime: {}", blacklist.getEndTime());
            }

            cacheBlacklist(blacklist);

            return blacklist;
        } finally {
            unlock(lockKey);
        }
    }

    @Override
    public BlacklistCacheDto isBlocked(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }

        String key = BlacklistConfig.getBlacklistKey(userId);

        try {
            Object cached = RedisUtils.get(key);
            if (cached != null) {
                BlacklistCacheDto dto = objectMapper.convertValue(cached, BlacklistCacheDto.class);
                log.debug("Redis에서 사용자 차단 정보를 조회했습니다. userId: {}", userId);
                return dto;
            }

            UserBlacklist blacklist = userBlacklistDao.findActiveBlacklist(userId);
            if (blacklist != null) {
                LocalDateTime now = LocalDateTime.now();
                if (blacklist.getEndTime().isAfter(now)) {
                    log.info("DB에서 사용자 차단 정보를 조회해 Redis에 다시 저장합니다. userId: {}", userId);
                    cacheBlacklist(blacklist);
                    return buildCacheDto(blacklist);
                } else {
                    log.info("차단 기간이 만료되어 사용자 차단을 해제합니다. userId: {}", userId);
                    unbanIfExpired(userId);
                }
            } else {
                unbanIfExpired(userId);
            }

            return null;
        } catch (Exception e) {
            log.error("사용자 차단 정보 조회 실패, userId: {}", userId, e);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbanIfExpired(String userId) {
        UserBlacklist blacklist = userBlacklistDao.findLatestEnabledBlacklist(userId);
        if (blacklist != null && blacklist.getEndTime().isBefore(LocalDateTime.now())) {
            userBlacklistDao.unban(blacklist.getId());
            String key = BlacklistConfig.getBlacklistKey(userId);
            RedisUtils.del(key);
            log.info("만료된 사용자 차단을 해제했습니다. userId: {}", userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unban(String userId, String operator) {
        log.info("사용자 차단 해제 요청, userId: {}, operator: {}", userId, operator);

        LambdaQueryWrapper<UserBlacklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBlacklist::getUserId, userId)
                .eq(UserBlacklist::getStatus, 1)
                .orderByDesc(UserBlacklist::getCreateTime)
                .last("LIMIT 1");

        UserBlacklist blacklist = userBlacklistDao.selectOne(wrapper);
        if (blacklist != null) {
            userBlacklistDao.unban(blacklist.getId());
            String key = BlacklistConfig.getBlacklistKey(userId);
            RedisUtils.del(key);
            log.info("사용자 차단 해제 완료, userId: {}", userId);
            return true;
        }

        log.warn("차단 중인 사용자 기록을 찾을 수 없습니다. userId: {}", userId);
        return false;
    }

    @Override
    public List<UserBlacklist> getHistory(String userId) {
        return userBlacklistDao.findHistoryByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUnbanExpired() {
        log.info("만료된 사용자 차단 일괄 해제를 시작합니다.");

        List<UserBlacklist> expiredList = userBlacklistDao.findExpiredBlacklist(LocalDateTime.now(), 100);

        int count = 0;
        for (UserBlacklist blacklist : expiredList) {
            try {
                userBlacklistDao.unban(blacklist.getId());
                String key = BlacklistConfig.getBlacklistKey(blacklist.getUserId());
                RedisUtils.del(key);
                count++;
                log.info("만료된 사용자 차단 해제 완료, userId: {}, username: {}", blacklist.getUserId(), blacklist.getUsername());
            } catch (Exception e) {
                log.error("사용자 차단 해제 실패, userId: {}", blacklist.getUserId(), e);
            }
        }

        log.info("만료된 사용자 차단 일괄 해제 완료, count: {}", count);
        return count;
    }

    @Override
    public void forceLogout(HttpServletRequest request, HttpServletResponse response) {
        log.info("사용자 세션 강제 로그아웃을 시작합니다.");
        try {
            com.iflytek.sec.uap.client.api.UapUserInfoAPI.logout(request, response);
            log.info("사용자 세션 강제 로그아웃 완료");
        } catch (Exception e) {
            log.error("사용자 세션 강제 로그아웃 실패", e);
        }
    }

    private void cacheBlacklist(UserBlacklist blacklist) {
        try {
            String key = BlacklistConfig.getBlacklistKey(blacklist.getUserId());
            BlacklistCacheDto dto = buildCacheDto(blacklist);

            long ttl = dto.getRemainingSeconds();
            if (ttl > 0) {
                RedisUtils.set(key, dto, ttl);
                log.debug("사용자 차단 정보를 Redis에 저장했습니다. userId: {}, ttl: {}초", blacklist.getUserId(), ttl);
            }
        } catch (Exception e) {
            log.error("사용자 차단 정보 Redis 저장 실패, userId: {}", blacklist.getUserId(), e);
        }
    }

    private BlacklistCacheDto buildCacheDto(UserBlacklist blacklist) {
        long remainingSeconds =
                Duration.between(LocalDateTime.now(), blacklist.getEndTime()).getSeconds();
        if (remainingSeconds < 0) {
            remainingSeconds = 0;
        }

        long endTimeMillis = blacklist
                .getEndTime()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        return BlacklistCacheDto.builder()
                .userId(blacklist.getUserId())
                .username(blacklist.getUsername())
                .reason(blacklist.getBanReason())
                .level(blacklist.getBanLevel())
                .count(blacklist.getBanCount())
                .endTimeMillis(endTimeMillis)
                .remainingSeconds(remainingSeconds)
                .build();
    }

    private boolean tryLock(String key, long expireSeconds) {
        try {
            return RedisUtils.redisTemplate.opsForValue().setIfAbsent(key, "locked", Duration.ofSeconds(expireSeconds));
        } catch (Exception e) {
            log.error("사용자 차단 잠금 획득 실패", e);
            return false;
        }
    }

    private void unlock(String key) {
        try {
            RedisUtils.del(key);
        } catch (Exception e) {
            log.error("사용자 차단 잠금 해제 실패", e);
        }
    }
}
