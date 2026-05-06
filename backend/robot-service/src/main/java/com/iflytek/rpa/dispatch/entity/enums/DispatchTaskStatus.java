package com.iflytek.rpa.dispatch.entity.enums;

import lombok.Getter;

/**
 * 스케줄링작업상태
 *
 * @author jqfang
 * @since 2025-08-15
 */
@Getter
public enum DispatchTaskStatus {
    ACTIVE("active", "사용중"),
    STOP("stop", "닫기"),
    EXPIRED("expired", "완료경과"),
    ;

    private final String value;
    private final String name;

    DispatchTaskStatus(String value, String name) {
        this.value = value;
        this.name = name;
    }
}