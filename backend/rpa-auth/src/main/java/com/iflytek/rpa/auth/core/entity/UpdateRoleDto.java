package com.iflytek.rpa.auth.core.entity;

/**
 * 업데이트역할DTO
 * @author xqcao2
 *
 */
public class UpdateRoleDto {

    /**
     * 기본 키id
     */
    private String id;

    /**
     * 역할이름
     */
    private String name;

    /**
     * 역할코드
     */
    private String code;

    /**
     * 역할상태 역할상태{0중지사용 1사용}
     */
    private Integer status = 1;

    /**
     * 사용id
     */
    private String appId;

    /**
     * 위단계역할 ID
     */
    private String higherRole;

    /**
     * 정렬필드
     */
    private Integer sort = 1;

    /**
     * 비고
     */
    private String remark;

    /**
     * 해당역할아래메뉴및공가능의지정,1:강함지정, 0: 강함지정
     */
    private Integer isMustBind = 1;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getHigherRole() {
        return higherRole;
    }

    public void setHigherRole(String higherRole) {
        this.higherRole = higherRole;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getIsMustBind() {
        return isMustBind;
    }

    public void setIsMustBind(Integer isMustBind) {
        this.isMustBind = isMustBind;
    }
}