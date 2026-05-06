package com.iflytek.rpa.component.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 상태
 *
 * @author makejava
 * @since 2024-12-19
 */
@Getter
@AllArgsConstructor
public enum BlockStatusEnum {

    /**
     * 미완료
     */
    UNBLOCKED(0, "미완료"),

    /**
     * 완료
     */
    BLOCKED(1, "완료");

    /**
     * 상태코드
     */
    private final Integer code;

    /**
     * 상태설명
     */
    private final String description;

    /**
     * 근거상태코드가져오기 
     */
    public static BlockStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (BlockStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 여부로있음상태코드
     */
    public static boolean isValidCode(Integer code) {
        return getByCode(code) != null;
    }
}