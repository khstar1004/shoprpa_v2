package com.iflytek.rpa.auth.blacklist.task;

import com.iflytek.rpa.auth.blacklist.service.BlackListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 이름단일예약작업
 * 해제완료경과의사용자(기기제어)
 *
 * 설명: 
 * 1. 필요사용로드기기제어: 에서조회시(isBlocked)조회업데이트경과상태
 * 2. 예약작업로: 매일실행일, 관리가능의경과기록
 * 3. 사용: 
 *    - 업데이트데이터베이스 status 필드: 1(중)-> 0(완료해제)
 *    - 관리 Redis 중가능의저장
 *    - 보관데이터베이스및 Redis 의데이터일
 *    - 내용오류: 관리길이시간미완료방문의사용자기록
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
     * 예약해제완료경과의사용자
     * 매일 2 실행일
     *
     * 비고: 필요로드기기제어, 예약작업로
     * 로드에서 isBlocked() 방법법중, 조회시조회업데이트경과상태
     */
    @Scheduled(cron = "${blacklist.task.unban-cron:0 0 2 * * ?}")
    public void unbanExpiredUsers() {
        log.info("열기 실행예약해제작업");

        try {
            int count = blackListService.batchUnbanExpired();
            log.info("예약해제작업완료, 공유해제 {} 개사용자", count);
        } catch (Exception e) {
            log.error("예약해제작업실행실패", e);
        }
    }

    /**
     * 예약관리경과데이터
     * 매일 2 실행
     * 가능선택: 삭제초과경과 90 의완료해제기록
     */
    @Scheduled(cron = "${blacklist.task.cleanup-cron:0 0 2 * * ?}")
    public void cleanupExpiredData() {
        log.info("열기 실행이름단일데이터관리작업");

        try {
            // TODO: 관리(가능선택)
            // 예: 삭제 90 전의완료해제기록
            log.info("이름단일데이터관리작업완료");
        } catch (Exception e) {
            log.error("이름단일데이터관리작업실행실패", e);
        }
    }
}