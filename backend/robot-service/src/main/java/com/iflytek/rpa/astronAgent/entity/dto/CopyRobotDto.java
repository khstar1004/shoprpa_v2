package com.iflytek.rpa.astronAgent.entity.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 복사봇요청 DTO
 */
@Data
public class CopyRobotDto {

    /**
     * 봇id(appId)
     */
    @NotBlank(message = "봇id비워 둘 수 없습니다")
    private String robotId;

    /**
     * 봇버전
     */
    @NotNull(message = "봇버전비워 둘 수 없습니다")
    private Integer version;

    /**
     * 목록사용자휴대폰 번호
     */
    @NotBlank(message = "목록사용자휴대폰 번호는 비워 둘 수 없습니다")
    private String targetPhone;
}