package com.iflytek.rpa.dispatch.controller;

import com.iflytek.rpa.dispatch.entity.vo.TerminalTaskDetailVo;
import com.iflytek.rpa.dispatch.service.DispatchTaskService;
import com.iflytek.rpa.utils.response.AppResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 스케줄링관리관리-예약 작업
 *
 * @author jqfang
 * @since 2025-08-15
 */
@RestController
@RequestMapping("dispatch-task")
@Slf4j
public class DispatchTaskController {

    @Autowired
    private DispatchTaskService dispatchTaskService;

    /**
     * 문의조회지정단말여부있음작업업데이트
     *
     * @param terminalId 단말ID
     * @return true테이블있음데이터업데이트, false테이블데이터 없음업데이트
     */
    @GetMapping("/poll-task-update")
    public AppResponse<Boolean> pollTaskUpdate(@RequestParam("terminalId") String terminalId) {
        boolean hasUpdate = dispatchTaskService.checkTaskUpdate(terminalId);
        return AppResponse.success(hasUpdate);
    }

    /**
     * 가져오기단말작업
     *
     * @param terminalId 단말ID
     * @return 단말작업
     */
    @GetMapping("/terminal-task-detail")
    public AppResponse<TerminalTaskDetailVo> getTerminalTaskDetail(@RequestParam("terminalId") String terminalId) {
        return dispatchTaskService.getTerminalTaskDetail(terminalId);
    }
}