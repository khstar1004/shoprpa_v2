package com.iflytek.rpa.auth.core.entity;

import com.iflytek.rpa.auth.sp.uap.annotation.Password;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 관리관리단말업데이트사용자비밀번호요청 
 */
@Data
public class UpdateUserPasswordDto {

    /**
     * 사용자명(로그인이름)
     */
    @NotBlank(message = "사용자명은 비워 둘 수 없습니다")
    private String loginName;

    /**
     * 비밀번호
     */
    @NotBlank(message = "비밀번호는 비워 둘 수 없습니다")
    private String oldPassword;

    /**
     * 새비밀번호
     */
    @NotBlank(message = "새비밀번호는 비워 둘 수 없습니다")
    @Password
    private String newPassword;
}