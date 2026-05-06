package com.iflytek.rpa.component.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 봇컴포넌트이미지객체
 *
 * @author makejava
 * @since 2024-12-19
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComponentRobotBlockVo {

    /**
     * 기본 키id
     */
    private Long id;

    /**
     * 봇id
     */
    private String robotId;

    /**
     * 봇버전
     */
    private Integer robotVersion;

    /**
     * 컴포넌트id
     */
    private String componentId;

    /**
     * 컴포넌트이름(닫기 조회)
     */
    private String componentName;

    /**
     * 봇이름(닫기 조회)
     */
    private String robotName;

    /**
     * 생성자id
     */
    private String creatorId;

    /**
     * 생성자이름(닫기 조회)
     */
    private String creatorName;

    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 수정자id
     */
    private String updaterId;

    /**
     * 수정자이름(닫기 조회)
     */
    private String updaterName;

    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 삭제 여부 0: 삭제되지 않음, 1: 삭제됨
     */
    private Integer deleted;

    /**
     * 테넌트id
     */
    private String tenantId;
}