package com.iflytek.rpa.triggerTask.entity.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateTaskDto extends InsertTaskDto {
    /**
     * 트리거기기예약 작업id
     */
    @NotBlank
    String taskId;
}