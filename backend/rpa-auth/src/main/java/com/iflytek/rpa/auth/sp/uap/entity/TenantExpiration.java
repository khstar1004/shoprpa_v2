package com.iflytek.rpa.auth.sp.uap.entity;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 테넌트까지정보유형
 *
 * @author system
 */
@Data
public class TenantExpiration implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 기본 키ID
     */
    private String id;

    /**
     * 테넌트ID
     */
    private String tenantId;

    /**
     * 까지시간(형식: YYYY-MM-DD)
     * 버전, 필드저장의예암호화후의데이터
     * 버전, 필드저장의예문서데이터
     */
    private String expirationDate;

    /**
     * 생성 시간
     */
    private Date createTime;

    /**
     * 수정 시간
     */
    private Date updateTime;

    /**
     * 삭제 여부(0-아니요, 1-예)
     */
    private Integer isDelete;
}