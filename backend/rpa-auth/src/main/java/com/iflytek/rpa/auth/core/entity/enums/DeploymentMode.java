package com.iflytek.rpa.auth.core.entity.enums;

/**
 * 모듈방식
 */
public enum DeploymentMode {
    /**
     * SaaS모듈 - 사용ShopRPA 계정인증
     */
    SAAS("saas", "SaaS모듈"),

    /**
     * 있음모듈 - SSO인증
     */
    PRIVATE_ENTERPRISE("private-enterprise", "있음모듈-SSO"),

    /**
     * 있음모듈 - 내부모듈UAP인증
     */
    PRIVATE_UAP("private-uap", "있음모듈-내부모듈UAP"),

    /**
     * Casdoor 모듈
     */
    CASDOOR("casdoor", "Casdoor 모듈");

    private final String code;
    private final String description;

    DeploymentMode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DeploymentMode fromCode(String code) {
        for (DeploymentMode mode : values()) {
            if (mode.code.equalsIgnoreCase(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("지원하지 않는의모듈방식: " + code);
    }
}