package com.iflytek.rpa.common.feign.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.Date;

/**
 * 테넌트객체
 *
 * @author wanchen2
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tenant implements Serializable {

    private static final long serialVersionUID = 2231322704057975086L;

    /**
     * 기본 키ID
     */
    private String id;

    /**
     * 테넌트이름
     */
    private String name;

    /**
     * 테넌트코드
     */
    private String tenantCode;

    /**
     * 테넌트상태 {0중지사용 1사용}
     */
    private Integer status;

    /**
     * 비고
     */
    private String remark;

    /**
     * 생성사람
     */
    private String creator;

    /**
     * 삭제 여부
     */
    private Integer isDelete;

    /**
     * 여부테넌트
     */
    private Boolean isDefaultTenant;

    /*
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

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Integer getIsDelete() {
        return isDelete;
    }

    public void setIsDelete(Integer isDelete) {
        this.isDelete = isDelete;
    }

    public Boolean getIsDefaultTenant() {
        return isDefaultTenant;
    }

    public void setIsDefaultTenant(Boolean isDefaultTenant) {
        this.isDefaultTenant = isDefaultTenant;
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