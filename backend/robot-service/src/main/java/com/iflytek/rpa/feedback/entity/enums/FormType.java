package com.iflytek.rpa.feedback.entity.enums;

import lombok.Getter;

/**
 * 문의테이블단일유형
 *
 * @author system
 * @since 2024-12-15
 */
@Getter
public enum FormType {
    PRO(1, "버전문의"),
    ENTERPRISE(2, "버전문의");

    private final Integer code;
    private final String name;

    FormType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 근거code가져오기 
     *
     * @param code 유형코드
     * @return 값, 결과가찾을 수 없습니다반환null
     */
    public static FormType getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FormType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}