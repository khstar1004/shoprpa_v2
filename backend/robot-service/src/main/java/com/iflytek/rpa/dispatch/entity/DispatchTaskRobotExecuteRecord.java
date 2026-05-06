package com.iflytek.rpa.dispatch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 스케줄링방식-예약 작업-봇-실행기록유형
 *
 * @author jqfang
 * @since 2025-08-15
 */
@Data
@TableName("dispatch_task_robot_execute_record")
public class DispatchTaskRobotExecuteRecord implements Serializable {
    private static final long serialVersionUID = 223373423657236317L;

    /**
     * 기본 키id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 봇실행id
     */
    private Long executeId;

    /**
     * 스케줄링방식예약 작업실행id
     */
    private Long dispatchTaskExecuteId;

    /**
     * 봇id
     */
    private String robotId;

    /**
     * 봇버전
     */
    private Integer robotVersion;

    /**
     * 시작 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    /**
     * 종료 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    /**
     * 실행시 단일위치초
     */
    private Long executeTime;

    /**
     * 실행 결과: robotFail:실패,  robotSuccess:성공, robotCancel:가져오기 (중중지), robotExecute:정상에서실행
     */
    private String result;

    /**
     * 봇구성 매개변수
     */
    private String paramJson;

    /**
     * 오류원인
     */
    private String errorReason;

    /**
     * 로그내용
     */
    private String executeLog;

    /**
     * 기록의본저장경로
     */
    private String videoLocalPath;

    /**
     * 모듈전체경로코드
     */
    private String deptIdPath;

    /**
     * 단말일식별자, 예준비mac주소
     */
    private String terminalId;

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