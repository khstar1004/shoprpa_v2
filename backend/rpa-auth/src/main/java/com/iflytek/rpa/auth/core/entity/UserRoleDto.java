package com.iflytek.rpa.auth.core.entity;

import lombok.Data;

/**
 * 사용자역할정보DTO
 * @author system
 * @date 2025-01-27
 */
@Data
public class UserRoleDto {
    /**
     * 사용자ID
     */
    private String userId;

    /**
     * 역할이름
     */
    private String roleName;
}