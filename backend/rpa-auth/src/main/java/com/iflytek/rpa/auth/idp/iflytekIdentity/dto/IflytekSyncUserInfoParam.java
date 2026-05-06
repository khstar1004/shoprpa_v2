package com.iflytek.rpa.auth.idp.iflytekIdentity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 정보 - 요청 매개변수
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IflytekSyncUserInfoParam {
    private String userid;
    private String password;
    private IflytekSyncUserInfoLogin login;
    private IflytekSyncUserInfoUserInfo userinfo;
}