package com.iflytek.rpa.task.entity.bo;

import lombok.Data;

@Data
public class ScheduleRule {
    private Integer dayOfWeek; // 예약: 

    private Integer year; // 예약: 년

    private Integer month; // 예약: 월

    private Integer date; // 예약: 일

    private Integer hour; // 예약: 시

    private Integer minute; // 예약: 분

    private Integer second; // 예약: 초
}