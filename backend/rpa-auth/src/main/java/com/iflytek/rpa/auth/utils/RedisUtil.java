package com.iflytek.rpa.auth.utils;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RedisUtil {
    public static void deleteRedisKeysByPrefix(String prefix) {
        try {
            Set<String> keys = RedisUtils.redisTemplate.keys(prefix + "*");
            if (!keys.isEmpty()) {
                RedisUtils.redisTemplate.delete(keys);
                log.info("성공삭제{}개으로'{}'로전의Redis", keys.size(), prefix);
            } else {
                log.info("찾을 수 없는 으로'{}'로전의Redis", prefix);
            }
        } catch (Exception e) {
            log.error("삭제Redis전실패: {}", e.getMessage(), e);
        }
    }
}