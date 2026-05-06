package com.iflytek.rpa.auth.sp.uap.annotation;

import java.lang.annotation.*;
import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * 휴대폰 번호인증정상이면
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {PhoneValidator.class}) // 지정관리기기, 예휴대폰 번호형식인증예개유형검증
public @interface Phone {

    String pattern() default "^(?:(?:\\+|00)86)?1\\d{10}$";

    String message() default "휴대폰 번호형식법";

    Class<?>[] groups() default {}; // groups사용지정분그룹, 가능으로검증가져오기아니요의기기제어, 현재지정되지 않았습니다작업분그룹기기제어, 매필요행검증

    Class<? extends Payload>[] payload() default {};

    interface Default {}
}