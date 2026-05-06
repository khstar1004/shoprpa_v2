package com.iflytek.rpa.auth.blacklist.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자이름단일유형
 *
 * @author system
 * @date 2025-12-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_blacklist")
public class UserBlacklist implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 기본 키ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 사용자ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 사용자명
     */
    @TableField("username")
    private String username;

    /**
     * 원인
     */
    @TableField("ban_reason")
    private String banReason;

    /**
     * 대기단계(1,2,3...)
     */
    @TableField("ban_level")
    private Integer banLevel;

    /**
     * 데이터
     */
    @TableField("ban_count")
    private Integer banCount;

    /**
     * 시길이(초)
     */
    @TableField("ban_duration")
    private Long banDuration;

    /**
     * 시작 시간
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 종료 시간
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 상태(1:중, 0:완료해제)
     */
    @TableField("status")
    private Integer status;

    /**
     * 사람
     */
    @TableField("operator")
    private String operator;

    /**
     * 생성 시간
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 수정 시간
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}