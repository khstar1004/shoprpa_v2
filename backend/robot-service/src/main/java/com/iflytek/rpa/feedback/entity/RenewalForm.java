package com.iflytek.rpa.feedback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 테이블단일유형
 *
 * @author system
 * @since 2024-12-15
 */
@Data
@TableName("renewal_form")
public class RenewalForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 기본 키ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 테이블단일유형 1=버전 2=버전  3~99
     */
    private Integer formType;

    /**
     * 이름
     */
    private String companyName;

    /**
     * 사람휴대폰 번호
     */
    private String mobile;

    /**
     * 시길이
     */
    private String renewalDuration;

    /**
     * 상태 0=대기관리 1=완료관리 2=완료
     */
    private Integer status;

    /**
     * 서비스비고
     */
    private String remark;

    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;

    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedAt;
}