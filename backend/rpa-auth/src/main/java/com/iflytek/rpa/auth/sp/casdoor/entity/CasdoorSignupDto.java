package com.iflytek.rpa.auth.sp.casdoor.entity;

import lombok.Data;

/**
 * Casdoor 회원가입요청 매개변수
 */
@Data
public class CasdoorSignupDto {
    private String application;
    private String organization;
    private String username;
    private String password;
    private String name;
    private String phone;
    private String countryCode;
}