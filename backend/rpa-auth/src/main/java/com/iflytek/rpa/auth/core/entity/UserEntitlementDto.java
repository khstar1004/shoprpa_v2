package com.iflytek.rpa.auth.core.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * 사용자권한DTO
 *
 * @author system
 */
@Data
public class UserEntitlementDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 계획기기권한
     */
    private Boolean moduleDesigner;

    /**
     * 실행기기권한
     */
    private Boolean moduleExecutor;

    /**
     * 제어권한
     */
    private Boolean moduleConsole;

    /**
     * 팀마켓권한
     */
    private Boolean moduleMarket;
}