package com.iflytek.rpa.task.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iflytek.rpa.task.entity.dto.ScheduleTaskRecordDeleteDto;
import com.iflytek.rpa.task.entity.dto.ScheduleTaskRecordDto;
import com.iflytek.rpa.task.entity.dto.TaskExecuteDto;
import com.iflytek.rpa.task.entity.vo.TaskRecordListVo;
import com.iflytek.rpa.task.service.ScheduleTaskExecuteService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 예약 작업실행기록
 *
 * @author mjren
 * @since 2024-10-15 14:59:09
 */
@RestController
@RequestMapping("/task-execute")
public class ScheduleTaskExecuteController {
    /**
     * 서비스객체
     */
    @Resource
    private ScheduleTaskExecuteService scheduleTaskExecuteService;

    /**
     * 예약 작업-실행상태위
     *
     * @param executeDto
     * @return String
     * @throws NoLoginException
     */
    @PostMapping("/status")
    public AppResponse<?> setTaskExecuteResult(@RequestBody TaskExecuteDto executeDto) throws NoLoginException {
        return scheduleTaskExecuteService.setTaskExecuteStatus(executeDto);
    }

    /**
     * 예약 작업-실행기록목록
     *
     * @param executeDto
     * @return
     * @throws NoLoginException
     */
    /*    @PostMapping("/list")
    public AppResponse<?> getTaskExecuteRecordList(@Valid @RequestBody TaskExecuteDto executeDto) throws NoLoginException {
        return scheduleTaskExecuteService.getTaskExecuteRecordList(executeDto);
    }*/

    /**
     * 예약 작업실행기록
     *
     * @param recordDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/list")
    public AppResponse<IPage<TaskRecordListVo>> getRecordList(@RequestBody ScheduleTaskRecordDto recordDto)
            throws NoLoginException {
        return scheduleTaskExecuteService.getRecordList(recordDto);
    }

    /**
     * 량삭제예약 작업실행기록
     *
     * @param dto 패키지필요삭제의작업실행ID목록
     * @return 삭제결과
     * @throws NoLoginException
     */
    @PostMapping("/batch-delete")
    public AppResponse<?> batchDelete(@RequestBody ScheduleTaskRecordDeleteDto dto) throws NoLoginException {
        return scheduleTaskExecuteService.batchDelete(dto);
    }
}