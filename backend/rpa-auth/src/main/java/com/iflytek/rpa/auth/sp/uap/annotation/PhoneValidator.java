package com.iflytek.rpa.auth.sp.uap.annotation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * 검증관리기기: 휴대폰 번호형식인증의유형
 */
public class PhoneValidator implements ConstraintValidator<Phone, String> {

    // 비고해제객체
    private Phone phone;

    // [Phone]객체
    @Override
    public void initialize(Phone constraintAnnotation) {
        phone = constraintAnnotation;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 가져오기[Phone]객체의기기형식인증테이블방식
        String pattern = phone.pattern();
        Pattern compile = Pattern.compile(pattern);
        Matcher matcher = compile.matcher(value);
        return matcher.matches();
    }
}