package com.iflytek.rpa.robot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 단말봇실행기록테이블(RobotExecuteRecord)유형
 *
 * @author makejava
 * @since 2024-09-29 15:34:14
 */
@Data
public class RobotExecuteRecord implements Serializable {
    private static final long serialVersionUID = 930070558482150308L;
    /**
     * 기본 키id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String executeId;

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
     * : startBtn,schedule,commander비고: 실행방식목록전필요예삼(1)클릭실행버튼실행봇(2)예약 작업실행의봇(3)commander단말실행봇
     */
    private String mode;
    /**
     * 예약 작업실행id
     */
    private String taskExecuteId;
    /**
     * 실행 결과
     * robotFail:실패
     * robotSuccess:성공
     * robotCancel:가져오기 (중중지)
     * robotExecute:정상에서실행
     */
    private String result;
    /**
     * 오류원인
     */
    private String errorReason;
    /**
     * 로그내용
     */
    private String executeLog;
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

    private String tenantId;

    private String videoLocalPath;

    private String terminalId;

    @TableField(exist = false)
    private String taskName;

    private String dataTablePath;
}