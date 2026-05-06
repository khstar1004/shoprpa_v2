package com.iflytek.rpa.auth.core.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 사용자권한유형
 *
 * @author system
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserEntitlement implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 기본 키ID
     */
    private String id;

    /**
     * 사용자ID
     */
    private String userId;

    /**
     * 테넌트ID
     */
    private String tenantId;

    /**
     * 계획기기권한(0-권한이 없습니다, 1-있음권한)
     */
    private Integer moduleDesigner;

    /**
     * 실행기기권한(0-권한이 없습니다, 1-있음권한)
     */
    private Integer moduleExecutor;

    /**
     * 제어권한(0-권한이 없습니다, 1-있음권한)
     */
    private Integer moduleConsole;

    /**
     * 팀마켓권한(0-권한이 없습니다, 1-있음권한, 1)
     */
    private Integer moduleMarket;

    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 삭제 여부(0-아니요, 1-예)
     */
    private Integer isDelete;
}