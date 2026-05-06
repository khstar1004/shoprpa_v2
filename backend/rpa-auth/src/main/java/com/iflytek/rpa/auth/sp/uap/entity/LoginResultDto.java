package com.iflytek.rpa.auth.sp.uap.entity;

import lombok.Data;

/**
 * 로그인결과DTO
 * @author lihang
 * @date 2025-11-25
 */
@Data
public class LoginResultDto {
    private String ticket;
    private String tgt;
    private String redirectUrl;
    private String tenantId;
}