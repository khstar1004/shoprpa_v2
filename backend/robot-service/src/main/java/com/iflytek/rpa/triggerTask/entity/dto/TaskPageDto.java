package com.iflytek.rpa.triggerTask.entity.dto;

import lombok.Data;

@Data
public class TaskPageDto {
    Integer pageSize = 8;
    Integer pageNo = 1;
    String name; // 예약 작업이름, 조회
    String taskType; // 예약:schedule, 메일mail, 파일file, hotKey
}