package com.iflytek.rpa.auth.core.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.Date;

/**
 * @desc: 사용자유형
 * @author: weilai <laiwei3@iflytek.com>
 * @create: 2025/11/24 17:18
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class User implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -8814389360599851366L;

    /**
     * 기본 키
     */
    private String id;

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
    private Integer userType;

    /**
     * 사용자유형  딕셔너리값
     */
    private String userTypeText;

    /**
     * 사용자
     */
    private Integer userSource = 1;

    /**
     * 기기
     */
    private String phone;

    /**
     * 주소
     */
    private String address;

    /**
     * 메일함
     */
    private String email;

    /**
     * 상태 사용자상태{0중지사용 1사용}
     */
    private Integer status;

    /**
     * 기기id
     */
    private String orgId;

    /**
     * 기기코드
     */
    private String orgCode;

    /**
     * 기기이름
     */
    private String orgName;

    /**
     * 비고
     */
    private String remark;

    /**
     * 일
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date birthday;

    // 생성 시간
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 업데이트날짜
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 비밀번호업데이트날짜
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date pwdUpdateTime;

    /**
     * 인증
     */
    private String idNumber;

    /**
     * 삭제필드
     */
    private Integer isDelete;

    /**
     * 정보
     */
    private String extInfo;

    /**
     * 삼방법필드
     */
    private String thirdExtInfo;

    /**
     * 사용자이미지(BASE64암호화문자열)
     */
    private String profile;

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

    public String getUserTypeText() {
        return userTypeText;
    }

    public void setUserTypeText(String userTypeText) {
        this.userTypeText = userTypeText;
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

    public String getOrgCode() {
        return orgCode;
    }

    public void setOrgCode(String orgCode) {
        this.orgCode = orgCode;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
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

    public Date getPwdUpdateTime() {
        return pwdUpdateTime;
    }

    public void setPwdUpdateTime(Date pwdUpdateTime) {
        this.pwdUpdateTime = pwdUpdateTime;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public Integer getIsDelete() {
        return isDelete;
    }

    public void setIsDelete(Integer isDelete) {
        this.isDelete = isDelete;
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

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}