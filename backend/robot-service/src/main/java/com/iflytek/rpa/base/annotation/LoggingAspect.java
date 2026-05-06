package com.iflytek.rpa.base.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    @Around("@annotation(logExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint, LogExecutionTime logExecutionTime) throws Throwable {

        // 입력방법법실행전의관리
        long startTime = System.currentTimeMillis();
        System.out.println("### method started !!! ### ");

        // 계속실행의방법법
        Object proceed = joinPoint.proceed();

        // 입력방법법실행후의관리
        long endTime = System.currentTimeMillis();
        Signature signature = joinPoint.getSignature(); // 가져오기방법법이름 :  방법법의전체경로
        long executionTime = endTime - startTime;
        System.out.println("### method ended !!! ### ");
        System.out.println(signature + "executed in " + executionTime + "ms");
        return proceed;
    }
}