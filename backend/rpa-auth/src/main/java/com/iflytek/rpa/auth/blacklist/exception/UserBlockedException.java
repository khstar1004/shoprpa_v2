package com.iflytek.rpa.auth.blacklist.exception;

import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 차단된 사용자의 접근을 중단하는 예외입니다.
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
     * 차단 사유
     */
    private final String reason;

    /**
     * 종료 시간
     */
    private final LocalDateTime endTime;

    /**
     * 남은 시간(초)
     */
    private final Long remainingSeconds;

    /**
     * 사용자 차단 예외를 생성합니다.
     *
     * @param userId 사용자ID
     * @param username 사용자명
     * @param reason 차단 사유
     * @param endTime 종료 시간
     * @param remainingSeconds 남은 시간(초)
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
     * 사용자에게 전달할 차단 안내 메시지를 생성합니다.
     */
    private static String buildMessage(String username, String reason, LocalDateTime endTime, Long remainingSeconds) {
        long safeRemainingSeconds = Math.max(remainingSeconds != null ? remainingSeconds : 0L, 0L);
        long days = safeRemainingSeconds / 86400;
        long hours = (safeRemainingSeconds % 86400) / 3600;
        long minutes = (safeRemainingSeconds % 3600) / 60;

        StringBuilder sb = new StringBuilder();
        sb.append("계정 ").append(username).append("은 현재 차단 중입니다");
        if (reason != null && !reason.isEmpty()) {
            sb.append(", 사유: ").append(reason);
        }
        sb.append(". 남은 시간: ");
        if (days > 0) {
            sb.append(days).append("일");
        }
        if (hours > 0) {
            if (days > 0) {
                sb.append(" ");
            }
            sb.append(hours).append("시간");
        }
        if (minutes > 0) {
            if (days > 0 || hours > 0) {
                sb.append(" ");
            }
            sb.append(minutes).append("분");
        }
        if (days == 0 && hours == 0 && minutes == 0) {
            sb.append(safeRemainingSeconds).append("초");
        }
        return sb.toString();
    }
}
