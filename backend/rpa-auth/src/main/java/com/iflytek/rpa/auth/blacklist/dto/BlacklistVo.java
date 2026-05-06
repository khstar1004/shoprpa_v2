package com.iflytek.rpa.auth.blacklist.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이름단일이미지객체
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
     * 원인
     */
    private String banReason;

    /**
     * 대기단계
     */
    private Integer banLevel;

    /**
     * 데이터
     */
    private Integer banCount;

    /**
     * 시길이(초)
     */
    private Long banDuration;

    /**
     * 시길이설명
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
     * 시간(초)
     */
    private Long remainingSeconds;

    /**
     * 시간설명
     */
    private String remainingTimeDesc;

    /**
     * 상태(1:중, 0:완료해제)
     */
    private Integer status;

    /**
     * 상태설명
     */
    private String statusDesc;

    /**
     * 사람
     */
    private String operator;

    /**
     * 생성 시간
     */
    private LocalDateTime createTime;
}