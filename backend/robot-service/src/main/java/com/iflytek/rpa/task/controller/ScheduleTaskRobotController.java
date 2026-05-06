package com.iflytek.rpa.task.controller;

import com.iflytek.rpa.task.service.ScheduleTaskRobotService;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 예약 작업봇목록(ScheduleTaskRobot)테이블제어
 *
 * @author mjren
 * @since 2024-10-15 14:59:09
 */
@RestController
@RequestMapping("scheduleTaskRobot")
public class ScheduleTaskRobotController {
    /**
     * 서비스객체
     */
    @Resource
    private ScheduleTaskRobotService scheduleTaskRobotService;
}