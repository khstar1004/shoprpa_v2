package com.iflytek.rpa.market.entity.vo;

import lombok.Data;

@Data
public class AppUpdateCheckVo {
    String appId; // appId
    Integer updateStatus; // 0: 아니요안내업데이트 1: 안내업데이트
}