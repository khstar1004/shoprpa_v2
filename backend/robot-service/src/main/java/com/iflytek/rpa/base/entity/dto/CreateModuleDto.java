package com.iflytek.rpa.base.entity.dto;

import lombok.Data;

@Data
public class CreateModuleDto extends ProcessModuleListDto {
    String moduleName; // 모듈이름
}