package com.iflytek.rpa.auth.core.entity;

/**
 * @author: boqiu3
 * @date: 2023/4/21 9:41
 * @description: 사용다중테넌트방식아래, 연결매개변수 사용
 */
public class TenantModeExtendDto {

    /**
     * 테넌트id
     */
    private String tenantId;

    /**
     *
     */
    private String requestUrl;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }
}