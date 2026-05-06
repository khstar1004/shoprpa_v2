package com.iflytek.rpa.robot.entity.vo;

import lombok.Data;

@Data
public class ExeUpdateCheckVo {
    String robotId;
    String appId;
    Integer updateStatus; // 0: 아니요안내업데이트 1: 안내업데이트
}