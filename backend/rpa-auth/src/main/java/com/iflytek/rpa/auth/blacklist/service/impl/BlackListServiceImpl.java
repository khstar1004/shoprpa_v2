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
 * 이름단일서비스유형
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

    /**
     * 분방식전
     */
    private static final String LOCK_PREFIX = "LOCK:BL:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserBlacklist add(String userId, String username, String reason, String operator) {
        log.info("열기 추가이름단일, userId: {}, username: {}, reason: {}, operator: {}", userId, username, reason, operator);

        // 사용분방식, 발송제목
        String lockKey = LOCK_PREFIX + userId;
        try {
            boolean lockSuccess = tryLock(lockKey, 10);
            if (!lockSuccess) {
                log.warn("가져오기분방식실패, userId: {}", userId);
                throw new RuntimeException("시스템, 요청 후시도");
            }

            // 조회현재여부있음의기록
            UserBlacklist existingBlacklist = userBlacklistDao.findActiveBlacklist(userId);

            UserBlacklist blacklist;
            if (existingBlacklist != null) {
                // 완료있음기록, 업그레이드대기단계
                log.info(
                        "사용자완료에서이름단일중, 업그레이드대기단계, 현재대기단계: {}, 데이터: {}",
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

                log.info("이름단일업그레이드성공, 새대기단계: {}, 새데이터: {}, : {}", newLevel, newCount, blacklist.getEndTime());
            } else {
                // 추가기록
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

                log.info("추가이름단일성공, : {}", blacklist.getEndTime());
            }

            // 입력 Redis 저장
            cacheBlacklist(blacklist);

            return blacklist;
        } finally {
            // 
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
            // 에서 Redis 조회
            // 결과가 Redis 중저장에서 key, 설명사용자지정에서중(TTL 예근거 endTime 의)
            Object cached = RedisUtils.get(key);
            if (cached != null) {
                // Redis 중저장에서, 설명사용자에서이름단일중, 직선연결반환
                BlacklistCacheDto dto = objectMapper.convertValue(cached, BlacklistCacheDto.class);
                log.debug("에서 Redis 조회까지이름단일정보, userId: {}", userId);
                return dto;
            }

            // Redis 중찾을 수 없습니다, 조회데이터베이스
            //            UserBlacklist blacklist = userBlacklistDao.findActiveBlacklist(userId);
            UserBlacklist blacklist = null;
            if (blacklist != null) {
                LocalDateTime now = LocalDateTime.now();
                if (blacklist.getEndTime().isAfter(now)) {
                    // 데이터베이스중저장된 미완료경과, 돌아가기 Redis
                    log.info("에서데이터베이스조회까지이름단일정보, 돌아가기 Redis, userId: {}", userId);
                    cacheBlacklist(blacklist);
                    return buildCacheDto(blacklist);
                } else {
                    // 완료경과, 로드해제(업데이트데이터베이스상태)
                    log.info("이름단일완료경과, 로드해제, userId: {}", userId);
                    unbanIfExpired(userId);
                }
            }

            return null;
        } catch (Exception e) {
            log.error("조회이름단일실패, userId: {}", userId, e);
            // 예외시반환 null, 아니요조회데이터베이스, 가능
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbanIfExpired(String userId) {
        UserBlacklist blacklist = userBlacklistDao.findActiveBlacklist(userId);
        if (blacklist != null && blacklist.getEndTime().isBefore(LocalDateTime.now())) {
            userBlacklistDao.unban(blacklist.getId());
            String key = BlacklistConfig.getBlacklistKey(userId);
            RedisUtils.del(key);
            log.info("해제사용자, userId: {}", userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unban(String userId, String operator) {
        log.info("해제사용자, userId: {}, operator: {}", userId, operator);

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
            log.info("해제완료, userId: {}", userId);
            return true;
        }

        log.warn("찾을 수 없는 의이름단일기록, userId: {}", userId);
        return false;
    }

    @Override
    public List<UserBlacklist> getHistory(String userId) {
        return userBlacklistDao.findHistoryByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUnbanExpired() {
        log.info("열기 량해제완료경과사용자");

        // 매관리 100 
        List<UserBlacklist> expiredList = userBlacklistDao.findExpiredBlacklist(LocalDateTime.now(), 100);

        int count = 0;
        for (UserBlacklist blacklist : expiredList) {
            try {
                userBlacklistDao.unban(blacklist.getId());
                String key = BlacklistConfig.getBlacklistKey(blacklist.getUserId());
                RedisUtils.del(key);
                count++;
                log.info("해제경과사용자, userId: {}, username: {}", blacklist.getUserId(), blacklist.getUsername());
            } catch (Exception e) {
                log.error("해제사용자실패, userId: {}", blacklist.getUserId(), e);
            }
        }

        log.info("량해제완료, 공유해제 {} 개사용자", count);
        return count;
    }

    @Override
    public void forceLogout(HttpServletRequest request, HttpServletResponse response) {
        log.info("강함제어비고판매사용자");
        try {
            // 호출 UAP API 비고판매, 에서 request 중지우기정보
            com.iflytek.sec.uap.client.api.UapUserInfoAPI.logout(request, response);
            log.info("사용자완료비고판매");
        } catch (Exception e) {
            log.error("비고판매사용자실패", e);
        }
    }

    /**
     * 저장이름단일까지 Redis
     */
    private void cacheBlacklist(UserBlacklist blacklist) {
        try {
            String key = BlacklistConfig.getBlacklistKey(blacklist.getUserId());
            BlacklistCacheDto dto = buildCacheDto(blacklist);

            // 계획시간(초)
            long ttl = dto.getRemainingSeconds();
            if (ttl > 0) {
                RedisUtils.set(key, dto, ttl);
                log.debug("이름단일완료저장까지 Redis, userId: {}, ttl: {}초", blacklist.getUserId(), ttl);
            }
        } catch (Exception e) {
            log.error("저장이름단일까지 Redis 실패, userId: {}", blacklist.getUserId(), e);
        }
    }

    /**
     * 생성저장 DTO
     */
    private BlacklistCacheDto buildCacheDto(UserBlacklist blacklist) {
        long remainingSeconds =
                Duration.between(LocalDateTime.now(), blacklist.getEndTime()).getSeconds();
        if (remainingSeconds < 0) {
            remainingSeconds = 0;
        }

        // 를 LocalDateTime 변환로시간(초), 순서열제목
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

    /**
     * 시도가져오기분방식(단일)
     */
    private boolean tryLock(String key, long expireSeconds) {
        try {
            return RedisUtils.redisTemplate.opsForValue().setIfAbsent(key, "locked", Duration.ofSeconds(expireSeconds));
        } catch (Exception e) {
            log.error("가져오기분방식실패", e);
            return false;
        }
    }

    /**
     * 분방식
     */
    private void unlock(String key) {
        try {
            RedisUtils.del(key);
        } catch (Exception e) {
            log.error("분방식실패", e);
        }
    }
}