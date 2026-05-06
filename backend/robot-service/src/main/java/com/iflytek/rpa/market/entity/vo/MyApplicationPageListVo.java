package com.iflytek.rpa.market.entity.vo;

import lombok.Data;

/**
 * 사용신청페이지목록VO
 */
@Data
public class MyApplicationPageListVo {
    /**
     * 사용검토ID
     */
    private String id;

    /**
     * 봇id
     */
    private String robotId;
    /**
     * 봇버전
     */
    private String robotVersion;
    /**
     * 봇이름
     */
    private String robotName;

    /**
     * 비밀단계식별자
     */
    private String securityLevel;

    /**
     * 제출검토시간
     */
    private String submitAuditTime;

    /**
     * 검토
     */
    private String auditOpinion;
    /**
     * 신청유형: release(위신청)/use(사용신청)
     */
    private String applicationType;
    /**
     * 신청상태: pending(대기검토)/approved(완료통신경과)/rejected(통과하지 못했습니다)/canceled(완료판매)
     */
    private String status;
}