package com.iflytek.rpa.auth.core.entity;

/**
 * 분조회사용자목록정보DTO
 * @author xqcao2
 *
 */
public class ListRoleDto extends PageQueryDto {

    /**
     * 역할ID
     */
    private String roleId;

    /**
     * 위단계역할ID
     */
    private String parentRoleId;

    /**
     * 역할이름
     */
    private String roleName;

    /**
     * 사용ID
     */
    private String appId;

    /**
     * 사용코드
     */
    private String appCode;

    /**
     * 사용자ID
     */
    private String userId;

    /**
     * 테넌트ID
     * 예로완료내용낮음버전 추가입력필드 요청 사용필드
     */
    private String tenantId;

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getParentRoleId() {
        return parentRoleId;
    }

    public void setParentRoleId(String parentRoleId) {
        this.parentRoleId = parentRoleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}