package com.iflytek.rpa.common.feign.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.Date;

/**
 * @desc: 조직유형
 * @author: weilai <laiwei3@iflytek.com>
 * @create: 2025/11/24 17:39
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Org implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * 기기id
     */
    private String id;

    /**
     * 기기이름
     */
    private String name;

    /**
     * 기기코드
     */
    private String code;

    /**
     * 이름
     */
    private String province;

    /**
     * 코드
     */
    private String provinceCode;

    /**
     * 이름
     */
    private String city;

    /**
     * 코드
     */
    private String cityCode;

    /**
     * 이름
     */
    private String district;

    /**
     * 코드
     */
    private String districtCode;

    /**
     * 기기명칭
     */
    private String shortName;

    /**
     * 기기유형
     */
    private String orgType;

    /**
     * 기기유형이름
     */
    private String orgTypeName;

    /**
     * 기기유형코드
     */
    private String orgTypeCode;

    /**
     * 위단계기기id
     */
    private String higherOrg;

    /**
     * 위단계기기이름
     */
    private String higherName;

    /**
     * 기기상태
     */
    private Integer status = 1;

    /**
     * 정렬
     */
    private Integer sort = 1;

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

    /**
     * 기기단계
     */
    private Integer level;

    /**
     * 단계코드
     */
    private String levelCode;

    /**
     * 여부삭제
     */
    private Integer isDelete;

    /**
     * 단계기기id
     */
    private String firstLevelId;

    /**
     * 필드
     */
    private String extInfo;

    /**
     * 삼방법필드
     */
    private String thirdExtInfo;

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

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getDistrictCode() {
        return districtCode;
    }

    public void setDistrictCode(String districtCode) {
        this.districtCode = districtCode;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getOrgType() {
        return orgType;
    }

    public void setOrgType(String orgType) {
        this.orgType = orgType;
    }

    public String getOrgTypeName() {
        return orgTypeName;
    }

    public void setOrgTypeName(String orgTypeName) {
        this.orgTypeName = orgTypeName;
    }

    public String getOrgTypeCode() {
        return orgTypeCode;
    }

    public void setOrgTypeCode(String orgTypeCode) {
        this.orgTypeCode = orgTypeCode;
    }

    public String getHigherOrg() {
        return higherOrg;
    }

    public void setHigherOrg(String higherOrg) {
        this.higherOrg = higherOrg;
    }

    public String getHigherName() {
        return higherName;
    }

    public void setHigherName(String higherName) {
        this.higherName = higherName;
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

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getLevelCode() {
        return levelCode;
    }

    public void setLevelCode(String levelCode) {
        this.levelCode = levelCode;
    }

    public Integer getIsDelete() {
        return isDelete;
    }

    public void setIsDelete(Integer isDelete) {
        this.isDelete = isDelete;
    }

    public String getFirstLevelId() {
        return firstLevelId;
    }

    public void setFirstLevelId(String firstLevelId) {
        this.firstLevelId = firstLevelId;
    }

    public String getExtInfo() {
        return extInfo;
    }

    public void setExtInfo(String extInfo) {
        this.extInfo = extInfo;
    }

    public String getThirdExtInfo() {
        return thirdExtInfo;
    }

    public void setThirdExtInfo(String thirdExtInfo) {
        this.thirdExtInfo = thirdExtInfo;
    }
}