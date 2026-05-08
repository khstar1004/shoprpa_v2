package com.iflytek.rpa.auth.blacklist.config;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 사용자 차단 정책 설정
 *
 * @author system
 * @date 2025-12-16
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "blacklist")
public class BlacklistConfig {

    /**
     * 차단 단계별 기간(초)
     * level 1 -> 1시간
     * level 2 -> 1일
     * level 3 -> 7일
     * level 4 -> 30일
     * level 5 -> 365일
     * level 5를 초과하면 마지막 단계 기간을 사용합니다.
     */
    private List<Long> durations;

    /**
     * 비밀번호 오류 허용 횟수
     */
    private Integer passwordErrorLimit = 5;

    /**
     * 비밀번호 오류 카운터 만료 시간(초)
     */
    private Long passwordErrorExpire;

    /**
     * 민감 데이터 접근 허용 횟수
     */
    private Integer sensitiveAccessLimit = 1;

    /**
     * 정책 위반 허용 횟수
     */
    private Integer violationLimit = 3;

    /**
     * 기본 차단 정책을 초기화합니다.
     */
    @PostConstruct
    public void init() {
        if (durations == null || durations.isEmpty()) {
            durations = new ArrayList<>();
            durations.add(3600L); // 1시간
            durations.add(86400L); // 1일
            durations.add(604800L); // 7일
            durations.add(2592000L); // 30일
            durations.add(31536000L); // 365일
        }

        if (passwordErrorExpire == null && !durations.isEmpty()) {
            passwordErrorExpire = durations.get(0);
        }
    }

    /**
     * 차단 단계에 맞는 차단 기간을 반환합니다.
     *
     * @param level 차단 단계
     * @return 차단 기간(초)
     */
    public Long getDurationByLevel(int level) {
        if (level <= 0) {
            return durations.get(0);
        }
        if (level > durations.size()) {
            return durations.get(durations.size() - 1);
        }
        return durations.get(level - 1);
    }

    /**
     * 비밀번호 오류 Redis Key를 생성합니다.
     *
     * @param userId 사용자ID
     * @return Redis Key
     */
    public static String getPasswordErrorKey(String userId) {
        return "LOGIN_FAIL:" + userId;
    }

    /**
     * 사용자 차단 Redis Key를 생성합니다.
     *
     * @param userId 사용자ID
     * @return Redis Key
     */
    public static String getBlacklistKey(String userId) {
        return "BL:user:" + userId;
    }
}
