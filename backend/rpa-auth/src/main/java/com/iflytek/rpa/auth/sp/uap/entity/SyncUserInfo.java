package com.iflytek.rpa.auth.sp.uap.entity;

import lombok.Data;

/**
 * 사용자 정보 
 */
@Data
public class SyncUserInfo {
    /**
     * 사용자ID
     */
    private String id;

    /**
     * 로그인이름
     */
    private String loginName;

    /**
     * 사용자이름
     */
    private String name;

    /**
     * 휴대폰 번호
     */
    private String phone;

    /**
     * 주소
     */
    private String address;
}