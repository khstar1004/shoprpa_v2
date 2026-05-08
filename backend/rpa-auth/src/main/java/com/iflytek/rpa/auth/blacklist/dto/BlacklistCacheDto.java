package com.iflytek.rpa.auth.blacklist.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Redis에 저장하는 사용자 차단 정보 DTO
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
     * 차단 사유
     */
    private String reason;

    /**
     * 차단 단계
     */
    private Integer level;

    /**
     * 차단 횟수
     */
    private Integer count;

    /**
     * 차단 종료 시각(epoch milliseconds)
     */
    private Long endTimeMillis;

    /**
     * 남은 시간(초)
     */
    private Long remainingSeconds;
}
