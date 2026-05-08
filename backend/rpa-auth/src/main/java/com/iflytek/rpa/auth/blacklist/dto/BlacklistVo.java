package com.iflytek.rpa.auth.blacklist.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 차단 목록 응답 객체
 *
 * @author system
 * @date 2025-12-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 기록ID
     */
    private Long id;

    /**
     * 사용자ID
     */
    private String userId;

    /**
     * 사용자명
     */
    private String username;

    /**
     * 차단 사유
     */
    private String banReason;

    /**
     * 차단 단계
     */
    private Integer banLevel;

    /**
     * 차단 횟수
     */
    private Integer banCount;

    /**
     * 차단 기간(초)
     */
    private Long banDuration;

    /**
     * 차단 기간 설명
     */
    private String banDurationDesc;

    /**
     * 시작 시간
     */
    private LocalDateTime startTime;

    /**
     * 종료 시간
     */
    private LocalDateTime endTime;

    /**
     * 남은 시간(초)
     */
    private Long remainingSeconds;

    /**
     * 남은 시간 설명
     */
    private String remainingTimeDesc;

    /**
     * 상태(1:차단 중, 0:해제됨)
     */
    private Integer status;

    /**
     * 상태 설명
     */
    private String statusDesc;

    /**
     * 처리자
     */
    private String operator;

    /**
     * 생성 시간
     */
    private LocalDateTime createTime;
}
