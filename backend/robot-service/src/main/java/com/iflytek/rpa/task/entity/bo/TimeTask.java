package com.iflytek.rpa.task.entity.bo;

import javax.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class TimeTask {
    /**
     * 실행방식, circular,예약fixed,지정custom
     */
    @NotBlank(message = "실행방식비워 둘 수 없습니다")
    private String runMode;
    /**
     * , -1로있음일, 3600, , , custom
     */
    @Length(max = 20, message = "길이정도할 수 없음초과경과20")
    private String cycleFrequency;
    /**
     * 유형, 매1시간, 매3시간, , 지정
     */
    @Length(max = 20, message = "지정시길이경과길이")
    private String cycleNum;
    /**
     * 단일위치: minutes, hour
     */
    private String cycleUnit;

    private String scheduleType;

    private ScheduleRule scheduleRule;

    /**
     * cron테이블방식
     */
    private String cronExpression;
}