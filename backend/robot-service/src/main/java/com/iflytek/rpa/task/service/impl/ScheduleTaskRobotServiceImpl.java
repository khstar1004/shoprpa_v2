package com.iflytek.rpa.task.service.impl;

import com.iflytek.rpa.task.dao.ScheduleTaskRobotDao;
import com.iflytek.rpa.task.entity.ScheduleTaskRobot;
import com.iflytek.rpa.task.service.ScheduleTaskRobotService;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 예약 작업봇목록(ScheduleTaskRobot)테이블서비스유형
 *
 * @author mjren
 * @since 2024-10-15 14:59:09
 */
@Service("scheduleTaskRobotService")
public class ScheduleTaskRobotServiceImpl implements ScheduleTaskRobotService {
    @Resource
    private ScheduleTaskRobotDao scheduleTaskRobotDao;

    /**
     * 통신경과ID조회단일데이터
     *
     * @param id 기본 키
     * @return 객체
     */
    @Override
    public ScheduleTaskRobot queryById(Long id) {
        return this.scheduleTaskRobotDao.queryById(id);
    }
}