package com.iflytek.rpa.dispatch.service;

import com.iflytek.rpa.dispatch.entity.vo.TerminalTaskDetailVo;
import com.iflytek.rpa.utils.response.AppResponse;

public interface DispatchTaskService {
    /**
     * 가져오기단말작업
     *
     * @param terminalId 단말ID
     * @return 단말작업
     */
    AppResponse<TerminalTaskDetailVo> getTerminalTaskDetail(String terminalId);

    /**
     * 문의조회지정단말여부있음작업업데이트
     *
     * @param terminalId 단말ID
     * @return true테이블있음데이터업데이트, false테이블데이터 없음업데이트
     */
    boolean checkTaskUpdate(String terminalId);
}