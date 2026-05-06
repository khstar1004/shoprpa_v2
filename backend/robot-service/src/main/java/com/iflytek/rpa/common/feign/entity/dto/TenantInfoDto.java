package com.iflytek.rpa.common.feign.entity.dto;

import lombok.Data;

/**
 * @author mjren
 * @date 2025-03-19 10:28
 * @copyright Copyright (c) 2025 mjren
 */
@Data
public class TenantInfoDto {
    /**
     * 테넌트id
     */
    private String id;

    /**
     * 테넌트이름
     */
    private String name;

    /**
     * 테넌트코드
     */
    private String code;

    /**
     * 관리관리원id
     */
    private String managerId;

    /**
     * 관리관리원이름
     */
    private String managerName;
}