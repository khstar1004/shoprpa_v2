package com.iflytek.rpa.dispatch.listener;

import com.iflytek.rpa.dispatch.entity.RedisListBo;
import com.iflytek.rpa.dispatch.entity.TaskDispatchEvent;
import com.iflytek.rpa.dispatch.entity.enums.DispatchTaskFromType;
import com.iflytek.rpa.dispatch.entity.enums.DispatchTaskType;
import com.iflytek.rpa.utils.RedisKeyUtils;
import com.iflytek.rpa.utils.RedisUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 작업스케줄링파일기기
 */
@Slf4j
@Component
public class TaskDispatchEventListener {

    @EventListener
    public void handleTaskDispatched(TaskDispatchEvent event) {
        log.info("까지작업스케줄링파일: {}", event);

        // 를파일정보저장까지Redis중
        // 1, 에서예약 작업목록 의변수변경
        //     a. 작업, 트리거방식있음통신경과실행버튼아래발송, 원인추가까지큐
        //     b. 예약및예약 작업, 원인로예작업변수필요아래발송, 원인식별자테이블여부있음변수변경
        // 2, 에서실행기록목록 의재시도및결과
        //     a. 아니요분작업유형, 가능으로를모든작업통신경과재시도버튼직선연결까지큐, 매개변수에서실행기록가져오기
        List<String> terminalIds = event.getDispatchTerminalIds();
        if (terminalIds != null && !terminalIds.isEmpty()) {
            if (event.getDispatchTaskFromType() == DispatchTaskFromType.NORMAL) {
                for (String terminalId : terminalIds) {
                    if (event.getDispatchTaskType() == DispatchTaskType.MANUAL) {
                        String redisKey = RedisKeyUtils.getDispatchTaskListKey(terminalId);
                        // 를dispatchTaskId추가까지큐(list)중
                        RedisUtils.lSet(
                                redisKey, new RedisListBo(event.getDispatchTaskId(), event.getDispatchTaskFromType()));
                        log.info("완료를작업스케줄링파일의taskId추가까지Redis큐: key={}, value={}", redisKey, event.getDispatchTaskId());
                    } else {
                        String redisKey = RedisKeyUtils.getDispatchTaskStatusKey(terminalId);
                        // Redis key, value로"1", 아니요경과
                        RedisUtils.set(redisKey, "1");
                        log.info("완료를예약작업스케줄링파일저장까지Redis: key={}, value=1", redisKey);
                    }
                }
            } else {
                for (String terminalId : terminalIds) {
                    String redisKey = RedisKeyUtils.getDispatchTaskListKey(terminalId);
                    RedisUtils.lSet(
                            redisKey, new RedisListBo(event.getDispatchTaskId(), event.getDispatchTaskFromType()));
                    log.info("완료를재시도작업스케줄링파일의taskId추가까지Redis큐: key={}", redisKey);
                }
            }
        }
    }
}