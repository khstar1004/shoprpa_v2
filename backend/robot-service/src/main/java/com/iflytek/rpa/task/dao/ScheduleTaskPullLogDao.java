package com.iflytek.rpa.task.dao;

import com.iflytek.rpa.task.entity.ScheduleTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * (ScheduleTaskPullLog)테이블데이터베이스방문
 *
 * @author mjren
 * @since 2024-11-18 14:13:21
 */
@Mapper
public interface ScheduleTaskPullLogDao {

    Integer insetLog(ScheduleTask task);
}