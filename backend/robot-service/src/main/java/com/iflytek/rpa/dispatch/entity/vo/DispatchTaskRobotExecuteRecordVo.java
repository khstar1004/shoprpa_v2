package com.iflytek.rpa.dispatch.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

@Data
public class DispatchTaskRobotExecuteRecordVo {

    /**
     * id
     */
    private String id;

    /**
     * 봇실행id
     */
    private String executeId;

    /**
     * 스케줄링방식예약 작업실행id
     */
    private String dispatchTaskExecuteId;

    /**
     * 봇id
     */
    private String robotId;

    /**
     * 봇이름
     */
    private String robotName;

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
}