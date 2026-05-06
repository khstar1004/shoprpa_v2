package com.iflytek.rpa.terminal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

/**
 * @author mjren
 * @date 2025-06-10 16:38
 * @copyright Copyright (c) 2025 mjren
 */
@Data
public class Terminal {

    /**
     * 기본 키id, 사용데이터예약시스템계획의정도관리관리
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 단말일식별자, 예준비mac주소
     */
    private String terminalId;

    /**
     * 테넌트ID
     */
    private String tenantId;

    /**
     * 모듈ID
     */
    private String deptId;

    /**
     * 모듈전체경로ID
     */
    private String deptIdPath;

    /**
     * 단말이름
     */
    private String name;

    /**
     * 준비계정
     */
    private String account;

    /**
     * 운영체제
     */
    private String os;

    /**
     * ip목록
     */
    private String ip;

    /**
     * 연결IP, 서버감지후의권장ip
     */
    private String actualClientIp;

    /**
     * 사용자지정ip
     */
    private String customIp;

    /**
     * 단말
     */
    private Integer port;
    /**
     * 사용자지정단말
     */
    private Integer customPort;
    /**
     * 현재상태, 실행중busy, 빈free, offline, 단일기기중standalone
     */
    private String status;

    /**
     * 단말설명
     */
    private String remark;

    /**
     * 후로그인의사용자의id, 사용근거이름선택
     */
    private String userId;

    /**
     * 정보 : 준비사용자명, 단말계정
     */
    private String osName;

    /**
     * 정보 : 준비사용자비밀번호, 단말계정비밀번호
     */
    private String osPwd;

    //    /**
    //     * CPU사용(분)
    //     */
    //    @TableField(exist = false)
    //    private Integer cpu;
    //
    //    /**
    //     * 메모리사용(분)
    //     */
    //    @TableField(exist = false)
    //    private Integer memory;
    //
    //    /**
    //     * 하드사용(분)
    //     */
    //    @TableField(exist = false)
    //    private Integer disk;

    /**
     * 여부스케줄링방식 (0: 아니요, 1: 예)
     */
    private Integer isDispatch;

    /**
     * URL
     */
    private String monitorUrl;

    /**
     * 단말기록생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date createTime;

    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 삭제 여부 (0: 삭제되지 않음, 1: 삭제됨)
     */
    private Short deleted;
}