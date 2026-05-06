package com.iflytek.rpa.component.entity.bo;

import lombok.Data;

/**
 * 컴포넌트봇사용업데이트BO
 *
 * @author makejava
 * @since 2024-12-19
 */
@Data
public class ComponentRobotUseUpdateBo {

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
    private Integer oldComponentVersion;

    /**
     * 새컴포넌트버전
     */
    private Integer newComponentVersion;

    /**
     * 테넌트ID
     */
    private String tenantId;

    /**
     * 업데이트사람ID
     */
    private String updaterId;
}