package com.iflytek.rpa.task.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iflytek.rpa.task.entity.ScheduleTaskExecute;
import com.iflytek.rpa.task.entity.dto.ScheduleTaskRecordDto;
import com.iflytek.rpa.task.entity.dto.TaskExecuteDto;
import com.iflytek.rpa.task.entity.vo.TaskRecordListVo;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 예약 작업실행기록(ScheduleTaskExecute)테이블데이터베이스방문
 *
 * @author mjren
 * @since 2024-10-15 14:59:09
 */
@Mapper
public interface ScheduleTaskExecuteDao extends BaseMapper<ScheduleTaskExecute> {

    void insertExecuteRecord(ScheduleTaskExecute scheduleTaskExecute);

    Integer countExecuteRecord(@Param("taskExecuteId") String executeId);

    Integer getMaxBatch(@Param("taskId") String taskId);

    Integer updateExecuteStatus(@Param("entity") TaskExecuteDto taskExecuteDto);

    IPage<TaskExecuteDto> getTaskExecuteRecordList(
            IPage<TaskExecuteDto> pageConfig, @Param("entity") TaskExecuteDto executeDto);

    /**
     * 분조회예약 작업실행기록목록
     *
     * @param pageConfig 분매칭
     * @param recordDto  조회매개변수
     * @return 분결과
     */
    IPage<TaskRecordListVo> getTaskRecordList(
            IPage<TaskRecordListVo> pageConfig, @Param("dto") ScheduleTaskRecordDto recordDto);

    /**
     * 량삭제예약 작업실행기록
     *
     * @param taskExecuteIdList 작업실행ID목록
     * @param userId            사용자ID
     * @param tenantId          테넌트ID
     * @return 삭제의기록데이터
     */
    Integer batchDeleteByTaskExecuteIds(
            @Param("taskExecuteIdList") List<String> taskExecuteIdList,
            @Param("userId") String userId,
            @Param("tenantId") String tenantId);

    /**
     * 조회시간 초과의실행기록
     * 가져오기상태로executing시작 시간예약의기록
     *
     * @param timeoutTime 시간 초과시간
     * @return 시간 초과의실행기록목록
     */
    List<ScheduleTaskExecute> getTimeoutExecutingRecords(@Param("timeoutTime") Date timeoutTime);

    /**
     * 근거ID목록업데이트실행기록로가져오기 상태
     *
     * @param idList 기록ID목록
     * @return 업데이트의기록데이터
     */
    Integer updateExecutingRecordsToCancelByIds(@Param("idList") List<Long> idList);
}