package com.iflytek.rpa.component.entity.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 조회컴포넌트사용DTO
 *
 * @author makejava
 * @since 2024-12-19
 */
@Data
public class GetComponentUseDto {

    /**
     * 봇ID
     */
    @NotBlank(message = "봇ID비워 둘 수 없습니다")
    private String robotId;

    /**
     * 실행위치
     */
    @NotBlank(message = "실행위치비워 둘 수 없습니다")
    private String mode;

    /**
     * 봇버전(허용비어 있습니다)
     */
    private Integer version;

    /**
     * 스케줄링방식 의 version
     */
    private Integer robotVersion;
}