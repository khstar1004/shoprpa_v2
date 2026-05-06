package com.iflytek.rpa.auth.blacklist.config;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 이름단일매칭유형
 * 매칭시길이
 *
 * @author system
 * @date 2025-12-16
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "blacklist")
public class BlacklistConfig {

    /**
     * 시길이매칭(초)
     * level 1 -> 1시간
     * level 2 -> 1
     * level 3 -> 7
     * level 4 -> 30
     * level 5 -> 365
     * 초과경과 level 5 이면사용후일개대기단계의시길이
     */
    private List<Long> durations;

    /**
     * 비밀번호오류데이터제한제어
     */
    private Integer passwordErrorLimit = 5;

    /**
     * 비밀번호오류계획데이터경과시간(초)
     * 사용매칭의일개대기단계(1시간), 확인및시길이매칭일
     */
    private Long passwordErrorExpire;

    /**
     * 데이터방문데이터제한제어
     */
    private Integer sensitiveAccessLimit = 1;

    /**
     * 데이터제한제어
     */
    private Integer violationLimit = 3;

    /**
     * 매칭
     */
    @PostConstruct
    public void init() {
        if (durations == null || durations.isEmpty()) {
            durations = new ArrayList<>();
            durations.add(3600L); // 1시간
            durations.add(86400L); // 1
            durations.add(604800L); // 7
            durations.add(2592000L); // 30
            durations.add(31536000L); // 365
        }

        // 비밀번호오류계획데이터경과시간사용매칭의일개대기단계(1시간)
        if (passwordErrorExpire == null && !durations.isEmpty()) {
            passwordErrorExpire = durations.get(0);
        }
    }

    /**
     * 근거대기단계가져오기 시길이(초)
     *
     * @param level 대기단계
     * @return 시길이(초)
     */
    public Long getDurationByLevel(int level) {
        if (level <= 0) {
            return durations.get(0);
        }
        if (level > durations.size()) {
            // 초과경과대대기단계, 반환후일개대기단계의시길이(또는가능로)
            return durations.get(durations.size() - 1);
        }
        return durations.get(level - 1);
    }

    /**
     * 가져오기비밀번호오류 Redis Key
     *
     * @param userId 사용자ID
     * @return Redis Key
     */
    public static String getPasswordErrorKey(String userId) {
        return "LOGIN_FAIL:" + userId;
    }

    /**
     * 가져오기 이름단일 Redis Key
     *
     * @param userId 사용자ID
     * @return Redis Key
     */
    public static String getBlacklistKey(String userId) {
        return "BL:user:" + userId;
    }
}