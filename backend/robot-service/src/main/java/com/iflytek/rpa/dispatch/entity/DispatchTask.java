package com.iflytek.rpa.dispatch.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 스케줄링방식-예약 작업유형
 *
 * @author jqfang
 * @since 2025-08-15
 */
@Data
@TableName("dispatch_task")
public class DispatchTask implements Serializable {
    private static final long serialVersionUID = 221173423657236377L;

    /**
     * 스케줄링방식예약 작업id
     */
    @TableId
    private String dispatchTaskId;

    /**
     * 작업상태: 사용중 active, 닫기 stop, 완료경과 expired
     */
    private String status;

    /**
     * 스케줄링방식예약 작업이름
     */
    private String name;

    /**
     * 생성스케줄링예약 작업의매개변수;예약schedule저장계획계획실행의JSON
     */
    private String cronJson;

    /**
     * 트리거파일: 트리거manual, 예약schedule, 예약트리거trigger
     */
    private String type;

    /**
     * 오류예관리: 건너뛰기jump, 중지stop, 재시도후건너뛰기retry_jump, 재시도후중지retry_stop
     */
    private String exceptional;

    /**
     * 재시도 데이터
     */
    private Integer retryNum;

    /**
     * 여부사용시간 초과시간 1:사용 0:아니요사용
     */
    private Boolean timeoutEnable;

    /**
     * 시간 초과시간
     */
    private Integer timeout;

    /**
     * 여부사용정렬팀 1:사용 0:아니요사용
     */
    private Boolean queueEnable;

    /**
     * 여부열기시작기록 1:사용 0:아니요사용
     */
    private Boolean screenRecordEnable;

    /**
     * 여부열기시작 1:사용 0:아니요사용
     */
    private Boolean virtualDesktopEnable;

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