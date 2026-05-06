package com.iflytek.rpa.robot.entity.enums;

import lombok.Getter;

@Getter
public enum SharedVarTypeEnum {
    TEXT("text", "텍스트"),
    PASSWORD("password", "비밀번호"),
    ARRAY("array", "배열"),
    GROUP("group", "변수그룹"),
    ;

    private final String code;
    private final String name;

    SharedVarTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}