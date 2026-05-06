package com.iflytek.rpa.market.entity.dto;

import lombok.Data;

@Data
public class MyApplicationPageListDto {
    /**
     * 봇이름
     */
    private String robotName;

    /**
     * 신청유형: release(위신청)/use(사용신청)
     */
    private String applicationType;

    /**
     * 신청상태: pending(대기검토)/approved(완료통신경과)/rejected(통과하지 못했습니다)/canceled(완료판매)
     */
    private String status;

    /**
     * 테넌트id
     */
    private String tenantId;

    /**
     * 사용자id
     */
    private String userId;

    /**
     * 코드
     */
    private Integer pageNo;

    /**
     * 매크기
     */
    private Integer pageSize;
}