package com.iflytek.rpa.triggerTask.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.iflytek.rpa.triggerTask.entity.dto.InsertTaskDto;
import com.iflytek.rpa.triggerTask.entity.dto.TaskPageDto;
import com.iflytek.rpa.triggerTask.entity.dto.UpdateTaskDto;
import com.iflytek.rpa.triggerTask.entity.vo.Executor;
import com.iflytek.rpa.triggerTask.entity.vo.TaskPage4TriggerVo;
import com.iflytek.rpa.triggerTask.entity.vo.TaskPageVo;
import com.iflytek.rpa.triggerTask.entity.vo.TriggerTaskVo;
import com.iflytek.rpa.triggerTask.service.TriggerTaskService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/triggerTask")
public class TriggerTaskController {

    @Resource
    private TriggerTaskService triggerTaskService;

    /**
     * 이름 변경검증
     * @param name
     * @return
     * @throws NoLoginException
     */
    @GetMapping("/isNameCopy")
    AppResponse<Boolean> isTaskNameCopy(@RequestParam String name) throws NoLoginException {
        return triggerTaskService.isTaskNameCopy(name);
    }

    /**
     * 선택봇-봇목록, 지원조회
     * @param name
     * @return
     * @throws NoLoginException
     */
    @GetMapping("/robotExe/list")
    AppResponse<List<Executor>> getRobotExeList(@RequestParam String name)
            throws NoLoginException, JsonProcessingException {
        return triggerTaskService.getRobotExeList(name);
    }

    /**
     * 새생성예약 작업
     * 시삽입예약 작업닫기param매개변수 까지schedule_task_robot
     * @param queryDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/insert")
    AppResponse<Boolean> insertTriggerTask(@Valid @RequestBody InsertTaskDto queryDto) throws NoLoginException {
        return triggerTaskService.insertTriggerTask(queryDto);
    }

    /**
     * 예약 작업--작업정보돌아가기
     * @param taskId
     * @return
     */
    @GetMapping("/get")
    AppResponse<TriggerTaskVo> getTriggerTask(@RequestParam String taskId)
            throws NoLoginException, JsonProcessingException {
        return triggerTaskService.getTriggerTask(taskId);
    }

    /**
     * 삭제단일개예약 작업연결
     * @param taskId
     * @return
     */
    @GetMapping("/delete")
    AppResponse<Boolean> deleteTriggerTask(@RequestParam String taskId) throws NoLoginException {
        return triggerTaskService.deleteTriggerTask(taskId);
    }

    /**
     * 업데이트예약 작업구성 매개변수
     * @param queryDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/update")
    AppResponse<Boolean> updateTriggerTask(@Valid @RequestBody UpdateTaskDto queryDto) throws NoLoginException {
        return triggerTaskService.updateTriggerTask(queryDto);
    }

    /**
     * 사용, 사용 안 함예약 작업연결
     * @param taskId
     * @return
     * @throws NoLoginException
     */
    @GetMapping("/enable")
    AppResponse<Boolean> enableTriggerTask(String taskId, Integer enable) throws NoLoginException {
        return triggerTaskService.enableTriggerTask(taskId, enable);
    }

    /**
     * 예약 작업목록분조회연결 - 프론트엔드요청 
     * @param queryDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/page/list")
    AppResponse<IPage<TaskPageVo>> triggerTaskPage(@Valid @RequestBody TaskPageDto queryDto) throws NoLoginException {
        return triggerTaskService.triggerTaskPage(queryDto);
    }

    /**
     * 예약 작업목록분조회연결 - 본트리거기기요청 (pageSize 로100가능)
     * @param queryDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/page/list4Trigger")
    AppResponse<IPage<TaskPage4TriggerVo>> triggerTaskPage4Trigger(@Valid @RequestBody TaskPageDto queryDto)
            throws NoLoginException {
        return triggerTaskService.triggerTaskPage4Trigger(queryDto);
    }
}