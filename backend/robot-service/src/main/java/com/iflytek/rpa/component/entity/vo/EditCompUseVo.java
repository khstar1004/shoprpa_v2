package com.iflytek.rpa.component.entity.vo;

import lombok.Data;

@Data
public class EditCompUseVo {
    String componentId; // 컴포넌트id
    String name; // 컴포넌트이름
    Integer componentVersion; // 컴포넌트버전
    String icon; // 컴포넌트icon
}