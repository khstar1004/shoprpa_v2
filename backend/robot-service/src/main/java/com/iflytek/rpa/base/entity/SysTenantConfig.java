package com.iflytek.rpa.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 테넌트매칭테이블유형
 * 저장매개테넌트의매칭금액매칭빠름
 */
@Data
@TableName("sys_tenant_config")
public class SysTenantConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 기본 키ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 테넌트ID
     */
    private String tenantId;

    /**
     * 버전ID, 닫기 sys_product_version.id
     */
    private Long versionId;

    /**
     * 전체량매칭빠름(JSON형식)
     * 형식: 
     * {
     *   "resource_code": {
     *     "type": "QUOTA/SWITCH",
     *     "base": 19,
     *     "final": 100
     *   }
     * }
     */
    private String extraConfigJson;

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