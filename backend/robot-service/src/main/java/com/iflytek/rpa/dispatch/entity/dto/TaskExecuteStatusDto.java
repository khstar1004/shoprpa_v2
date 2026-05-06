package com.iflytek.rpa.dispatch.entity.dto;

import com.iflytek.rpa.dispatch.entity.DispatchTaskExecuteRecord;
import lombok.Data;

@Data
public class TaskExecuteStatusDto extends DispatchTaskExecuteRecord {
    /*
     * 작업ID
     */
    private Long dispatchTaskId;

    /*
     * 작업실행ID
     */
    private Long dispatchTaskExecuteId;

    /*
     * 단말ID
     */
    private String terminalId;

    /*
     * 작업실행상태,   성공  "success"     # 시작 실패     "start_error"     # 실행실패      "exe_error"     # 가져오기      CANCEL = "cancel"     # 실행중   "executing"
     */
    private String result;
}