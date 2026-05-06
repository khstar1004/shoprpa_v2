package com.iflytek.rpa.component.entity.vo;

import lombok.Data;

@Data
public class CompManageVo {
    String componentId;
    String name;
    String icon;
    String introduction;
    Integer version; // 버전
    Integer latestVersion; // 새버전
    Integer blocked; // 여부설치 1 예:  0 아니요  (`제거` 및 `설치` 버튼)
    Integer isLatest; // 여부예새버전 1 예: 0 아니요
}