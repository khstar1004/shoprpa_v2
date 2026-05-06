package com.iflytek.rpa.auth.blacklist.exception;

import lombok.Getter;

/**
 * 입력이름단일예외
 * 서비스코드출력예외후, 전체영역예외관리기기를사용자추가까지이름단일
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
     * 원인
     */
    private final String reason;

    /**
     * 유형
     */
    private final BlackType blackType;

    /**
     * 데이터
     *
     * @param userId 사용자ID
     * @param username 사용자명
     * @param reason 원인
     * @param blackType 유형
     */
    public ShouldBeBlackException(String userId, String username, String reason, BlackType blackType) {
        super("사용자 " + username + "(" + userId + ") 트리거이면: " + reason);
        this.userId = userId;
        this.username = username;
        this.reason = reason;
        this.blackType = blackType;
    }

    /**
     * 데이터(원인예외)
     *
     * @param userId 사용자ID
     * @param username 사용자명
     * @param reason 원인
     * @param blackType 유형
     * @param cause 원인예외
     */
    public ShouldBeBlackException(String userId, String username, String reason, BlackType blackType, Throwable cause) {
        super("사용자 " + username + "(" + userId + ") 트리거이면: " + reason, cause);
        this.userId = userId;
        this.username = username;
        this.reason = reason;
        this.blackType = blackType;
    }

    /**
     * 유형
     */
    @Getter
    public enum BlackType {
        /**
         * 비밀번호오류데이터경과다중
         */
        PASSWORD_ERROR("비밀번호오류데이터경과다중", 1),

        /**
         * 데이터방문
         */
        SENSITIVE_ACCESS("법방문데이터", 2),

        /**
         * 
         */
        VIOLATION("", 3),

        /**
         * 
         */
        MANUAL("관리관리요소", 0);

        private final String description;
        private final int triggerCount;

        BlackType(String description, int triggerCount) {
            this.description = description;
            this.triggerCount = triggerCount;
        }
    }
}