package com.iflytek.rpa.triggerTask.entity.vo;

import com.iflytek.rpa.task.entity.dto.RobotInfo;
import lombok.Data;

@Data
public class RobotInfoVo extends RobotInfo {

    /**
     * task_robot테이블의id, 사용분일개예약 작업내부다중개의봇
     */
    private Long id;

    /**
     * 여부있음구성 매개변수
     */
    private Boolean haveParam;

    Integer sort; // 순서열
    String robotName; // 봇이름
    Integer robotVersion; // 봇버전
}