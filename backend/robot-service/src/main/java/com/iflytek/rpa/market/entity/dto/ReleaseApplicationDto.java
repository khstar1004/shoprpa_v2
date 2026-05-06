package com.iflytek.rpa.market.entity.dto;

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 클라이언트위신청DTO
 */
@Data
public class ReleaseApplicationDto {

    /**
     * robotId, 비워 둘 수 없습니다
     */
    @NotBlank(message = "봇ID비워 둘 수 없습니다")
    private String robotId;

    /**
     * robotVersion, 비워 둘 수 없습니다
     */
    @NotNull(message = "봇버전비워 둘 수 없습니다")
    private Integer robotVersion;

    /**
     * 목록 마켓ID목록, 비워 둘 수 없습니다
     */
    @NotNull(message = "마켓id비워 둘 수 없습니다")
    private List<String> marketIdList;

    /**
     * 권한식별자
     */
    private Integer editFlag;

    /**
     * 분유형
     */
    private String category;

    private String appName;
}