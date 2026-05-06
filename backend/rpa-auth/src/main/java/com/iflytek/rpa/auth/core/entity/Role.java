package com.iflytek.rpa.auth.core.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.Date;

/**
 * @desc: 역할유형
 * @author: weilai <laiwei3@iflytek.com>
 * @create: 2025/11/24 16:22
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Role implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

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
     * 사용이름
     */
    private String appName;

    /**
     * 위단계역할 ID
     */
    private String higherRole;

    /**
     * 위단계역할이름
     */
    private String higherName;

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

    /**
     * 단계역할id
     */
    private String firstLevelId;

    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

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

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getHigherRole() {
        return higherRole;
    }

    public void setHigherRole(String higherRole) {
        this.higherRole = higherRole;
    }

    public String getHigherName() {
        return higherName;
    }

    public void setHigherName(String higherName) {
        this.higherName = higherName;
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

    public String getFirstLevelId() {
        return firstLevelId;
    }

    public void setFirstLevelId(String firstLevelId) {
        this.firstLevelId = firstLevelId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}