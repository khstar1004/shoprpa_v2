package com.iflytek.rpa.task.service;

import com.iflytek.rpa.task.entity.ScheduleTaskRobot;

/**
 * 예약 작업봇목록(ScheduleTaskRobot)테이블서비스연결
 *
 * @author mjren
 * @since 2024-10-15 14:59:09
 */
public interface ScheduleTaskRobotService {

    /**
     * 통신경과ID조회단일데이터
     *
     * @param id 기본 키
     * @return 객체
     */
    ScheduleTaskRobot queryById(Long id);
}