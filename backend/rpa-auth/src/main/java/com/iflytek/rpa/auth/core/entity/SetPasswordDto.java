package com.iflytek.rpa.auth.core.entity;

import com.iflytek.rpa.auth.sp.uap.annotation.Password;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 비밀번호요청  DTO(사용회원가입후비밀번호)
 */
@Data
public class SetPasswordDto {

    /**
     * 시인증(회원가입후반환의)
     */
    @NotBlank(message = "시인증비워 둘 수 없습니다")
    private String tempToken;

    /**
     * 새비밀번호
     */
    @NotBlank(message = "비밀번호는 비워 둘 수 없습니다")
    @Password
    private String password;

    /**
     * 비밀번호
     */
    @NotBlank(message = "비밀번호는 비워 둘 수 없습니다")
    private String confirmPassword;

    /**
     * 선택의테넌트ID(가능선택, 결과가사용자있음일개테넌트가능으로선택)
     */
    private String tenantId;
}