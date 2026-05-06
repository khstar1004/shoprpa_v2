package com.iflytek.rpa.auth.dataPreheater.entity;

import lombok.Getter;

@Getter
public enum MarketTypeEnum {
    TEAM("team", "팀마켓"),
    OFFICIAL("official", "방법마켓"),
    PUBLIC("public", "공유마켓"),
    ;

    private final String code;
    private final String name;

    MarketTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}