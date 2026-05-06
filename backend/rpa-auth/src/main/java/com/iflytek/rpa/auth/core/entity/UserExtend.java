package com.iflytek.rpa.auth.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * @desc: 사용자유형
 * @author: weilai <laiwei3@iflytek.com>
 * @create: 2025/11/24 17:44
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserExtend extends TenantModeExtendDto {

    /**
     * 사용자정보
     */
    private User user;

    /**
     * 사용자정보
     */
    private List<UapExtendRelation> extands;

    /**
     * 역할정보
     */
    private List<RoleBaseDto> roles;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<UapExtendRelation> getExtands() {
        return extands;
    }

    public void setExtands(List<UapExtendRelation> extands) {
        this.extands = extands;
    }

    public List<RoleBaseDto> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleBaseDto> roles) {
        this.roles = roles;
    }
}