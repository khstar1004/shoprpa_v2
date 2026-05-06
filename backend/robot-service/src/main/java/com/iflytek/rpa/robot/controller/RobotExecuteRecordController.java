package com.iflytek.rpa.robot.controller;

import com.iflytek.rpa.dispatch.service.DispatchTaskExecuteRecordService;
import com.iflytek.rpa.monitor.entity.RobotMonitorDto;
import com.iflytek.rpa.robot.entity.dto.ExecuteRecordDto;
import com.iflytek.rpa.robot.entity.dto.RobotExecuteRecordsBatchDeleteDto;
import com.iflytek.rpa.robot.service.RobotExecuteRecordService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 단말봇실행기록테이블(RobotExecute)테이블제어
 *
 * @author makejava
 * @since 2024-09-29 15:27:41
 */
@RestController
@RequestMapping("/robot-record")
public class RobotExecuteRecordController {

    @Autowired
    private RobotExecuteRecordService robotExecuteRecordService;

    @Autowired
    private DispatchTaskExecuteRecordService dispatchTaskExecuteRecordService;

    /**
     * 실행기록목록
     * @param recordDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/list")
    public AppResponse<?> recordList(@RequestBody ExecuteRecordDto recordDto) throws NoLoginException {
        return robotExecuteRecordService.recordList(recordDto);
    }

    /**
     * 조회실행로그
     * @param recordDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/log")
    public AppResponse<?> getExecuteLog(@RequestBody ExecuteRecordDto recordDto) throws NoLoginException {
        if (recordDto.getIsDispatch()) {
            return dispatchTaskExecuteRecordService.getRobotExecuteLog(Long.valueOf(recordDto.getExecuteId()));
        }
        return robotExecuteRecordService.getExecuteLog(recordDto);
    }

    /**
     * 실행기기-봇-실행
     * @paramMarketResourceDto
     * @return
     * @throws Exception
     */
    @PostMapping("/detail/overview")
    public AppResponse<?> getOverViewData(@RequestBody RobotMonitorDto robotMonitorDto) {
        return robotExecuteRecordService.robotOverview(robotMonitorDto);
    }

    /**
     * 업로드봇실행 결과
     * @param recordDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/save-result")
    public AppResponse<?> saveExecuteResult(@RequestBody ExecuteRecordDto recordDto) throws NoLoginException {
        String currentRobotId = recordDto.getRobotId();
        return robotExecuteRecordService.saveExecuteResult(recordDto, currentRobotId);
    }

    /**
     * 량삭제봇실행기록
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/delete-robot-execute-records")
    public AppResponse<String> deleteRobotExecuteRecords(@RequestBody RobotExecuteRecordsBatchDeleteDto batchDeleteDto)
            throws NoLoginException {
        return robotExecuteRecordService.deleteRobotExecuteRecords(batchDeleteDto);
    }
}