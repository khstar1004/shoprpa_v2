package com.iflytek.rpa.auth.blacklist.task;

import com.iflytek.rpa.auth.blacklist.service.BlackListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 사용자 차단 목록 예약 작업.
 *
 * 설명:
 * 1. 요청 시 isBlocked()에서 만료 상태를 확인하고 갱신합니다.
 * 2. 예약 작업은 매일 만료된 차단 기록을 정리합니다.
 * 3. 데이터베이스 status 필드를 1(차단 중)에서 0(해제됨)으로 갱신하고 Redis 캐시를 제거합니다.
 *
 * @author system
 * @date 2025-12-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "blacklist.task", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BlacklistScheduledTask {

    private final BlackListService blackListService;

    /**
     * 만료된 사용자 차단을 매일 새벽 2시에 해제합니다.
     */
    @Scheduled(cron = "${blacklist.task.unban-cron:0 0 2 * * ?}")
    public void unbanExpiredUsers() {
        log.info("만료된 사용자 차단 해제 예약 작업을 시작합니다.");

        try {
            int count = blackListService.batchUnbanExpired();
            log.info("만료된 사용자 차단 해제 예약 작업 완료, count: {}", count);
        } catch (Exception e) {
            log.error("만료된 사용자 차단 해제 예약 작업 실패", e);
        }
    }

    /**
     * 차단 목록 보관 데이터 정리 예약 작업입니다.
     * 현재는 해제 처리를 batchUnbanExpired()가 담당하므로 별도 삭제는 수행하지 않습니다.
     */
    @Scheduled(cron = "${blacklist.task.cleanup-cron:0 0 2 * * ?}")
    public void cleanupExpiredData() {
        log.info("차단 목록 보관 데이터 정리 예약 작업을 시작합니다.");

        try {
            log.info("차단 목록 보관 데이터 정리 예약 작업 완료");
        } catch (Exception e) {
            log.error("차단 목록 보관 데이터 정리 예약 작업 실패", e);
        }
    }
}
