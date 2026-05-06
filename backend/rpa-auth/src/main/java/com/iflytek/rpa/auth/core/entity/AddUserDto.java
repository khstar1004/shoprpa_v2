package com.iflytek.rpa.auth.core.entity;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddUserDto {
    /**
     * 휴대폰 번호()
     */
    @NotBlank
    private String phone;
    /**
     * 이름()
     */
    @NotBlank
    private String name;
    /**
     * 모듈Id()
     */
    @NotBlank
    private String orgId;
    /**
     * 역할Id 로 [지정되지 않았습니다]
     */
    private String roleId = "1";

    private String loginName;

    private String tenantId;

    private String password;

    private String confirmPassword;
}