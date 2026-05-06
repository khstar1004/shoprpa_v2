package com.iflytek.rpa.dispatch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 스케줄링방식-예약 작업-단말테이블유형
 *
 * @author jqfang
 * @since 2025-08-15
 */
@Data
@TableName("dispatch_task_terminal")
public class DispatchTaskTerminal implements Serializable {
    private static final long serialVersionUID = 113373423657236317L;

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
     * 트리거파일: 단말terminal, 단말분그룹group
     */
    private String terminalOrGroup;

    /**
     * 실행방식: 기기일random_one, 전체실행all
     */
    private String executeMethod;

    /**
     * 값: 저장 list<id>
     */
    private String value;
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