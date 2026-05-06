package com.iflytek.rpa.triggerTask.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.util.Date;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TriggerTask implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 트리거기기예약 작업id
     */
    @NotBlank
    private String taskId;

    /**
     * 트리거기기예약 작업이름
     */
    @NotBlank
    private String name;

    /**
     * 생성예약 작업의매개변수
     */
    @NotBlank
    private String taskJson;

    /**
     * 작업유형: 예약:schedule, mail, file, hotKey, manual:
     */
    @NotBlank
    private String taskType;

    /**
     * 여부사용 1 사용 ;0 아니요사용
     */
    private Integer enable;

    /**
     * 오류예관리: 건너뛰기 jump, 중중지 stop
     */
    @NotBlank
    private String exceptional;

    /**
     * 시간 초과시간
     */
    private Integer timeout;

    /**
     * 여부사용정렬팀 1:사용 0:아니요사용
     */
    private Integer queueEnable;

    private Integer deleted = 0;

    private String creatorId;

    private String tenantId;

    private Date createTime;

    private String updaterId;

    private Date updateTime;
}