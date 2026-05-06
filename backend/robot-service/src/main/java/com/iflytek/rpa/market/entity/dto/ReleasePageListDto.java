package com.iflytek.rpa.market.entity.dto;

import lombok.Data;

@Data
public class ReleasePageListDto {
    /**
     * 봇이름
     */
    private String robotName;

    /**
     * 모든id
     */
    private String creatorId;

    /**
     * 검토상태
     */
    private String status;

    /**
     * 테넌트id
     */
    private String tenantId;

    /**
     * 코드
     */
    private Integer pageNo;

    /**
     * 매크기
     */
    private Integer pageSize;
}