package com.iflytek.rpa.task.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.iflytek.rpa.task.entity.bo.TimeTask;
import java.util.Date;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskInfoDto {
    /**
     * 예약 작업id
     */
    private String taskId;

    private String taskType = "taskTime";

    /**
     * 작업이름
     */
    @NotNull(message = "작업이름비워 둘 수 없습니다")
    private String name;
    /**
     * 설명
     */
    private String description;
    /**
     * 실행봇순서열
     */
    //    @JsonSerialize(using = ListRobotJsonSerializer.class)
    private List<String> executeSequence;

    /**
     * 예외관리방식: stop중지  skip건너뛰기
     */
    private String exceptionHandleWay;

    /**
     * 시작 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date startAt;
    /**
     * 종료 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date endAt;

    @Valid
    private TimeTask timeTask;
}