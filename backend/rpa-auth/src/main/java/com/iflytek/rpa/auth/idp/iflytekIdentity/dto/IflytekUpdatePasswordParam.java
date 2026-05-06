package com.iflytek.rpa.auth.idp.iflytekIdentity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shoprpa계정수정비밀번호매개변수
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IflytekUpdatePasswordParam {
    private String id;
    private String ccode;
    private String type;
    private String opwd;
    private String npwd;
    private String pwdtype;
}