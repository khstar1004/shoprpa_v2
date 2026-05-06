package com.iflytek.rpa.component.entity.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GetRobotBlockDto {
    /**
     * 봇id
     */
    @NotBlank(message = "봇ID비워 둘 수 없습니다")
    private String robotId;

    /**
     * 발송요청 의위치
     */
    @NotBlank(message = "mode비워 둘 수 없습니다")
    private String mode;

    /**
     * 봇버전
     */
    private Integer robotVersion;
}