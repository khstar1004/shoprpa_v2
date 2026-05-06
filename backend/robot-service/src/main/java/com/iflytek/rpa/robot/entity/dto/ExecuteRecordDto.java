package com.iflytek.rpa.robot.entity.dto;

import com.iflytek.rpa.robot.entity.RobotExecuteRecord;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExecuteRecordDto extends RobotExecuteRecord {

    @NotBlank(message = "실행id비워 둘 수 없습니다")
    private String executeId;

    @NotBlank(message = "봇id비워 둘 수 없습니다")
    private String robotId;
    /**
     * 예약 작업실행id
     */
    private String taskExecuteId;

    @NotBlank(message = "시작방식비워 둘 수 없습니다")
    private String mode;

    @NotBlank(message = "실행 결과비워 둘 수 없습니다")
    private String result;

    private String deptIdPath;

    private Integer pageNo;

    private Integer pageSize;

    private String sortBy;

    private String sortType;

    // 추가dispatch닫기필드
    /**
     * 여부로dispatch방식, true테이블dispatch, false테이블기존있음방식
     */
    private Boolean isDispatch = false;

    /**
     * 봇버전
     */
    private Integer robotVersion;

    /**
     * dispatch작업실행ID
     */
    private Long dispatchTaskExecuteId;

    /**
     * 단말ID
     */
    private String terminalId;

    /**
     * 오류원인
     */
    private String error_reason;

    /**
     * 실행로그
     */
    private String executeLog;

    /**
     * 본저장경로
     */
    private String videoLocalPath;

    /**
     * 봇구성 매개변수
     */
    private String paramJson;
    /**
     * 데이터가져오기위경로
     */
    private String dataTablePath;
}