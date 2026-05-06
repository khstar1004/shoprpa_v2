package com.iflytek.rpa.task.entity.vo;

import com.iflytek.rpa.robot.entity.RobotExecuteRecord;
import java.util.List;
import lombok.Data;

/**
 * 예약 작업실행기록목록VO
 * @author jqfang3
 * @since 2025-08-05
 */
@Data
public class TaskRecordListVo {

    /**
     * 작업실행ID
     */
    private String taskExecuteId;

    /**
     * 작업ID
     */
    private String taskId;

    /**
     * 작업이름
     */
    private String taskName;
    /**
     * 작업실행
     */
    private Integer count;
    /**
     * 작업유형 : manual, 예약: schedule, 메일: mail, 파일: file, : hotKey
     */
    private String taskType;

    /**
     * 작업시작 시간
     */
    private String taskStartTime;

    /**
     * 작업종료 시간
     */
    private String taskEndTime;

    /**
     * 작업상태: 
     * 성공: success, 시작 실패: start_error, 실행실패: exe_error, 가져오기 : cancel, 실행중: executing
     */
    private String taskExecuteStatus;

    /**
     * 봇실행기록목록
     */
    private List<RobotExecuteRecord> robotExecuteRecordList;
}