package com.iflytek.rpa.component.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 컴포넌트상태
 *
 * @author makejava
 * @since 2024-12-19
 */
@Getter
@AllArgsConstructor
public enum ComponentStatusEnum {

    /**
     * 상태
     */
    RESOURCE_STATUS_TO_OBTAIN("toObtain", "대기가져오기"),
    RESOURCE_STATUS_OBTAINED("obtained", "완료가져오기"),
    RESOURCE_STATUS_TO_UPDATE("toUpdate", "대기업데이트"),

    /**
     * 데이터
     */
    DATA_SOURCE_CREATE("create", "생성"),
    DATA_SOURCE_MARKET("market", "마켓가져오기"),

    /**
     * 변환상태
     */
    TRANSFORM_STATUS_EDITING("editing", "중"),
    TRANSFORM_STATUS_PUBLISHED("published", "완료발송버전"),
    TRANSFORM_STATUS_SHARED("shared", "완료위"),
    TRANSFORM_STATUS_LOCKED("locked", "지정");

    /**
     * 상태코드
     */
    private final String code;

    /**
     * 상태이름
     */
    private final String name;

    /**
     * 근거상태코드가져오기상태이름
     */
    public static String getNameByCode(String code) {
        for (ComponentStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum.getName();
            }
        }
        return "지원하지 않는상태";
    }

    /**
     * 근거상태코드가져오기 
     */
    public static ComponentStatusEnum getByCode(String code) {
        for (ComponentStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }
}