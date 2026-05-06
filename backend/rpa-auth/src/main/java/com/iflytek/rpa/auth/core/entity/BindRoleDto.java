package com.iflytek.rpa.auth.core.entity;

import java.util.List;

/**
 * 지정역할DTO
 * @author xqcao2
 *
 */
public class BindRoleDto {

    /**
     * 사용자ID
     */
    private String userId;

    /**
     * 역할목록
     */
    private List<String> roleIdList;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getRoleIdList() {
        return roleIdList;
    }

    public void setRoleIdList(List<String> roleIdList) {
        this.roleIdList = roleIdList;
    }
}