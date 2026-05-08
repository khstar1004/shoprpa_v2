package com.iflytek.rpa.base.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("@annotation(logExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint, LogExecutionTime logExecutionTime) throws Throwable {

        // 입력방법법실행전의관리
        long startTime = System.currentTimeMillis();
        log.debug("method started");

        // 계속실행의방법법
        Object proceed = joinPoint.proceed();

        // 입력방법법실행후의관리
        long endTime = System.currentTimeMillis();
        Signature signature = joinPoint.getSignature(); // 가져오기방법법이름 :  방법법의전체경로
        long executionTime = endTime - startTime;
        log.debug("{} executed in {}ms", signature, executionTime);
        return proceed;
    }
}
