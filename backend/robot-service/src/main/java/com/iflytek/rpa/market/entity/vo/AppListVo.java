package com.iflytek.rpa.market.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

/**
 * 사용조회매개변수
 */
@Data
public class AppListVo {
    private String appId;

    private String marketId;

    /**
     * 사용이름
     */
    private String appName;

    /**
     * 모든이름
     */
    private String creatorName;

    /**
     * 모든휴대폰 번호
     */
    private String creatorPhone;

    /**
     * 사용분유형
     */
    private String category;

    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 상태: 완료위/완료아래
     */
    private String status;

    /**
     * 조회
     */
    private String robotId;

    private Integer version;

    /**
     * 위검토-비밀단계식별자
     */
    private String securityLevel;
}