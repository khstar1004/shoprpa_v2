package com.iflytek.rpa.terminal.entity.enums;

import lombok.Getter;

@Getter
public enum UsageTypeEnum {
    ALL("all", "모든사람"),
    DEPT("dept", "모듈모든사람"),
    SELECT("select", "지정사람"),
    ;

    private final String code;
    private final String name;

    UsageTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}