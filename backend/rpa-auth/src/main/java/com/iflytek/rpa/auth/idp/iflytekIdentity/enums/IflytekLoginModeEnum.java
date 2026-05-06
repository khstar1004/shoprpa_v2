package com.iflytek.rpa.auth.idp.iflytekIdentity.enums;

import lombok.Getter;

@Getter
public enum IflytekLoginModeEnum {
    PASSWORD("phone", "휴대폰 번호비밀번호로그인"),
    FREE("free", "없음비밀번호로그인");

    private final String value;
    private final String name;

    IflytekLoginModeEnum(String value, String name) {
        this.value = value;
        this.name = name;
    }
}