package com.iflytek.rpa.auth.core.entity;

/**
 * 근거역할ID 분조회사용자목록정보DTO
 *
 * 비고: 현재역할  아니요역할
 * @author xqcao2
 *
 */
public class ListUserByRoleDto extends PageQueryDto {

    /**
     * 테넌트ID
     */
    private String tenantId;

    /**
     * 역할ID 조회현재역할 아니요패키지역할
     */
    private String roleId;

    /**
     * 기기ID 근거기기ID필터링 현재기기  아니요패키지기기
     */
    private String orgId;

    /**
     * 검색내용  로그인이름 이름
     */
    private String keyWord;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getKeyWord() {
        return keyWord;
    }

    public void setKeyWord(String keyWord) {
        this.keyWord = keyWord;
    }
}