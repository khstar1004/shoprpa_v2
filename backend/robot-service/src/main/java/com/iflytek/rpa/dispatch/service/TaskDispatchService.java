package com.iflytek.rpa.dispatch.service;

import com.iflytek.rpa.dispatch.entity.TaskDispatchEvent;
import com.iflytek.rpa.dispatch.entity.dto.TaskDispatchDto;
import com.iflytek.rpa.dispatch.entity.enums.DispatchTaskFromType;
import com.iflytek.rpa.dispatch.entity.enums.DispatchTaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 파일게시서비스
 * 게시파일및비고해제게시파일
 */
@Slf4j
@Service
public class TaskDispatchService {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 게시작업분파일
     */
    public void dispatchTask(TaskDispatchDto taskDispatchDto) {
        log.info("게시작업분파일: {}", taskDispatchDto);

        DispatchTaskType dispatchTaskType = DispatchTaskType.valueOf(taskDispatchDto.getDispatchTaskType());
        DispatchTaskFromType dispatchTaskFromType =
                DispatchTaskFromType.valueOf(taskDispatchDto.getDispatchTaskFromType());

        TaskDispatchEvent taskDispatchEvent = new TaskDispatchEvent(
                this,
                taskDispatchDto.getDispatchTaskId(),
                dispatchTaskType,
                dispatchTaskFromType,
                taskDispatchDto.getTerminalIds());

        // 게시파일
        eventPublisher.publishEvent(taskDispatchEvent);
    }
}