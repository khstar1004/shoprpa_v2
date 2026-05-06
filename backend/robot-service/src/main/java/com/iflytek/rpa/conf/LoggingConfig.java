package com.iflytek.rpa.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 로그매칭유형
 * 사용AspectJ관리으로지원AOP공가능
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class LoggingConfig {

    // 해당매칭유형확인AOP가능정상일반
    // @EnableAspectJAutoProxy(proxyTargetClass = true) 사용CGLIB관리, 
    // 가능으로있음연결의유형행관리
}