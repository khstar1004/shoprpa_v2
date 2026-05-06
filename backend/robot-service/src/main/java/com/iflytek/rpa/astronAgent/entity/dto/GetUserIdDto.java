package com.iflytek.rpa.astronAgent.entity.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 통신경과휴대폰 번호가져오기사용자ID요청 DTO
 */
@Data
public class GetUserIdDto {

    /**
     * 휴대폰 번호
     */
    @NotBlank(message = "휴대폰 번호는 비워 둘 수 없습니다")
    private String phone;
}