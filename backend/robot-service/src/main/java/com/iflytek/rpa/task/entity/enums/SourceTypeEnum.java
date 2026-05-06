package com.iflytek.rpa.task.entity.enums;

import lombok.Getter;

/**
 * @author keler
 * @date 2021/10/9
 */
@Getter
public enum SourceTypeEnum {
    CLIENT("client", "클라이언트"),
    WEB("web", "웹 페이지단말"),
    ;

    private String code;
    private String name;

    SourceTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}