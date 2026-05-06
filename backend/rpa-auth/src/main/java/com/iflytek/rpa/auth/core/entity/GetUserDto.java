package com.iflytek.rpa.auth.core.entity;

/**
 * 조회사용자정보 DTO
 * @author xqcao2
 *
 */
public class GetUserDto {

    /***
     * 요청 일개가능 개시 예& 닫기시스템
     */

    /**
     * 사용자ID (remark: 요청 일개가능 개시 요청확인예의, 아니요이면조회아니요까지)
     */
    private String userId;

    /**
     * 로그인이름 (remark: 요청 일개가능 개시 요청확인예의, 아니요이면조회아니요까지)
     */
    private String loginName;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }
}