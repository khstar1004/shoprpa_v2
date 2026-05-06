package com.iflytek.rpa.base.entity.dto;

import lombok.Data;

@Data
public class RenameModuleDto extends ProcessModuleListDto {
    String moduleName; // 모듈이름
    String moduleId; // 모듈Id
}