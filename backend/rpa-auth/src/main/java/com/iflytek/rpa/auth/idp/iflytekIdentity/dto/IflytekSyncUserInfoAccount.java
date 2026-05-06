package com.iflytek.rpa.auth.idp.iflytekIdentity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 정보 - 로그인계정정보
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IflytekSyncUserInfoAccount {
    private String loginid;
    private String ccode;
    private Integer lgtype;
}