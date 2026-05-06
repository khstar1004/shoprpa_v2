package com.iflytek.rpa.base.entity.vo;

import lombok.Data;

@Data
public class ProcessModuleListVo {
    String ResourceCategory; // 분유형 :  프로세스 ,  코드모듈
    String name; // 이름
    String resourceId; // id
}