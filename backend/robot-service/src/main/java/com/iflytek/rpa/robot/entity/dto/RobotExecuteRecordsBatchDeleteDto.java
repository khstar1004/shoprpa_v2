package com.iflytek.rpa.robot.entity.dto;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RobotExecuteRecordsBatchDeleteDto {

    /**
     * 봇실행기록ID List
     */
    @NotNull(message = "실행기록ID-List비워 둘 수 없습니다")
    private List<String> recordIds;
}