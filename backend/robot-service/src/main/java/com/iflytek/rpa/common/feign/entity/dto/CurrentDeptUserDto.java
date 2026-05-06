package com.iflytek.rpa.common.feign.entity.dto;

import lombok.Data;

/**
 * @author mjren
 * @date 2025-03-17 16:17
 * @copyright Copyright (c) 2025 mjren
 */
@Data
public class CurrentDeptUserDto {
    /**
     * 사람원또는모듈id
     */
    private String id;

    /**
     * 사람원또는모듈이름
     */
    private String name;

    /**
     * 여부
     */
    private Boolean status;

    /**
     * 유형, user:사람원, dept:모듈
     */
    private String type;

    /**
     * 역할이름
     */
    private String roleName;
}