package com.iflytek.rpa.auth.core.entity;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * @author mjren
 * @date 2025-03-12 16:53
 * @copyright Copyright (c) 2025 mjren
 */
@Data
public class DeptTreeNodeDto {

    private String id;
    private String appId;
    private String appName;
    private String name;
    private String pid;
    private String firstLevelId;
    private Integer sort;
    private String value;
    private String deptType;
    private String code;
    private Boolean hasNodes;
    private String status;
    private Boolean checked;

    /**
     * 모듈사람데이터
     */
    private Long userNum;

    /**
     * 모듈사람id
     */
    private String userId;

    /**
     * 모듈사람이름
     */
    private String userName;

    private List<DeptTreeNodeDto> nodes = new ArrayList();
}