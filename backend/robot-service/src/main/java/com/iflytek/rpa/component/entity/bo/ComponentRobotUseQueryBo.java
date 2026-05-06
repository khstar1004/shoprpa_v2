package com.iflytek.rpa.component.entity.bo;

import lombok.Data;

/**
 * 컴포넌트봇사용조회BO
 *
 * @author makejava
 * @since 2024-12-19
 */
@Data
public class ComponentRobotUseQueryBo {

    /**
     * 봇ID
     */
    private String robotId;

    /**
     * 봇버전
     */
    private Integer robotVersion;

    /**
     * 컴포넌트ID
     */
    private String componentId;

    /**
     * 컴포넌트버전
     */
    private Integer componentVersion;

    /**
     * 테넌트ID
     */
    private String tenantId;
}