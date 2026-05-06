package com.iflytek.rpa.terminal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

/**
 * @author jqfang3
 * @date 2025-06-17
 */
@Data
public class TerminalLoginRecord {

    /**
     * 기본 키id
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 단말id
     */
    private String terminalId;

    /**
     * 모듈id
     */
    private String deptId;

    /**
     * 모듈전체경로id
     */
    private String deptIdPath;

    /**
     * 로그인IP
     */
    private String ip;

    /**
     * 로그인시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date loginTime;

    /**
     * 로그아웃시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date logoutTime;

    /**
     * 여부로그인성공 (0: 로그인실패, 1: 로그인성공)
     */
    private Integer loginStatus;

    /**
     * 설명
     */
    private String remark;

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
     * 삭제 여부 (0: 삭제되지 않음, 1: 삭제됨)
     */
    private Integer deleted;
}