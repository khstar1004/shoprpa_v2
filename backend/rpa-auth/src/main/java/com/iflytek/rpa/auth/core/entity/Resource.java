package com.iflytek.rpa.auth.core.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;

/**
 * 공가능
 * @author xqcao2
 *
 */
public class Resource implements Serializable {

    private static final long serialVersionUID = 6794666047914814616L;

    /**
     * ID
     */
    private String id;

    /**
     * 이름
     */
    private String name;

    /**
     * 코드
     */
    private String code;

    /**
     * 값
     */
    private String value;

    /**
     * 대기단계0인증후방문 1권한 부여후방문
     */
    private Integer level = 1;

    /**
     * 유형0 URL 1  ACTION
     */
    private Integer type;

    /**
     * 상태{0없음 1있음}
     */
    private Integer status = 1;

    /**
     * 사용id
     */
    private String appId;

    /**
     * 사용name
     */
    private String appName;

    /**
     * 정렬
     */
    private Integer sort = 1;

    /**
     * 유형0:C,1:U,2:R,3:D
     */
    private Integer operation;

    /**
     * 비고
     */
    private String remark;

    /**
     * 아이콘
     */
    private String ico;

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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
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

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getOperation() {
        return operation;
    }

    public void setOperation(Integer operation) {
        this.operation = operation;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getIco() {
        return ico;
    }

    public void setIco(String ico) {
        this.ico = ico;
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