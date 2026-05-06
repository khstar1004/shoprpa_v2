package com.iflytek.rpa.base.entity.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CGlobalDto {
    @NotBlank(message = "봇ID비워 둘 수 없습니다")
    private String robotId;

    private String globalId;
    private String varName;
    private String varType;
    private String varValue;
    private String varDescribe;
    private String creatorId;
    private String updaterId;
}