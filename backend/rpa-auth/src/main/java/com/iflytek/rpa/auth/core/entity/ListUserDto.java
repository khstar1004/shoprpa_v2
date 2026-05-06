package com.iflytek.rpa.auth.core.entity;

import java.util.List;

/**
 * 분조회사용자목록정보DTO
 * @author xqcao2
 *
 */
public class ListUserDto extends PageQueryDto {

    /**
     * 사용자ID 합치기  사용조회지정사용자ID의사용자
     */
    private List<String> userIds;

    /**
     * 역할ID 합치기  사용조회역할아래의사용자
     */
    private List<String> roleIdList;

    /**
     * 기기ID 사용필터링개기기아래의사용자
     */
    private String orgId;

    /**
     * 로그인이름
     */
    private String loginName;

    /**
     * 이름
     */
    private String name;

    /**
     * 메일함
     */
    private String email;

    /**
     * 
     */
    private String phone;

    /**
     * 상태 사용자상태{0중지사용 1사용}
     */
    private Integer status = 1;

    /**
     * 여부조회데이터(낮음버전시 사용)
     */
    private boolean queryPageCount = false;

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }

    public List<String> getRoleIdList() {
        return roleIdList;
    }

    public void setRoleIdList(List<String> roleIdList) {
        this.roleIdList = roleIdList;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public boolean isQueryPageCount() {
        return queryPageCount;
    }

    public void setQueryPageCount(boolean queryPageCount) {
        this.queryPageCount = queryPageCount;
    }
}