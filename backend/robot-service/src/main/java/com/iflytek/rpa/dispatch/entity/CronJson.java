package com.iflytek.rpa.dispatch.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CronJson {
    /**
     * 실행데이터
     */
    @JsonProperty("times")
    private String times;
    /**
     * 종료 시간, 형식: "2025-03-24 15:32:23"
     */
    @JsonProperty("end_time")
    private String endTime;

    /**
     * 로그, 값: regular, minutes, hours, days, weeks, months, advance
     */
    @JsonProperty("frequency_flag")
    private String frequencyFlag;

    /**
     * 분, : [0, 59]
     */
    @JsonProperty("minutes")
    private Integer minutes;

    /**
     * 시간, : [0, 23]
     */
    @JsonProperty("hours")
    private Integer hours;

    /**
     * , : [0, 6], 0테이블일요일
     */
    @JsonProperty("weeks")
    private List<Integer> weeks;

    /**
     * 월, : [1, 12]
     */
    @JsonProperty("months")
    private List<Integer> months;

    /**
     * 시간테이블방식, 형식: "2025-03-24 15:32:23"
     */
    @JsonProperty("time_expression")
    private String timeExpression;

    /**
     * cron테이블방식, 형식: "* * * * *"
     */
    @JsonProperty("cron_expression")
    private String cronExpression;
}