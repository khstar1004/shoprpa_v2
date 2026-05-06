package com.iflytek.rpa.auth.core.entity.enums;

/**
 * 로그인방식
 * @author lihang
 * @date 2025-11-25
 */
public enum LoginModeEnum {
    /**
     * 없음비밀번호로그인
     */
    NOPASSWORD("NoPassword");

    private final String code;

    LoginModeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}