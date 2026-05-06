package com.iflytek.rpa.base.entity.dto;

import static com.iflytek.rpa.robot.constants.RobotConstant.EDIT_PAGE;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author mjren
 * @date 2025-04-17 10:45
 * @copyright Copyright (c) 2025 mjren
 */
@Data
public class QueryParamDto {

    @NotBlank(message = "봇ID비워 둘 수 없습니다")
    private String robotId;

    /**
     * 실행위치, , EDIT_PAGE,PROJECT_LIST계획기기목록 ,EXECUTOR실행기기봇목록 ,CRONTAB트리거기기(본예약 작업)
     */
    private String mode = EDIT_PAGE;
    /**
     * 프로세스ID
     */
    private String processId;

    /**
     * schedule_task_robot테이블의id, 일개봇A에서예약 작업1중가능으로출력다중
     */
    private Long taskRobotUniqueId;
    /**
     * 스케줄링방식예약 작업봇버전
     */
    private Integer robotVersion;

    /**
     * python모듈ID
     */
    private String moduleId;
}