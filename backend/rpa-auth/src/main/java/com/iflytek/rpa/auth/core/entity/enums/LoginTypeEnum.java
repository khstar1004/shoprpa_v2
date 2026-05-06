package com.iflytek.rpa.auth.core.entity.enums;

import lombok.Getter;

@Getter
public enum LoginTypeEnum {
    /**
     * 인증 코드 로그인
     */
    CODE("code", "인증 코드 로그인"),
    /**
     * 비밀번호로그인
     */
    PASSWORD("password", "비밀번호로그인");

    private final String value;
    private final String name;

    LoginTypeEnum(String value, String name) {
        this.value = value;
        this.name = name;
    }
}