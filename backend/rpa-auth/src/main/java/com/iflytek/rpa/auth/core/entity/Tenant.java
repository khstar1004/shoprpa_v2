package com.iflytek.rpa.auth.core.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 테넌트객체
 *
 * @author wanchen2
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Tenant implements Serializable {

    private static final long serialVersionUID = 2231322704057975086L;

    /**
     * 기본 키ID
     */
    private String id;

    /**
     * 테넌트이름
     */
    private String name;

    /**
     * 테넌트코드
     */
    private String tenantCode;

    /**
     * 테넌트상태 {0중지사용 1사용}
     */
    private Integer status;

    /**
     * 비고
     */
    private String remark;

    /**
     * 생성사람
     */
    private String creator;

    /**
     * 삭제 여부
     */
    private Integer isDelete;

    /**
     * 여부테넌트
     */
    private Boolean isDefaultTenant;

    /**
     * 테넌트유형
     */
    private String tenantType;

    /*
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