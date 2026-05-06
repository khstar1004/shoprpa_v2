package com.iflytek.rpa.component.entity.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EditCompUseDto {

    @NotBlank(message = "컴포넌트ID비워 둘 수 없습니다")
    String componentId;

    @NotBlank(message = "봇ID비워 둘 수 없습니다")
    String robotId;

    @NotBlank(message = "실행위치비워 둘 수 없습니다")
    String mode; // 실행위치
}