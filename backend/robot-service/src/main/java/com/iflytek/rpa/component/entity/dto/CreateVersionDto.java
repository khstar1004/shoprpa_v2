package com.iflytek.rpa.component.entity.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateVersionDto {
    @NotBlank(message = "컴포넌트id비워 둘 수 없습니다")
    String componentId; // 컴포넌트id

    @NotNull
    @Min(value = 1, message = "아래일개버전대0")
    Integer nextVersion; // 버전

    @NotBlank(message = "변경 로그비워 둘 수 없습니다")
    String updateLog; // 변경 로그

    @NotBlank(message = "컴포넌트이름문자비워 둘 수 없습니다")
    String name; // 컴포넌트이름문자

    @NotBlank(message = "이미지비워 둘 수 없습니다")
    String icon; // 이미지

    @NotBlank(message = "비워 둘 수 없습니다")
    String introduction; // 
}