package com.iflytek.rpa.feedback.entity.enums;

import lombok.Getter;

/**
 * 문의테이블단일상태
 *
 * @author system
 * @since 2024-12-15
 */
@Getter
public enum FormStatus {
    PENDING(0, "대기관리"),
    DONE(1, "완료관리"),
    IGNORE(2, "완료");

    private final Integer code;
    private final String name;

    FormStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 근거code가져오기 
     *
     * @param code 상태코드
     * @return 값, 결과가찾을 수 없습니다반환null
     */
    public static FormStatus getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FormStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}