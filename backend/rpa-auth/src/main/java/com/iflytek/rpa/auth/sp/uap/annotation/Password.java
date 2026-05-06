package com.iflytek.rpa.auth.sp.uap.annotation;

import java.lang.annotation.*;
import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * 비밀번호검증 비고해제
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {PassWordValidator.class})
public @interface Password {
    // 정상이면테이블방식
    String regexp() default "";

    // 검증아니요통신경과시의안내정보
    String message() default "비밀번호형식아니요정상, 입력하세요6-20위치의비밀번호, 패키지숫자, 문자또는기호";

    // 분그룹
    Class<?>[] groups() default {};

    // 합치기검증
    Class<? extends Payload>[] payload() default {};

    interface Default {}
}