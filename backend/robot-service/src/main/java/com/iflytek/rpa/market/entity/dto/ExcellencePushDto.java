package com.iflytek.rpa.market.entity.dto;

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 중버전DTO
 */
@Data
public class ExcellencePushDto {

    @NotBlank(message = "봇ID비워 둘 수 없습니다")
    private String robotId;

    @NotNull(message = "봇버전비워 둘 수 없습니다")
    private Integer robotVersion;

    /**
     * 목록 테넌트ID목록
     */
    @NotEmpty(message = "목록사용자목록은 비워 둘 수 없습니다")
    private List<String> userIdList;

    /**
     * : auto(), manual()
     */
    private String pushStrategy;

    /**
     * 비밀단계제어: 여부기호합치기비밀단계필요의테넌트
     */
    private Boolean securityControl;

    /**
     * 설명
     */
    private String pushDescription;
}