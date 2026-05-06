package com.iflytek.rpa.market.entity.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PreReleaseCheckDto {
    /**
     * 봇ID()
     */
    @NotBlank(message = "봇ID비워 둘 수 없습니다")
    private String robotId;

    /**
     * 봇버전()
     */
    @NotNull(message = "봇버전비워 둘 수 없습니다")
    private Integer version;
}