package com.iflytek.rpa.conf.entity.vo;

import lombok.Data;

/**
 * 사용자회원가입반환VO
 */
@Data
public class UserRegisterVo {
    /**
     * 계정(휴대폰 번호)
     */
    private String account;

    /**
     * 비밀번호
     */
    private String password;

    private String userId;

    private String url;
}