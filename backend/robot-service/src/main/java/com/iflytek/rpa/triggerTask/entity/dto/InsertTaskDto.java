package com.iflytek.rpa.triggerTask.entity.dto;

import com.iflytek.rpa.task.entity.dto.RobotInfo;
import java.util.List;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InsertTaskDto {
    /**
     * 트리거기기예약 작업이름
     */
    @NotBlank
    private String name;

    /**
     * 오류예관리: 건너뛰기 jump, 중중지 stop
     */
    @NotBlank
    private String exceptional;

    /**
     * 여부사용 1 사용 ;0 아니요사용
     */
    private Integer enable; // 여부사용 1 사용 ;0 아니요사용

    /**
     * 작업유형: 예약:schedule, mail, file, hotKey, manual:
     */
    @NotBlank
    private String taskType;

    /**
     * 시간 초과시간
     */
    private Integer timeout;

    /**
     * 생성예약 작업의매개변수
     */
    @NotBlank
    private String taskJson;

    /**
     * 봇실행순서열
     */
    private List<RobotInfo> robotInfoList;

    /**
     * 여부사용정렬팀 1:사용 0:아니요사용
     */
    private Integer queueEnable;
}