package com.iflytek.rpa.base.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API로그기록비고해제
 * 사용필요기록입력매개, 출력매개, 예외및시의방법법
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiLog {

    /**
     * 설명
     */
    String value() default "";

    /**
     * 여부기록요청 매개변수
     */
    boolean logParams() default true;

    /**
     * 여부기록반환결과
     */
    boolean logResult() default true;

    /**
     * 여부기록예외정보
     */
    boolean logException() default true;

    /**
     * 여부기록실행시간
     */
    boolean logTime() default true;

    /**
     * 매개변수 대길이정도(초과경과이면가져오기)
     */
    int maxParamLength() default 2000;

    /**
     * 반환결과대길이정도(초과경과이면가져오기)
     */
    int maxResultLength() default 2000;
}