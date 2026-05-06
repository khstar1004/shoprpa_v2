package com.iflytek.rpa.auth.conf.condition;

import java.lang.annotation.*;
import org.springframework.context.annotation.Conditional;

/**
 * 파일비고해제: 모듈방식로 saas 또는 private-uap 시
 * 결과가매칭실패, 로 saas 방식(matchIfMissing = true)
 *
 * @author system
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(SaaSOrUAPCondition.class)
public @interface ConditionalOnSaaSOrUAP {}