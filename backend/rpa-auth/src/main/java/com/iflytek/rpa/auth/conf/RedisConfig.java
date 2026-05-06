package com.iflytek.rpa.auth.conf;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.iflytek.rpa.auth.utils.RedisUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis매칭유형
 * 사용매칭RedisTemplate비고입력까지RedisUtils
 */
@Configuration
public class RedisConfig {

    /**
     * 생성RedisTemplate Bean
     * 에서생성후까지RedisUtils의필드
     *
     * @param factory Redis연결, Spring비고입력
     * @return 매칭의RedisTemplate
     */
    @Bean(name = "rpaRedisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 매칭연결
        template.setConnectionFactory(factory);

        // 사용Jackson2JsonRedisSerializer순서열및반대순서열redis의value값
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        // 지정필요순서열의, field,get및set,으로기호, ANY예있음패키지private및public
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 지정순서열입력의유형, 유형예final의, final의유형, 예String,Integer대기출력예외
        // 사용새버전API(Jackson 2.10+), 버전의enableDefaultTyping완료저장에서설치전체
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        // 사용StringRedisSerializer순서열및반대순서열redis의key값
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        // 값사용json순서열
        template.setValueSerializer(jackson2JsonRedisSerializer);
        // hash key 및value순서열방식
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        // 열기시작서비스지원(결과가필요Redis서비스공가능, 가져오기 비고아래행)
        // template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();

        // 를RedisTemplate까지RedisUtils의필드
        RedisUtils.redisTemplate = template;

        return template;
    }

    /**
     * 생성UAP내용의RedisTemplate Bean
     * 사용및UAP의JDK순서열방식, 사용저장UAP의token
     *
     * @param factory Redis연결, Spring비고입력
     * @return 매칭의UAP내용RedisTemplate
     */
    @Bean(name = "uapCompatibleRedisTemplate")
    public RedisTemplate<String, Object> uapCompatibleRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 매칭연결
        template.setConnectionFactory(factory);

        // 사용StringRedisSerializer순서열key(및UAP일)
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // 사용JdkSerializationRedisSerializer순서열value(및UAP일)
        org.springframework.data.redis.serializer.JdkSerializationRedisSerializer jdkSerializer =
                new org.springframework.data.redis.serializer.JdkSerializationRedisSerializer();

        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jdkSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(jdkSerializer);

        template.afterPropertiesSet();

        return template;
    }
}