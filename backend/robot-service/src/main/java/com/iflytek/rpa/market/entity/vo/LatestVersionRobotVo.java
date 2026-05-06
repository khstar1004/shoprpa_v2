package com.iflytek.rpa.market.entity.vo;

import lombok.Data;

@Data
public class LatestVersionRobotVo {
    /**
     * 봇id
     */
    String robotId;
    /**
     * 새버전
     */
    Integer latestVersion;
    /**
     * 위신청상태 pending:신청중 approved:신청완료통신경과 none:없음신청기록 null:위검토미완료열기시작
     */
    String applicationStatus;
}