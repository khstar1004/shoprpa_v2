package com.iflytek.rpa.utils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;

public class RedisUtils {
    public static RedisTemplate<String, Object> redisTemplate;

    /**
     * 지정저장실패시간
     *
     * @param key  
     * @param time 시간(초)
     * @return
     */
    public static boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return false;
        }
    }

    /**
     * 근거key 가져오기경과시간
     *
     * @param key  할 수 없음로null
     * @return 시간(초) 반환0테이블로있음
     */
    public static long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * key여부저장에서
     *
     * @param key 
     * @return true 저장에서 false찾을 수 없습니다
     */
    public static boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return false;
        }
    }

    /**
     * 삭제저장
     *
     * @param key 가능으로일개값 또는다중개
     */
    @SuppressWarnings("unchecked")
    public static void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete(CollectionUtils.arrayToList(key));
            }
        }
    }

    // ============================String=============================

    /**
     * 통신저장가져오기
     *
     * @param key 
     * @return 값
     */
    public static Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 통신저장입력
     *
     * @param key   
     * @param value 값
     * @return true성공 false실패
     */
    public static boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return false;
        }
    }

    /**
     * 통신저장입력시간
     *
     * @param key   
     * @param value 값
     * @param time  시간(초) time필요대0 결과가time소대기0 를없음제한
     * @return true성공 false 실패
     */
    public static boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return false;
        }
    }

    /**
     * 증가
     *
     * @param key   
     * @param delta 필요증가추가(대0)
     * @return
     */
    public static long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("증가원인대0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 시도가져오기분방식(재입력), 사용 SET NX PX/EX 
     * @param key     
     * @param value   요청 식별자, 오류삭제(예 UUID)
     * @param timeout 시간 초과시간
     * @param unit    시간단일위치
     * @return true 가져오기성공;false 가져오기실패
     */
    public static boolean tryLock(String key, String value, long timeout, TimeUnit unit) {
        try {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            LoggerUtils.error("redis추가예외", e);
            return false;
        }
    }

    /**
     * 분방식, 삭제있음의
     * @param key   
     * @param value 요청 식별자
     */
    public static void unlock(String key, String value) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (Objects.equals(cached, value)) {
                redisTemplate.delete(key);
            }
        } catch (Exception e) {
            LoggerUtils.error("redis해제예외", e);
        }
    }

    /**
     * 
     *
     * @param key   
     * @param delta 필요적음(소0)
     * @return
     */
    public static long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("원인대0");
        }
        return redisTemplate.opsForValue().increment(key, -delta);
    }

    // ================================Map=================================

    /**
     * HashGet
     *
     * @param key   할 수 없음로null
     * @param item  할 수 없음로null
     * @return 값
     */
    public static Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * 가져오기hashKey의모든값
     *
     * @param key 
     * @return 의다중개값
     */
    public static Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * HashSet
     *
     * @param key 
     * @param map 다중개값
     * @return true 성공 false 실패
     */
    public static boolean hmset(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return false;
        }
    }

    /**
     * HashSet 시간
     *
     * @param key  
     * @param map  다중개값
     * @param time 시간(초)
     * @return true성공 false실패
     */
    public static boolean hmset(String key, Map<String, Object> map, long time) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return false;
        }
    }

    /**
     * 일hash테이블중입력데이터,결과가찾을 수 없습니다를생성
     *
     * @param key   
     * @param item  
     * @param value 값
     * @return true 성공 false실패
     */
    public static boolean hset(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return false;
        }
    }

    /**
     * 일hash테이블중입력데이터,결과가찾을 수 없습니다를생성
     *
     * @param key   
     * @param item  
     * @param value 값
     * @param time  시간(초)  비고:결과가완료저장에서의hash테이블있음시간,를기존있음의시간
     * @return true 성공 false실패
     */
    public static boolean hset(String key, String item, Object value, long time) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return false;
        }
    }

    /**
     * 삭제hash테이블중의값
     *
     * @param key   할 수 없음로null
     * @param item  가능으로다중개 할 수 없음로null
     */
    public static void hdel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    /**
     * hash테이블중여부있음해당의값
     *
     * @param key   할 수 없음로null
     * @param item  할 수 없음로null
     * @return true 저장에서 false찾을 수 없습니다
     */
    public static boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    /**
     * hash증가 결과가찾을 수 없습니다,생성일개 추가후의값반환
     *
     * @param key  
     * @param item 
     * @param by   필요증가추가(대0)
     * @return
     */
    public static double hincr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    /**
     * hash
     *
     * @param key  
     * @param item 
     * @param by   필요적음(소0)
     * @return
     */
    public static double hdecr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, -by);
    }

    // ============================set=============================

    /**
     * 근거key가져오기Set중의모든값
     *
     * @param key 
     */
    public static Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return null;
        }
    }

    /**
     * 근거value에서일개set중조회,여부저장에서
     *
     * @param key   
     * @param value 값
     * @return true 저장에서 false찾을 수 없습니다
     */
    public static boolean sHasKey(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return false;
        }
    }

    /**
     * 를데이터입력set저장
     *
     * @param key    
     * @param values 값 가능으로예다중개
     * @return 성공개데이터
     */
    public static long sSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return 0;
        }
    }

    /**
     * 를set데이터입력저장
     *
     * @param key    
     * @param time   시간(초)
     * @param values 값 가능으로예다중개
     * @return 성공개데이터
     */
    public static long sSetAndTime(String key, long time, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return count;
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return 0;
        }
    }

    /**
     * 가져오기set저장의길이정도
     *
     * @param key 
     * @return
     */
    public static long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return 0;
        }
    }

    /**
     * 제거값로value의
     *
     * @param key    
     * @param values 값 가능으로예다중개
     * @return 제거의개데이터
     */
    public static long setRemove(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().remove(key, values);
            return count;
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return 0;
        }
    }
    // ===============================list=================================

    /**
     * 가져오기list저장의내용
     *
     * @param key   
     * @param start 열기 
     * @param end   결과  0 까지 -1테이블모든값
     * @return
     */
    public static List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return null;
        }
    }

    /**
     * 가져오기list저장의길이정도
     *
     * @param key 
     * @return
     */
    public static long lGetListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return 0;
        }
    }

    /**
     * 를list입력저장
     *
     * @param key   
     * @param value 값
     * @return
     */
    public static boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return false;
        }
    }

    /**
     * 를list입력저장
     *
     * @param key   
     * @param value 값
     * @param time  시간(초)
     * @return
     */
    public static boolean lSet(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return false;
        }
    }

    /**
     * 를list입력저장
     *
     * @param key   
     * @param value 값
     * @return
     */
    public static boolean lSet(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return false;
        }
    }

    /**
     * 를list입력저장
     *
     * @param key   
     * @param value 값
     * @param time  시간(초)
     * @return
     */
    public static boolean lSet(String key, List<Object> value, long time) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return false;
        }
    }

    /**
     * 근거검색수정list중의데이터
     *
     * @param key   
     * @param index 검색
     * @param value 값
     * @return
     */
    public static boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return false;
        }
    }

    /**
     * 제거N개값로value
     *
     * @param key   
     * @param count 제거다중적음개
     * @param value 값
     * @return 제거의개데이터
     */
    public static long lRemove(String key, long count, Object value) {
        try {
            return redisTemplate.opsForList().remove(key, count, value);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return 0;
        }
    }

    /**
     * 있음순서합치기추가일개또는다중개구성원, 또는업데이트완료저장에서구성원의분데이터
     *
     * @param key   
     * @param value 값
     * @param score 분데이터
     */
    public static Boolean zAdd(String key, Object value, double score) {
        try {
            return redisTemplate.opsForZSet().add(key, value, score);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return false;
        }
    }

    // ===============================ZSet=================================

    /**
     * 가져오기있음순서합치기의구성원데이터
     *
     * @param key 
     */
    public static Long zCard(String key) {
        try {
            return redisTemplate.opsForZSet().size(key);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return null;
        }
    }

    /**
     * 계획에서있음순서합치기중지정분데이터의구성원데이터
     *
     * @param key 
     * @param min 소분데이터
     * @param max 대분데이터
     */
    public static Long zCount(String key, double min, double max) {
        try {
            return redisTemplate.opsForZSet().count(key, min, max);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return null;
        }
    }

    /**
     * 있음순서합치기중지정구성원의분데이터추가위증가량
     *
     * @param key   
     * @param value 값
     * @param delta 증가량
     */
    public static Double zIncrementScore(String key, Object value, double delta) {
        try {
            return redisTemplate.opsForZSet().incrementScore(key, value, delta);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return null;
        }
    }

    /**
     * 통신경과검색반환있음순서합치기지정내부의구성원(에서소까지대)
     *
     * @param key   
     * @param start 열기 검색
     * @param end   결과검색
     */
    public static Set<Object> zRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForZSet().range(key, start, end);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return null;
        }
    }

    /**
     * 통신경과분데이터반환있음순서합치기지정내부의구성원(에서소까지대)
     *
     * @param key 
     * @param min 소분데이터
     * @param max 대분데이터
     */
    public static Set<Object> zRangeByScore(String key, double min, double max) {
        try {
            return redisTemplate.opsForZSet().rangeByScore(key, min, max);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return null;
        }
    }

    /**
     * 반환있음순서합치기중지정구성원의검색(에서소까지대)
     *
     * @param key   
     * @param value 값
     */
    public static Long zRank(String key, Object value) {
        try {
            return redisTemplate.opsForZSet().rank(key, value);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return null;
        }
    }

    /**
     * 제거있음순서합치기중의일개또는다중개구성원
     *
     * @param key    
     * @param values 값
     */
    public static Long zRemove(String key, Object... values) {
        try {
            return redisTemplate.opsForZSet().remove(key, values);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return null;
        }
    }

    /**
     * 제거있음순서합치기중지정의정렬이름의모든구성원
     *
     * @param key   
     * @param start 열기 검색
     * @param end   결과검색
     */
    public static Long zRemoveRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForZSet().removeRange(key, start, end);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return null;
        }
    }

    /**
     * 제거있음순서합치기중지정의분데이터의모든구성원
     *
     * @param key 
     * @param min 소분데이터
     * @param max 대분데이터
     */
    public static Long zRemoveRangeByScore(String key, double min, double max) {
        try {
            return redisTemplate.opsForZSet().removeRangeByScore(key, min, max);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return null;
        }
    }

    /**
     * 통신경과검색반환있음순서합치기지정내부의구성원(에서대까지소)
     *
     * @param key   
     * @param start 열기 검색
     * @param end   결과검색
     */
    public static Set<Object> zReverseRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForZSet().reverseRange(key, start, end);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return null;
        }
    }

    /**
     * 반환있음순서합치기중지정구성원의검색(에서대까지소)
     *
     * @param key   
     * @param value 값
     */
    public static Long zReverseRank(String key, Object value) {
        try {
            return redisTemplate.opsForZSet().reverseRank(key, value);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return null;
        }
    }

    /**
     * 반환있음순서중, 구성원의분데이터값
     *
     * @param key   
     * @param value 값
     */
    public static Double zScore(String key, Object value) {
        try {
            return redisTemplate.opsForZSet().score(key, value);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return null;
        }
    }

    /**
     * 계획지정의일개또는다중개있음순서의, 저장에서새의있음순서합치기중
     *
     * @param key      새합치기
     * @param otherKey 합치기
     * @param destKey  목록 합치기
     */
    public static Long zUnionAndStore(String key, String otherKey, String destKey) {
        try {
            return redisTemplate.opsForZSet().unionAndStore(key, otherKey, destKey);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return null;
        }
    }

    /**
     * 계획지정의일개또는다중개있음순서의, 저장에서새의있음순서합치기중
     *
     * @param key      새합치기
     * @param otherKey 합치기
     * @param destKey  목록 합치기
     */
    public static Long zIntersectAndStore(String key, String otherKey, String destKey) {
        try {
            return redisTemplate.opsForZSet().intersectAndStore(key, otherKey, destKey);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return null;
        }
    }

    /**
     * 통신경과검색 가져오기list중의값
     *
     * @param key   
     * @param index 검색  index>=0시,  0 테이블, 1 이개요소, 유형;index<0시, -1, 테이블, -2데이터이개요소, 유형
     * @return
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            LoggerUtils.error("redis예외", e);
            return null;
        }
    }
}