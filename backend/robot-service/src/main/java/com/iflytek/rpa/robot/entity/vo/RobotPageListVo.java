package com.iflytek.rpa.robot.entity.vo;

import lombok.Data;

@Data
public class RobotPageListVo {
    /**
     * 봇id(다중선택사용)
     */
    String robotId;
    /**
     *
     */
    String robotName;
    /**
     * 생성 시간
     */
    String createTime;
    /**
     * 수정 시간
     */
    String latestReleaseTime;

    /**
     * 비밀단계식별자(비밀단계red/green/yellow)
     */
    String securityLevel;

    /**
     * web유형
     */
    String type;
    /**
     * 모든id
     */
    String creatorId;
    /**
     * 모든이름
     */
    String creatorName;
    /**
     * 모든휴대폰 번호
     */
    String creatorPhone;
    /**
     * 모듈
     */
    String deptName;
    /**
     * 모듈경로
     */
    String deptIdPath;

    Integer appVersion;

    Integer version;

    String tenantId;
}