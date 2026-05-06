package com.iflytek.rpa.task.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 예약 작업봇목록(ScheduleTaskRobot)유형
 *
 * @author mjren
 * @since 2024-10-15 14:59:09
 */
@Data
public class ScheduleTaskRobot implements Serializable {
    private static final long serialVersionUID = -98756982211004692L;

    private Long id;
    /**
     * 작업ID
     */
    private String taskId;
    /**
     * 봇ID
     */
    private String robotId;
    /**
     * 봇버전
     */
    private Integer version;
    /**
     * 정렬, 소전
     */
    private Integer sort;

    private String tenantId;
    /**
     * 생성자id
     */
    private String creatorId;
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
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
    /**
     * 삭제 여부 0: 삭제되지 않음, 1: 삭제됨
     */
    private Integer deleted;

    @TableField(exist = false)
    private String robotName;

    @TableField(exist = false)
    private String taskName;

    /**
     * 예약 작업봇구성 매개변수
     */
    private String paramJson;
}