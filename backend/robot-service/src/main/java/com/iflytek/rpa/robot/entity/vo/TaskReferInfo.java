package com.iflytek.rpa.robot.entity.vo;

import java.util.List;
import lombok.Data;

@Data
public class TaskReferInfo {

    // taskId
    String taskId;

    // 사용해당실행기기의예약 작업이름
    String taskName;

    // 실행기기이름;
    List<String> robotNames;

    // 높이index위치
    List<Integer> highIndex;
}