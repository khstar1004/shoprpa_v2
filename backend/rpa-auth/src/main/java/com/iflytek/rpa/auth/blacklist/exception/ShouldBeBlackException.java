package com.iflytek.rpa.auth.blacklist.exception;

import lombok.Getter;

/**
 * 사용자를 차단 목록에 추가해야 하는 상황을 나타내는 예외입니다.
 *
 * @author system
 * @date 2025-12-16
 */
@Getter
public class ShouldBeBlackException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 사용자ID
     */
    private final String userId;

    /**
     * 사용자명
     */
    private final String username;

    /**
     * 차단 사유
     */
    private final String reason;

    /**
     * 차단 유형
     */
    private final BlackType blackType;

    /**
     * 차단 예외를 생성합니다.
     *
     * @param userId 사용자ID
     * @param username 사용자명
     * @param reason 차단 사유
     * @param blackType 차단 유형
     */
    public ShouldBeBlackException(String userId, String username, String reason, BlackType blackType) {
        super("사용자 " + username + "(" + userId + ") 차단 조건 발생: " + reason);
        this.userId = userId;
        this.username = username;
        this.reason = reason;
        this.blackType = blackType;
    }

    /**
     * 원인 예외가 있는 차단 예외를 생성합니다.
     *
     * @param userId 사용자ID
     * @param username 사용자명
     * @param reason 차단 사유
     * @param blackType 차단 유형
     * @param cause 원인 예외
     */
    public ShouldBeBlackException(String userId, String username, String reason, BlackType blackType, Throwable cause) {
        super("사용자 " + username + "(" + userId + ") 차단 조건 발생: " + reason, cause);
        this.userId = userId;
        this.username = username;
        this.reason = reason;
        this.blackType = blackType;
    }

    /**
     * 차단 트리거 유형
     */
    @Getter
    public enum BlackType {
        /**
         * 비밀번호 오류 초과
         */
        PASSWORD_ERROR("비밀번호 오류 초과", 1),

        /**
         * 민감 데이터 접근
         */
        SENSITIVE_ACCESS("민감 데이터 접근", 2),

        /**
         * 일반 정책 위반
         */
        VIOLATION("정책 위반", 3),

        /**
         * 관리자 수동 차단
         */
        MANUAL("관리자 수동 차단", 0);

        private final String description;
        private final int triggerCount;

        BlackType(String description, int triggerCount) {
            this.description = description;
            this.triggerCount = triggerCount;
        }
    }
}
