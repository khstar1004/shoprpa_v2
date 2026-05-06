package com.iflytek.rpa.robot.entity.dto;

import com.iflytek.rpa.base.entity.dto.OpenModuleDto;
import lombok.Data;

@Data
public class SaveModuleDto extends OpenModuleDto {
    String moduleContent; // 코드모듈내용
    String robotId; // robotID

    /**
     * python모듈기록
     */
    String breakpoint;
}