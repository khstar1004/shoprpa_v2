package com.iflytek.rpa.dispatch.entity.dto;

import com.iflytek.rpa.dispatch.entity.DispatchTaskRobotExecuteRecord;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RobotExecuteStatusDto extends DispatchTaskRobotExecuteRecord {
    /*
     * 봇실행id
     */
    //    @NotNull(message = "봇실행ID비워 둘 수 없습니다")
    private Long executeId;

    /*
     * 봇id
     */
    @NotBlank(message = "봇ID비워 둘 수 없습니다")
    private String robotId;

    /*
     * 봇버전
     */
    @NotNull(message = "봇버전비워 둘 수 없습니다")
    private Integer robotVersion;

    /*
     * 작업실행ID
     */
    @NotNull(message = "작업실행ID비워 둘 수 없습니다")
    private Long dispatchTaskExecuteId;

    /*
     * 단말ID
     */
    private String terminalId;

    /*
     * 작업실행상태,   실행 결과:: robotFail:실패,  robotSuccess:성공, robotCancel:가져오기 (중중지), robotExecute:정상에서실행
     */
    private String result;

    /*
     * 오류원인
     */
    private String error_reason;

    /**
     * 봇구성 매개변수
     */
    private String paramJson;

    /*
     * 실행로그
     */
    private String executeLog;

    /*
     * 본저장경로
     */
    private String videoLocalPath;

    private String dataTablePath;
}