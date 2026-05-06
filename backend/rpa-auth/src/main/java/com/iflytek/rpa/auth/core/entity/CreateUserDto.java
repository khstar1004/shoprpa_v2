package com.iflytek.rpa.auth.core.entity;

import java.util.Date;

/**
 * 생성사용자DTO
 * @author xqcao2
 *
 */
public class CreateUserDto {

    /**
     * 사용자이름
     */
    private String name;

    /**
     * 사용자명  
     */
    private String loginName;

    /**
     * 사용자유형
     *  SUPER_ADMIN("초과단계관리관리원", 1),
     *  SYSTEM_ADMIN("평면관리관리원", 2),
     *  NORMAL_USER("통신사용자", -1),
     *  RESOURCE_POOL_USER("사용자", 3),
     *  TENANT_SUPER_ADMIN("테넌트초과단계관리관리원", 0);
     */
    private Integer userType = -1;

    /**
     * 사용자{1시스템추가,2계정}
     */
    private Integer userSource = 1;

    /**
     *   일
     */
    private String phone;

    /**
     * 주소
     */
    private String address;

    /**
     * 메일함  일
     */
    private String email;

    /**
     * 사용자상태{0중지사용 1사용}
     */
    private Integer status = 1;

    /**
     * 기기id  검증기기여부저장에서
     */
    private String orgId;

    /**
     * 비고
     */
    private String remark;

    /**
     * 일
     */
    private Date birthday;

    /**
     * 인증
     */
    private String idNumber;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public Integer getUserType() {
        return userType;
    }

    public void setUserType(Integer userType) {
        this.userType = userType;
    }

    public Integer getUserSource() {
        return userSource;
    }

    public void setUserSource(Integer userSource) {
        this.userSource = userSource;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }
}