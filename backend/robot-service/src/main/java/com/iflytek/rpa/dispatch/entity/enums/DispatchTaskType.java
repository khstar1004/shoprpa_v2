package com.iflytek.rpa.dispatch.entity.enums;

import lombok.Getter;

/**
 * 스케줄링작업유형
 *
 * @author jqfang
 * @since 2025-08-15
 */
@Getter
public enum DispatchTaskType {
    MANUAL("manual", "트리거"),
    SCHEDULE("schedule", "예약"),
    TRIGGER("trigger", "예약트리거"),
    ;

    private final String value;
    private final String name;

    DispatchTaskType(String value, String name) {
        this.value = value;
        this.name = name;
    }
    // 추가근거value값가져오기 의방법법
    public static DispatchTaskType getByValue(String value) {
        for (DispatchTaskType type : values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("지원하지 않는의작업유형: " + value);
    }
}