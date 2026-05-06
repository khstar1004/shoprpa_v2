package com.iflytek.rpa.triggerTask.entity.enums;

import lombok.Getter;

/**
 * @author keler
 * @date 2021/10/9
 */
@Getter
public enum TaskTypeEnum {
    TIME_TASK("schedule", "시간"),
    FILE_TASK("file", "파일"),
    MAIL_TASK("mail", "메일함"),
    HOT_KEY_TASK("hotKey", "트리거"),
    MANUAL_TASK("manual", "트리거"),
    ;

    private String code;
    private String name;

    TaskTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}