package com.iflytek.rpa.auth.idp.iflytekIdentity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shoprpa계정삭제사용자매개변수
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IflytekDeleteUserParam {
    private String userid;
}