package com.iflytek.rpa.triggerTask.entity.vo;

import java.util.List;
import lombok.Data;

@Data
public class TaskPage4TriggerVo extends TaskPageVo {
    String taskJson; // 예약 작업구성 매개변수
    String exceptional;
    Integer timeout;
    Integer queueEnable;
    List<RobotInfoVo> robotInfoList; // 봇닫기모듈분매개변수
}