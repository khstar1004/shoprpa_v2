package com.iflytek.rpa.dispatch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 스케줄링방식-예약 작업실행기록유형
 *
 * @author jqfang
 * @since 2025-08-15
 */
@Data
@TableName("dispatch_task_execute_record")
public class DispatchTaskExecuteRecord implements Serializable {
    private static final long serialVersionUID = 221173423657136377L;

    /**
     * 기본 키id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 스케줄링방식예약 작업id
     */
    private Long dispatchTaskId;

    /**
     * 스케줄링방식예약 작업실행id
     */
    private Long dispatchTaskExecuteId;

    /**
     * 실행, 1, 2, 3....
     */
    private Integer count;

    /**
     * 트리거파일: 트리거manual, 예약schedule, 예약트리거trigger
     */
    private String dispatchTaskType;

    /**
     * 실행 결과:성공success, 실패error, 실행중executing, 중중지cancel, 아래발송실패dispatch_error
     */
    private String result;

    /**
     * 작업json데이터, 사용재시도시돌아가기조회매개변수
     */
    private String taskDetailJson;

    /**
     * 실행시작 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    /**
     * 실행종료 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    /**
     * 실행시 단일위치초
     */
    private Long executeTime;

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