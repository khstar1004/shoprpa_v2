package com.iflytek.rpa.auth.blacklist.exception;

import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 사용자예외
 *
 * @author system
 * @date 2025-12-16
 */
@Getter
public class UserBlockedException extends RuntimeException {

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
     * 종료 시간
     */
    private final LocalDateTime endTime;

    /**
     * 시간(초)
     */
    private final Long remainingSeconds;

    /**
     * 데이터
     *
     * @param userId 사용자ID
     * @param username 사용자명
     * @param reason 원인
     * @param endTime 종료 시간
     * @param remainingSeconds 시간(초)
     */
    public UserBlockedException(
            String userId, String username, String reason, LocalDateTime endTime, Long remainingSeconds) {
        super(buildMessage(username, reason, endTime, remainingSeconds));
        this.userId = userId;
        this.username = username;
        this.reason = reason;
        this.endTime = endTime;
        this.remainingSeconds = remainingSeconds;
    }

    /**
     * 생성예외메시지
     */
    private static String buildMessage(String username, String reason, LocalDateTime endTime, Long remainingSeconds) {
        long days = remainingSeconds / 86400;
        long hours = (remainingSeconds % 86400) / 3600;
        long minutes = (remainingSeconds % 3600) / 60;

        StringBuilder sb = new StringBuilder();
        sb.append("계정 ").append(username).append(" 완료");
        if (reason != null && !reason.isEmpty()) {
            sb.append(", 원인: ").append(reason);
        }
        sb.append(".시간: ");
        if (days > 0) {
            sb.append(days).append("");
        }
        if (hours > 0) {
            sb.append(hours).append("시간");
        }
        if (minutes > 0) {
            sb.append(minutes).append("분");
        }
        if (days == 0 && hours == 0 && minutes == 0) {
            sb.append(remainingSeconds).append("초");
        }
        return sb.toString();
    }
}