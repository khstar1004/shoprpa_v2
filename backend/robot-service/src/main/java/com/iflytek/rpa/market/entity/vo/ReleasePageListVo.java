package com.iflytek.rpa.market.entity.vo;

import lombok.Data;

/**
 * 게시페이지목록VO
 */
@Data
public class ReleasePageListVo {
    /**
     * 위검토ID
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
     * 모든id
     */
    private String creatorId;
    /**
     * 모든이름
     */
    private String creatorName;

    /**
     * 모든휴대폰 번호
     */
    private String creatorPhone;

    /**
     * 제출검토시간
     */
    private String submitAuditTime;

    /**
     * 검토상태
     */
    private String status;

    /**
     * 비밀단계식별자
     */
    private String securityLevel;
    /**
     * 허용사용의모듈ID목록
     */
    private String allowedDept;
}