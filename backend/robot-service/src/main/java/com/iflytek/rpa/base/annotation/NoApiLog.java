package com.iflytek.rpa.base.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 정렬제거API로그기록비고해제
 * 사용아니요필요기록로그의방법법(예문의연결대기높이호출의방법법)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoApiLog {

    /**
     * 정렬제거원인설명
     */
    String value() default "";
}