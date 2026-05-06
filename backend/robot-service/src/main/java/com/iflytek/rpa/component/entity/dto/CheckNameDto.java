package com.iflytek.rpa.component.entity.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckNameDto {

    @NotBlank(message = "컴포넌트새이름비워 둘 수 없습니다")
    String name;

    String componentId; // 컴포넌트id
}