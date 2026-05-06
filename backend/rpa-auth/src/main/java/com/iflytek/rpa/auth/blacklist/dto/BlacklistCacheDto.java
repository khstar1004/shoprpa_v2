package com.iflytek.rpa.auth.blacklist.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이름단일저장 DTO
 * 사용 Redis 저장
 *
 * @author system
 * @date 2025-12-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistCacheDto implements Serializable {

    private static final long serialVersionUID = 1L;

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
    private String reason;

    /**
     * 대기단계
     */
    private Integer level;

    /**
     * 데이터
     */
    private Integer count;

    /**
     * 종료 시간(시간, 초)
     */
    private Long endTimeMillis;

    /**
     * 시간(초)
     */
    private Long remainingSeconds;
}