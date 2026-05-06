package com.iflytek.rpa.robot.entity.enums;

import lombok.Getter;

/**
 * 파일량상태
 */
@Getter
public enum FileIndexStatus {
    START("", 1),
    END("완료", 2),
    FAIL("실패", 3),
    ;

    private final String comment;
    private final Integer value;

    FileIndexStatus(String comment, Integer value) {
        this.comment = comment;
        this.value = value;
    }
}