package com.iflytek.rpa.dispatch.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.dispatch.entity.DispatchTask;
import com.iflytek.rpa.dispatch.entity.vo.TerminalTaskDetailVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 스케줄링방식-예약 작업 Mapper 연결
 *
 * @author jqfang
 * @since 2025-08-15
 */
@Mapper
public interface DispatchTaskDao extends BaseMapper<DispatchTask> {
    /**
     * 근거단말ID조회작업정보
     *
     * @param terminalId 단말ID
     * @return 작업정보목록
     */
    List<TerminalTaskDetailVo.DispatchTaskInfo> selectTaskInfoByTerminalId(@Param("terminalId") String terminalId);
    /**
     * 근거작업ID조회작업
     *
     * @param taskId 작업ID
     * @return 작업정보
     */
    TerminalTaskDetailVo.DispatchTaskInfo selectTaskInfoByTaskId(@Param("taskId") String taskId);
    /**
     * 근거작업ID목록 량조회봇정보
     *
     * @param taskIds 작업ID목록
     * @return 봇정보목록
     */
    List<TerminalTaskDetailVo.DispatchRobotInfo> selectRobotInfoByTaskIds(@Param("taskIds") List<String> taskIds);
}