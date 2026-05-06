package com.iflytek.rpa.base.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 테이블개비고해제가능사용방법법위
@Retention(RetentionPolicy.RUNTIME) // 테이블개비고해제에서런타임가능사용
public @interface LogExecutionTime {}