package com.iflytek.rpa.dispatch.service.impl;

import static com.iflytek.rpa.robot.constants.RobotConstant.ROBOT_RESULT_EXECUTE;
import static com.iflytek.rpa.task.constants.TaskConstant.TASK_RESULT_EXECUTE;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.dispatch.dao.DispatchTaskExecuteRecordDao;
import com.iflytek.rpa.dispatch.entity.DispatchTask;
import com.iflytek.rpa.dispatch.entity.DispatchTaskExecuteRecord;
import com.iflytek.rpa.dispatch.entity.DispatchTaskRobotExecuteRecord;
import com.iflytek.rpa.dispatch.entity.dto.RobotExecuteStatusDto;
import com.iflytek.rpa.dispatch.entity.dto.TaskExecuteStatusDto;
import com.iflytek.rpa.dispatch.service.DispatchTaskExecuteRecordService;
import com.iflytek.rpa.robot.entity.vo.RecordLogVo;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.time.Duration;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DispatchTaskExecuteRecordServiceImpl
        extends ServiceImpl<DispatchTaskExecuteRecordDao, DispatchTaskExecuteRecord>
        implements DispatchTaskExecuteRecordService {

    @Autowired
    private IdWorker idWorker;

    @Value("${deBounce.prefix}")
    private String doBouncePrefix; // 전

    @Value("${deBounce.window}")
    private Long deBounceWindow; // 창

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    public AppResponse<String> reportTaskStatus(TaskExecuteStatusDto statusDto) throws NoLoginException {
        // 완료executeId
        String terminalId = statusDto.getTerminalId();
        if (terminalId == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "단말ID비워 둘 수 없습니다");
        }
        DispatchTask task = baseMapper.selectTaskById(statusDto.getDispatchTaskId());
        if (task == null) {
            return AppResponse.error(ErrorCodeEnum.E_SQL, "예약 작업데이터예외");
        }
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        Long taskExecuteId = statusDto.getDispatchTaskExecuteId();
        if (null == taskExecuteId) {
            // 일실행
            taskExecuteId = idWorker.nextId();
            DispatchTaskExecuteRecord taskExecuteRecord = new DispatchTaskExecuteRecord();
            taskExecuteRecord.setDispatchTaskId(statusDto.getDispatchTaskId());
            taskExecuteRecord.setDispatchTaskExecuteId(taskExecuteId);
            taskExecuteRecord.setDispatchTaskType(task.getType());
            taskExecuteRecord.setResult(TASK_RESULT_EXECUTE);
            taskExecuteRecord.setTenantId(tenantId);
            taskExecuteRecord.setCreatorId(userId);
            taskExecuteRecord.setTerminalId(terminalId);
            taskExecuteRecord.setUpdaterId(userId);
            taskExecuteRecord.setStartTime(new Date());
            // 가져오기 대
            Integer maxBatch = baseMapper.getMaxBatch(statusDto.getDispatchTaskId());
            if (null == maxBatch || 0 == maxBatch) {
                taskExecuteRecord.setCount(1);
            } else {
                taskExecuteRecord.setCount(maxBatch + 1);
            }
            // 삽입
            baseMapper.insertTaskExecuteRecord(taskExecuteRecord);
            // 반환실행id
            return AppResponse.success(taskExecuteId + "");
        }
        DispatchTaskExecuteRecord record = baseMapper.selectByExecuteId(taskExecuteId);
        //        Integer executeCount = baseMapper.countExecuteRecord(taskExecuteId);
        if (record == null) {
            return AppResponse.error(ErrorCodeEnum.E_SQL, "예약 작업실행기록데이터예외");
        }
        // 아니요예일실행, 업데이트실행상태
        if (null == statusDto.getResult()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "예약 작업실행 결과비워 둘 수 없습니다");
        }
        //        if(null == statusDto.getTaskDetailJson() || "".equals(statusDto.getTaskDetailJson())){
        //            return AppResponse.error(ErrorCodeEnum.E_PARAM, "요청 입력예약 작업");
        //        }
        Date startTime = record.getStartTime();
        Date endTime = new Date();
        statusDto.setEndTime(endTime);
        Duration duration = Duration.between(startTime.toInstant(), endTime.toInstant());
        statusDto.setExecuteTime(duration.getSeconds());
        baseMapper.updateTaskExecuteStatus(statusDto);
        return AppResponse.success(taskExecuteId + "");
    }

    @Override
    public AppResponse<String> reportRobotStatus(RobotExecuteStatusDto recordDto) throws NoLoginException {
        Long executeId = recordDto.getExecuteId();
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        recordDto.setCreatorId(userId);
        recordDto.setUpdaterId(userId);
        recordDto.setTenantId(tenantId);

        AppResponse<String> currentLevelCodeRes = rpaAuthFeign.getCurrentLevelCode();
        if (!currentLevelCodeRes.ok()) throw new ServiceException("rpa-auth 서비스가 준비되지 않았습니다");
        String deptIdPath = currentLevelCodeRes.getData();
        recordDto.setDeptIdPath(deptIdPath);
        // 근거executeId, 여부예일, 예일, 시작 시간
        if (null == executeId) {
            if (null == recordDto.getResult() || !ROBOT_RESULT_EXECUTE.equals(recordDto.getResult())) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "실행 결과비어 있습니다또는데이터오류");
            }
            executeId = idWorker.nextId();
            recordDto.setExecuteId(executeId);
            recordDto.setStartTime(new Date());
            // 삽입
            baseMapper.insertRobotExecuteRecord(recordDto);
            return AppResponse.success(executeId + "");
        } else {
            if (null == recordDto.getResult() || ROBOT_RESULT_EXECUTE.equals(recordDto.getResult())) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM, "실행 결과오류");
            }
            DispatchTaskRobotExecuteRecord executeRecord = baseMapper.getRobotExecuteRecord(recordDto);
            if (null == executeRecord || null == executeRecord.getStartTime()) {
                return AppResponse.error(ErrorCodeEnum.E_SQL, "실행기록데이터예외");
            }

            Date endTime = new Date();
            recordDto.setEndTime(endTime);
            // 계획실행시
            recordDto.setExecuteTime(endTime.toInstant().getEpochSecond()
                    - executeRecord.getStartTime().toInstant().getEpochSecond());
            baseMapper.updateRobotExecuteRecord(recordDto);
        }
        return AppResponse.success("작업결과");
    }

    @Override
    public AppResponse<RecordLogVo> getRobotExecuteLog(Long executeId) throws NoLoginException {
        if (executeId == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "봇실행ID비워 둘 수 없습니다");
        }

        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        RecordLogVo executeLog = baseMapper.getRobotExecuteLog(executeId, tenantId);

        if (executeLog == null) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EMPTY, "조회하지 못한해당봇실행기록");
        }

        return AppResponse.success(executeLog);
    }
}