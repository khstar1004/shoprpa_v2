package com.iflytek.rpa.auth.core.entity;

import lombok.Data;

/**
 * 테넌트까지정보조회반환DTO
 *
 * @author system
 */
@Data
public class TenantExpirationDto {

    /**
     * 테넌트ID
     */
    private String tenantId;

    /**
     * 테넌트유형(personal-개사람버전, professional-버전, enterprise_purchased-버전, enterprise_subscription-버전)
     */
    private String tenantType;

    /**
     * 까지시간(형식: YYYY-MM-DD)
     * 개사람버전및버전반환null(아니요제한)
     */
    private String expirationDate;

    /**
     * 데이터
     * 개사람버전및버전반환null(아니요제한)
     * 완료까지반환데이터
     */
    private Long remainingDays;

    /**
     * 여부까지
     * 개사람버전및버전반환false(아니요제한)
     */
    private Boolean isExpired;

    /**
     * 여부안내까지(까지전N필요)
     * 개사람버전및버전반환false(아니요제한)
     */
    private Boolean shouldAlert;
}