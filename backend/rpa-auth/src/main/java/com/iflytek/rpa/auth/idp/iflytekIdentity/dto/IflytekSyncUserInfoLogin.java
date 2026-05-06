package com.iflytek.rpa.auth.idp.iflytekIdentity.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 정보 - 로그인정보
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IflytekSyncUserInfoLogin {
    private List<IflytekSyncUserInfoAccount> account;
}