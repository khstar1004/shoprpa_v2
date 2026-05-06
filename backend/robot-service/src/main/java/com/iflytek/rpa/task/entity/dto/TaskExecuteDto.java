package com.iflytek.rpa.task.entity.dto;

import com.iflytek.rpa.robot.entity.RobotExecuteRecord;
import com.iflytek.rpa.task.entity.ScheduleTaskExecute;
import java.util.List;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskExecuteDto extends ScheduleTaskExecute {

    @NotBlank(message = "작업id비워 둘 수 없습니다")
    private String taskId;

    private List<RobotExecuteRecord> robotExecuteRecordList;

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
     * dispatch작업ID
     */
    private Long dispatchTaskId;

    /**
     * dispatch작업실행ID
     */
    private Long dispatchTaskExecuteId;

    /**
     * 단말ID
     */
    private String terminalId;
}