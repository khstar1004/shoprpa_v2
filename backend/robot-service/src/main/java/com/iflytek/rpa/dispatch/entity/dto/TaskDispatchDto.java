package com.iflytek.rpa.dispatch.entity.dto;

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class TaskDispatchDto {
    @NotBlank(message = "작업분ID비워 둘 수 없습니다")
    private String dispatchTaskId;

    @NotEmpty(message = "단말비워 둘 수 없습니다")
    private List<String> terminalIds;

    private String dispatchTaskType;

    private String dispatchTaskFromType;
}