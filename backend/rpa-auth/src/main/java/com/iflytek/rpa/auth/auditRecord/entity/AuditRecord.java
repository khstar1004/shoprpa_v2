package com.iflytek.rpa.auth.auditRecord.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import lombok.Data;

/**
 * 계획로그테이블
 * @author jqfang3
 * @since 2025-08-04
 */
@Data
@TableName("audit_record")
public class AuditRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 기본 키ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 파일모듈코드
     */
    private Integer eventModuleCode;

    /**
     * 파일모듈
     */
    private String eventModuleName;

    /**
     * 파일코드
     */
    private Integer eventTypeCode;

    /**
     * 파일유형
     */
    private String eventTypeName;

    /**
     * 파일
     */
    private String eventDetail;

    /**
     * 생성자id
     */
    private String creatorId;

    /**
     * 테넌트id
     */
    private String tenantId;

    /**
     * 생성자이름
     */
    private String creatorName;

    /**
     * 생성 시간
     */
    private Date createTime;

    /**
     * 역할이름목록(데이터베이스필드)
     */
    @TableField(exist = false)
    private List<String> roleNames;

    /**
     * 사용자ID(데이터베이스필드)
     */
    @TableField(exist = false)
    private String userId;
}