package com.iflytek.rpa.auth.blacklist.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 차단 목록 엔티티
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
     * 차단 사유
     */
    @TableField("ban_reason")
    private String banReason;

    /**
     * 차단 단계(1,2,3...)
     */
    @TableField("ban_level")
    private Integer banLevel;

    /**
     * 차단 횟수
     */
    @TableField("ban_count")
    private Integer banCount;

    /**
     * 차단 기간(초)
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
     * 상태(1:차단 중, 0:해제됨)
     */
    @TableField("status")
    private Integer status;

    /**
     * 처리자
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
