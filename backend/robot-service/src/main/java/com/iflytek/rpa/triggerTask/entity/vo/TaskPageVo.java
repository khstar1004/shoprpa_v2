package com.iflytek.rpa.triggerTask.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

@Data
public class TaskPageVo {
    String taskId; // 트리거기기예약 작업id
    String name; // 트리거기기예약 작업이름
    String robotNames; // 봇이름, 사용열기

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    Date createTime;

    String taskType; // 예약:schedule, 메일mail, 파일file, hotKey
    Integer enable; // 여부사용 1 사용 ;0 아니요사용
    String taskJson; // 예약 작업구성 매개변수
}