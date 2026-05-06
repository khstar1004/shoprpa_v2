package com.iflytek.rpa.dispatch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 스케줄링방식-예약 작업및봇테이블유형
 *
 * @author jqfang3
 * @since 2025-08-15
 */
@Data
@TableName("dispatch_task_robot")
public class DispatchTaskRobot implements Serializable {
    private static final long serialVersionUID = 221173423657236317L;

    /**
     * 기본 키id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 스케줄링방식예약 작업id
     */
    private String dispatchTaskId;

    /**
     * 봇ID
     */
    private String robotId;

    /**
     * 봇버전
     */
    private Integer version;
    /**
     * 여부사용버전:  0:사용할 수 없습니다,1:완료사용
     */
    private Boolean online;
    /**
     * 봇구성 매개변수
     */
    private String paramJson;

    /**
     * 정렬, 소전
     */
    private Integer sort;

    /**
     * 테넌트id
     */
    private String tenantId;

    /**
     * 생성자id
     */
    private String creatorId;

    /**
     * 수정자id
     */
    private String updaterId;

    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 삭제 여부 0: 삭제되지 않음, 1: 삭제됨
     */
    private Integer deleted;
}