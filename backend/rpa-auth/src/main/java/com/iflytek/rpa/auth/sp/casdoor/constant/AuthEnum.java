package com.iflytek.rpa.auth.sp.casdoor.constant;

/**
 * @desc: TODO
 * @author: weilai <laiwei3@iflytek.com>
 * @create: 2025/12/11 10:21
 */
public enum AuthEnum {
    CASDOOR_CURRENT_USER_TOKEN("casdoor_current_user_token", "casdoor현재사용자의token");

    private final String code;
    private final String description;

    AuthEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    // 통신경과code가져오기 
    public static AuthEnum fromCode(String code) {
        for (AuthEnum ele : values()) {
            if (ele.code.equals(code)) {
                return ele;
            }
        }
        throw new IllegalArgumentException("지원하지 않는권한코드: " + code);
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}