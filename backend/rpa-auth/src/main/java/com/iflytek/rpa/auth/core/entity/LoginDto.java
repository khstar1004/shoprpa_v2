package com.iflytek.rpa.auth.core.entity;

import com.iflytek.rpa.auth.core.entity.enums.LoginTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginDto {

    /**
     * 휴대폰 번호
     */
    private String phone;

    /**
     * 로그인이름
     */
    private String loginName;

    /**
     * 비밀번호
     */
    private String password;

    /**
     * 인증 코드
     */
    private String captcha;

    /**
     * 로그인유형
     */
    private LoginTypeEnum loginType;

    /**
     * 테넌트ID
     */
    private String tenantId;

    /**
     * 로그인평면(client: 클라이언트, admin: 실행운영후, invite: 초대연결)
     */
    private String platform;

    /**
     * 인증 코드(login/register/set_password), 사용검증사용
     */
    private String scene;
}