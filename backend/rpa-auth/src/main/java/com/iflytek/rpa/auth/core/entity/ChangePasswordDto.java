package com.iflytek.rpa.auth.core.entity;

import com.iflytek.rpa.auth.sp.uap.annotation.Password;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 수정비밀번호요청  DTO
 */
@Data
public class ChangePasswordDto {

    /**
     * 계정(로그인이름)
     */
    //    @NotBlank(message = "계정비워 둘 수 없습니다")
    private String loginName;

    /**
     * 로그인휴대폰 번호
     */
    //    @NotBlank(message = "휴대폰 번호는 비워 둘 수 없습니다")
    private String phone;

    /**
     * 기존비밀번호
     */
    @NotBlank(message = "기존비밀번호는 비워 둘 수 없습니다")
    private String oldPassword;

    /**
     * 새비밀번호
     */
    @NotBlank(message = "새비밀번호는 비워 둘 수 없습니다")
    @Password
    private String newPassword;

    /**
     * 비밀번호
     */
    @NotBlank(message = "비밀번호는 비워 둘 수 없습니다")
    private String confirmPassword;

    /**
     * 인증비밀번호여부일
     */
    @AssertTrue(message = "입력한 비밀번호가 올바르지 않습니다")
    public boolean isPasswordMatch() {
        if (newPassword == null || confirmPassword == null) {
            return true; //  @NotBlank 관리빈값검증
        }
        return newPassword.equals(confirmPassword);
    }
}