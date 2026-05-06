package com.iflytek.rpa.auth.core.entity;

/**
 * 생성기기DTO
 * @author xqcao2
 *
 */
public class CreateOrgDto {

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
     * 위단계기기id
     */
    private String higherOrg;

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

    public String getHigherOrg() {
        return higherOrg;
    }

    public void setHigherOrg(String higherOrg) {
        this.higherOrg = higherOrg;
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
}