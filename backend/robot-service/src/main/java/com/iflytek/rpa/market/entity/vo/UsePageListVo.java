package com.iflytek.rpa.market.entity.vo;

import lombok.Data;

/**
 * 사용신청페이지목록VO
 */
@Data
public class UsePageListVo {
    /**
     * 사용검토ID
     */
    private String id;
    /**
     * 봇id
     */
    private String robotId;
    /**
     * 봇이름
     */
    private String robotName;
    /**
     * 버전
     */
    private String robotVersion;

    /**
     * 비밀단계식별자
     */
    private String securityLevel;

    /**
     * 신청사람id
     */
    private String creatorId;

    /**
     * 신청사람이름
     */
    private String creatorName;
    /**
     * 신청사람휴대폰 번호
     */
    private String creatorPhone;
    /**
     * 신청사람모듈이름
     */
    private String creatorDeptName;
    /**
     * 신청사람모듈id
     */
    private String creatorDeptId;

    /**
     * 제출검토시간
     */
    private String submitAuditTime;

    /**
     * 검토상태
     */
    private String status;
}