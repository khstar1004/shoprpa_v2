package com.iflytek.rpa.dispatch.entity.enums;

import lombok.Getter;

/**
 * 아래발송작업
 */
@Getter
public enum DispatchTaskFromType {
    NORMAL("normal", "통신"),
    RETRY("retry", "재시도"),
    STOP("stop", "결과"),
    ;

    private final String value;
    private final String name;

    DispatchTaskFromType(String value, String name) {
        this.value = value;
        this.name = name;
    }
}