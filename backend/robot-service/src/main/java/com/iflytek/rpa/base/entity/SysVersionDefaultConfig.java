package com.iflytek.rpa.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 버전매칭테이블유형
 * 저장매개버전의매칭
 */
@Data
@TableName("sys_version_default_config")
public class SysVersionDefaultConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 기본 키ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 버전ID, 닫기 sys_product_version.id
     */
    private Long versionId;

    /**
     * 코드(예: designer_count, component_count, executor_count대기)
     */
    private String resourceCode;

    /**
     * 유형: 1-Quota(매칭금액), 2-Switch(열기닫기)
     */
    private Integer resourceType;

    /**
     * 단계코드(사용단계닫기시스템)
     */
    private String parentCode;

    /**
     * 값(Quota예수, Switch예0또는1)
     */
    private Integer defaultValue;

    /**
     * URL경로방식(JSON배열형식, 예: ["/api/v1/design/**"])
     */
    private String urlPatterns;

    /**
     * 설명
     */
    private String description;

    /**
     * 삭제식별자: 0-삭제되지 않음, 1-삭제됨
     */
    private Integer deleted;

    /**
     * 생성 시간
     */
    private Date createTime;

    /**
     * 수정 시간
     */
    private Date updateTime;
}