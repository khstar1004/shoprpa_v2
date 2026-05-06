package com.iflytek.rpa.market.entity.enums;

import lombok.Getter;

@Getter
public enum ExpireTypeEnum {
    FOUR_HOURS("4H", "4시간"),
    TWENTY_FOUR_HOURS("24H", "24시간"),
    SEVEN_DAYS("7D", "7"),
    THIRTY_DAYS("30D", "30"),
    ;

    private final String code;
    private final String name;

    ExpireTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 근거code가져오기 
     *
     * @param code 실패시간유형코드
     * @return 의값, 결과가찾을 수 없습니다이면반환null
     */
    public static ExpireTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (ExpireTypeEnum expireTypeEnum : values()) {
            if (expireTypeEnum.getCode().equalsIgnoreCase(code)) {
                return expireTypeEnum;
            }
        }
        return null;
    }
}