package com.iflytek.rpa.auth.core.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.Date;

/**
 * @desc: TODO
 * @author: weilai <laiwei3@iflytek.com>
 * @create: 2025/12/4 10:17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UapExtand extends TenantModeExtendDto implements Serializable {

    private static final long serialVersionUID = -542509006960020555L;

    /**
     * 속성ID
     */
    private String id;

    /**
     * 속성이름
     */
    private String name;

    /**
     * 속성코드
     */
    private String nameCode;

    /**
     * 속성코드
     */
    private String code;

    /**
     * 데이터 유형0테이블String,1테이블int,2테이블datetime
     */
    private Integer type;

    /**
     * 속성유형0테이블사용자 1테이블기기
     */
    private Integer catalog;

    /**
     * 여부{0아니요 1예}
     */
    private Integer mandatory;

    /**
     * 컴포넌트유형
     */
    private String componentType;

    /**
     * 데이터
     */
    private String componentData;

    /**
     * 일(0아니요 1예)
     */
    private Integer isUnique = 0;

    /**속성상태{0없음 1있음}
     *
     */
    private Integer status = 1;

    /**
     * 정렬
     */
    private Integer sort;

    /**
     * 비고
     */
    private String remark;

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

    public String getNameCode() {
        return nameCode;
    }

    public void setNameCode(String nameCode) {
        this.nameCode = nameCode;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getCatalog() {
        return catalog;
    }

    public void setCatalog(Integer catalog) {
        this.catalog = catalog;
    }

    public Integer getMandatory() {
        return mandatory;
    }

    public void setMandatory(Integer mandatory) {
        this.mandatory = mandatory;
    }

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public String getComponentData() {
        return componentData;
    }

    public void setComponentData(String componentData) {
        this.componentData = componentData;
    }

    public Integer getIsUnique() {
        return isUnique;
    }

    public void setIsUnique(Integer isUnique) {
        this.isUnique = isUnique;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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