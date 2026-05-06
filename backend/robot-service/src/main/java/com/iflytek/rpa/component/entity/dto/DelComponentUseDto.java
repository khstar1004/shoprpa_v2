package com.iflytek.rpa.component.entity.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 추가컴포넌트사용DTO
 *
 * @author makejava
 * @since 2024-12-19
 */
@Data
public class DelComponentUseDto {

    /**
     * 실행위치
     */
    @NotBlank(message = "실행위치비워 둘 수 없습니다")
    private String mode;

    /**
     * 봇ID
     */
    @NotBlank(message = "봇ID비워 둘 수 없습니다")
    private String robotId;

    /**
     * 봇버전
     */
    private Integer robotVersion;

    /**
     * 컴포넌트ID
     */
    @NotBlank(message = "컴포넌트ID비워 둘 수 없습니다")
    private String componentId;
}