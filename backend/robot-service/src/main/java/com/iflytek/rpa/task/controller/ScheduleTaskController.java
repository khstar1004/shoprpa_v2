package com.iflytek.rpa.task.controller;

import com.iflytek.rpa.task.entity.ScheduleTask;
import com.iflytek.rpa.task.entity.dto.ScheduleTaskDto;
import com.iflytek.rpa.task.entity.dto.TaskDto;
import com.iflytek.rpa.task.service.ScheduleTaskService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 예약 작업(ScheduleTask)테이블제어
 *
 * @author makejava
 * @since 2024-09-29 15:27:42
 */
@RestController
@RequestMapping("/task")
public class ScheduleTaskController {
    /**
     * 서비스객체
     */
    @Resource
    private ScheduleTaskService scheduleTaskService;

    /**
     * 예약 작업목록조회
     * @param taskDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/list")
    public AppResponse<?> cloudTaskList(@RequestBody TaskDto taskDto) throws NoLoginException {
        return scheduleTaskService.getTaskList(taskDto);
    }

    /**
     * 저장/업데이트예약 작업
     * @param task
     * @return
     * @throws Exception
     */
    @PostMapping("/save")
    public AppResponse<?> saveTask(@Valid @RequestBody ScheduleTaskDto task) throws Exception {
        return scheduleTaskService.saveTask(task);
    }

    /**
     * 예약 작업정보
     * @return
     */
    @PostMapping("/task-info")
    public AppResponse<?> getTaskInfoByTaskId(@RequestBody ScheduleTaskDto task) throws NoLoginException {
        return scheduleTaskService.getTaskInfoByTaskId(task.getTaskId());
    }

    /**
     * 가져오기예약 작업아래실행시간봇정보
     * @param
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/next-time")
    public AppResponse<?> getNextTimeInfo() throws NoLoginException {
        return scheduleTaskService.getNextTimeInfoAndUpdate();
    }

    /**
     * 예약 작업-사용, 사용 안 함
     * @param
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/enable")
    public AppResponse<?> enableTask(@RequestBody ScheduleTask task) throws NoLoginException {
        return scheduleTaskService.enableTask(task);
    }

    /**
     * 예약 작업-삭제
     * @param task
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/delete")
    public AppResponse<?> deleteTask(@RequestBody ScheduleTask task) throws NoLoginException {
        return scheduleTaskService.deleteTask(task);
    }

    /**
     * 예약 작업-이름 변경검증
     * @param task
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/name/same")
    public AppResponse<?> checkSameName(@RequestBody ScheduleTask task) throws NoLoginException {
        return scheduleTaskService.checkSameName(task);
    }

    /**
     * 예약 작업-corn테이블방식검증
     * @param task
     * @return true 테이블검증통신경과, false테이블검증아니요통신경과
     * @throws NoLoginException
     */
    @PostMapping("/corn/check")
    public AppResponse<?> checkCorn(@RequestBody ScheduleTask task) throws NoLoginException {
        return scheduleTaskService.checkCorn(task);
    }
}