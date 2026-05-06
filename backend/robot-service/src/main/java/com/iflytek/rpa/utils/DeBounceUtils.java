package com.iflytek.rpa.utils;

import static com.iflytek.rpa.utils.RedisUtils.redisTemplate;

import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.concurrent.TimeUnit;

/**
 * 닫기
 */
public class DeBounceUtils {

    /**
     * redis행관리
     * @param createLikeKey
     * @return
     */
    public static void deBounce(String createLikeKey, Long deBounceWindow) {
        Boolean b = redisTemplate.hasKey(createLikeKey);
        if (b != null && b) {
            // 완료저장에서완료
            redisTemplate.expire(createLikeKey, deBounceWindow, TimeUnit.MILLISECONDS);
            throw new ServiceException(ErrorCodeEnum.E_SERVICE.getCode(), "빠름완료, 후시도");
        }
        {
            // 찾을 수 없습니다
            redisTemplate.opsForValue().set(createLikeKey, "1", deBounceWindow, TimeUnit.MILLISECONDS);
        }
    }
}