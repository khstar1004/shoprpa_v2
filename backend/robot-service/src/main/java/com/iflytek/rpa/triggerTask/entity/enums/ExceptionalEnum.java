package com.iflytek.rpa.triggerTask.entity.enums;

import lombok.Getter;

@Getter
public enum ExceptionalEnum {
    JUMP("jump", "건너뛰기"),
    STOP("stop", "중중지"),
    ;

    private String code;
    private String name;

    ExceptionalEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}