package com.iflytek.rpa.base.entity.dto;

import static com.iflytek.rpa.robot.constants.RobotConstant.EDIT_PAGE;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OpenModuleDto extends ProcessModuleListDto {
    @NotBlank(message = "실행위치비워 둘 수 없습니다")
    private String mode = EDIT_PAGE;

    String moduleId; // 모듈Id

    /**
     * python모듈
     */
    private String breakpoint;
}