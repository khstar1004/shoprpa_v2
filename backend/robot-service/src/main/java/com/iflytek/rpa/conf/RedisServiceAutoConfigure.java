package com.iflytek.rpa.conf;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.rpa.conf.service.RedisService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author keler
 * @date 2021/7/26
 */
@Configuration
@ConditionalOnClass(RedisService.class)
@ConditionalOnProperty(name = "redis.open", havingValue = "true")
public class RedisServiceAutoConfigure {

    @Bean
    @ConditionalOnMissingBean
    public RedisService redisService(@Qualifier("rpaRedisTemplate") RedisTemplate<String, Object> redisTemplate) {
        return new RedisService(redisTemplate);
    }

    /**
     * retemplate종료 매칭
     *
     * @param factory
     * @return
     */
    @Bean("rpaRedisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 매칭연결
        template.setConnectionFactory(factory);

        // 사용Jackson2JsonRedisSerializer순서열및반대순서열redis의value값(사용JDK의순서열방식)
        Jackson2JsonRedisSerializer jacksonSeial = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        // 지정필요순서열의, field,get및set,으로기호, ANY예있음패키지private및public
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 지정순서열입력의유형, 유형예final의, final의유형, 예String,Integer대기출력예외
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jacksonSeial.setObjectMapper(om);

        // 사용StringRedisSerializer순서열및반대순서열redis의key값
        template.setKeySerializer(new StringRedisSerializer());
        // 값사용json순서열
        //        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setValueSerializer(jacksonSeial);

        // hash key 및value순서열방식
        template.setHashKeySerializer(new StringRedisSerializer());
        //        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(jacksonSeial);

        // 열기시작서비스
        template.setEnableTransactionSupport(true);
        template.afterPropertiesSet();

        return template;
    }

    /**
     * hash유형의데이터
     *
     * @param redisTemplate
     * @return
     */
    @Bean
    public HashOperations<String, String, Object> hashOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForHash();
    }

    /**
     * redis문자열유형데이터
     *
     * @param redisTemplate
     * @return
     */
    @Bean
    public ValueOperations<String, Object> valueOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForValue();
    }

    /**
     * 테이블유형의데이터
     *
     * @param redisTemplate
     * @return
     */
    @Bean
    public ListOperations<String, Object> listOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForList();
    }

    /**
     * 없음순서합치기유형의데이터
     *
     * @param redisTemplate
     * @return
     */
    @Bean
    public SetOperations<String, Object> setOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForSet();
    }

    /**
     * 있음순서합치기유형의데이터
     *
     * @param redisTemplate
     * @return
     */
    @Bean
    public ZSetOperations<String, Object> zSetOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForZSet();
    }
}