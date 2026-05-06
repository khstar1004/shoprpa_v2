package com.iflytek.rpa.dispatch.entity.enums;

import lombok.Getter;

/**
 * 단말또는단말분그룹
 *
 * @author jqfang
 * @since 2025-08-15
 */
@Getter
public enum TerminalOrGroupType {
    TERMINAL("terminal", "단말"),
    GROUP("group", "단말분그룹"),
    ;

    private final String value;
    private final String name;

    TerminalOrGroupType(String value, String name) {
        this.value = value;
        this.name = name;
    }
}