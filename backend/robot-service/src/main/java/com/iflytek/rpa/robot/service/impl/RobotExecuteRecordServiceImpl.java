package com.iflytek.rpa.robot.service.impl;

import static com.iflytek.rpa.robot.constants.RobotConstant.ROBOT_RESULT_EXECUTE;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.base.annotation.RobotVersionAnnotation;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.dispatch.entity.dto.RobotExecuteStatusDto;
import com.iflytek.rpa.dispatch.service.DispatchTaskExecuteRecordService;
import com.iflytek.rpa.monitor.entity.RobotMonitorDto;
import com.iflytek.rpa.robot.dao.RobotExecuteDao;
import com.iflytek.rpa.robot.dao.RobotExecuteRecordDao;
import com.iflytek.rpa.robot.dao.RobotVersionDao;
import com.iflytek.rpa.robot.entity.RobotExecuteRecord;
import com.iflytek.rpa.robot.entity.dto.ExecuteRecordDto;
import com.iflytek.rpa.robot.entity.dto.RobotExecuteRecordsBatchDeleteDto;
import com.iflytek.rpa.robot.service.HisDataEnumService;
import com.iflytek.rpa.robot.service.RobotExecuteRecordService;
import com.iflytek.rpa.task.dao.ScheduleTaskDao;
import com.iflytek.rpa.utils.DateUtils;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.NumberUtils;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 단말봇실행기록테이블(RobotExecute)테이블서비스유형
 *
 * @author makejava
 * @since 2024-09-29 15:27:41
 */
@Slf4j
@Service("robotExecuteRecordService")
public class RobotExecuteRecordServiceImpl extends ServiceImpl<RobotExecuteRecordDao, RobotExecuteRecord>
        implements RobotExecuteRecordService {

    @Autowired
    private RobotExecuteRecordDao robotExecuteRecordDao;

    @Autowired
    private HisDataEnumService hisDataEnumService;

    @Autowired
    private RobotExecuteDao robotExecuteDao;

    @Autowired
    private RobotVersionDao robotVersionDao;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    ScheduleTaskDao scheduleTaskDao;

    @Autowired
    private DispatchTaskExecuteRecordService dispatchTaskExecuteRecordService;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    public AppResponse<?> recordList(ExecuteRecordDto recordDto) throws NoLoginException {
        IPage<RobotExecuteRecord> pages = new Page<>();
        if (null == recordDto.getPageNo() || null == recordDto.getPageSize()) {
            return AppResponse.success(pages);
        }
        AppResponse<User> resp = rpaAuthFeign.getLoginUser();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = resp.getData();
        String userId = loginUser.getId();

        recordDto.setCreatorId(userId);
        AppResponse<String> res = rpaAuthFeign.getTenantId();
        if (res == null || res.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = res.getData();
        recordDto.setTenantId(tenantId);
        IPage<RobotExecuteRecord> pageConfig = new Page<>(recordDto.getPageNo(), recordDto.getPageSize(), true);
        pages = robotExecuteRecordDao.getExecuteRecordList(pageConfig, recordDto);
        List<RobotExecuteRecord> list = pages.getRecords();
        if (list.isEmpty()) {
            AppResponse.success(pages);
        }
        packageTaskInfo(list);
        return AppResponse.success(pages);
    }

    private void packageTaskInfo(List<RobotExecuteRecord> list) {
        for (RobotExecuteRecord robotExecuteRecord : list) {
            String taskExecuteId = robotExecuteRecord.getTaskExecuteId();
            if (!StringUtils.isEmpty(taskExecuteId)) {
                String taskName = scheduleTaskDao.getTaskNameByTaskExecuteId(taskExecuteId);
                if (taskName != null && !StringUtils.isEmpty(taskName)) {
                    robotExecuteRecord.setTaskName(taskName);
                } else {
                    robotExecuteRecord.setTaskName(null);
                }
            }
        }
    }

    @Override
    public AppResponse<?> getExecuteLog(ExecuteRecordDto recordDto) throws NoLoginException {
        String executeId = recordDto.getExecuteId();
        if (null == executeId) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "실행ID비어 있습니다");
        }
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || response.getData() == null) {
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
        recordDto.setTenantId(tenantId);
        String executeLog = robotExecuteRecordDao.getExecuteLog(recordDto);
        return AppResponse.success(executeLog);
    }

    @Override
    public AppResponse<?> robotOverview(RobotMonitorDto robotMonitorDto) {

        //        Date date = DateUtil.parse(deadline);
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        if (null == tenantId) {
            return AppResponse.error(ErrorCodeEnum.E_SQL, "테넌트 정보 조회 실패");
        }
        String robotId = robotMonitorDto.getRobotId();
        // 오늘의및의필요시시스템계획, 원인로테이블저장계획데이터, 저장완료매일데이터
        Date countTime = DateUtils.getEndOfDay(robotMonitorDto.getDeadline());
        RobotMonitorDto robotMonitorData =
                robotExecuteRecordDao.robotOverview(tenantId, robotId, countTime, robotMonitorDto.getVersion());
        robotMonitorData.setExecuteSuccessRate(NumberUtils.getRate(
                new BigDecimal(robotMonitorData.getExecuteSuccess()),
                new BigDecimal(robotMonitorData.getExecuteTotal())));
        robotMonitorData.setExecuteFailRate(NumberUtils.getRate(
                new BigDecimal(robotMonitorData.getExecuteFail()), new BigDecimal(robotMonitorData.getExecuteTotal())));
        robotMonitorData.setExecuteAbortRate(NumberUtils.getRate(
                new BigDecimal(robotMonitorData.getExecuteAbort()),
                new BigDecimal(robotMonitorData.getExecuteTotal())));
        robotMonitorData.setExecuteRunningRate(NumberUtils.getRate(
                new BigDecimal(robotMonitorData.getExecuteRunning()),
                new BigDecimal(robotMonitorData.getExecuteTotal())));
        return AppResponse.success(
                hisDataEnumService.getOverViewData("robotOverview", robotMonitorData, RobotMonitorDto.class));
    }

    @Override
    @RobotVersionAnnotation(clazz = ExecuteRecordDto.class)
    public AppResponse<?> saveExecuteResult(ExecuteRecordDto recordDto, String currentRobotId) throws NoLoginException {
        // 여부로dispatch방식
        if (recordDto.getIsDispatch() != null && recordDto.getIsDispatch()) {
            // 호출dispatch서비스
            RobotExecuteStatusDto statusDto = new RobotExecuteStatusDto();
            statusDto.setExecuteId(recordDto.getExecuteId() != null ? Long.valueOf(recordDto.getExecuteId()) : null);
            statusDto.setRobotId(recordDto.getRobotId());
            statusDto.setRobotVersion(recordDto.getRobotVersion());
            statusDto.setDispatchTaskExecuteId(recordDto.getDispatchTaskExecuteId());
            statusDto.setTerminalId(recordDto.getTerminalId());
            statusDto.setResult(recordDto.getResult());
            statusDto.setError_reason(recordDto.getError_reason());
            statusDto.setExecuteLog(recordDto.getExecuteLog());
            statusDto.setVideoLocalPath(recordDto.getVideoLocalPath());
            statusDto.setParamJson(recordDto.getParamJson());

            return dispatchTaskExecuteRecordService.reportRobotStatus(statusDto);
        }

        // 기존있음서비스
        String executeId = recordDto.getExecuteId();
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || response.getData() == null) {
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
        recordDto.setRobotId(currentRobotId);

        AppResponse<String> currentLevelCodeRes = rpaAuthFeign.getCurrentLevelCode();
        if (!currentLevelCodeRes.ok()) throw new ServiceException("rpa-auth 서비스가 준비되지 않았습니다");
        String deptIdPath = currentLevelCodeRes.getData();
        recordDto.setDeptIdPath(deptIdPath);
        // 근거executeId, 여부예일, 예일, 시작 시간
        if (null == executeId) {
            if (null == recordDto.getResult() || !ROBOT_RESULT_EXECUTE.equals(recordDto.getResult())) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "실행 결과비어 있습니다또는데이터오류");
            }
            executeId = idWorker.nextId() + "";
            recordDto.setExecuteId(executeId);
            recordDto.setStartTime(new Date());
            // 삽입
            robotExecuteRecordDao.insertExecuteRecord(recordDto);
        } else {
            if (null == recordDto.getResult() || ROBOT_RESULT_EXECUTE.equals(recordDto.getResult())) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM, "실행 결과오류");
            }
            RobotExecuteRecord executeRecord = robotExecuteRecordDao.getExecuteRecord(recordDto);
            if (null == executeRecord || null == executeRecord.getStartTime()) {
                return AppResponse.error(ErrorCodeEnum.E_SQL, "실행기록데이터예외");
            }

            Date endTime = new Date();
            recordDto.setEndTime(endTime);
            // 계획실행시
            recordDto.setExecuteTime(endTime.toInstant().getEpochSecond()
                    - executeRecord.getStartTime().toInstant().getEpochSecond());
            robotExecuteRecordDao.updateExecuteRecord(recordDto);
        }
        return AppResponse.success(executeId);
    }

    @Override
    public AppResponse<String> deleteRobotExecuteRecords(RobotExecuteRecordsBatchDeleteDto batchDeleteDto)
            throws NoLoginException {
        // 량삭제봇실행기록
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || response.getData() == null) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        List<String> recordsIds = batchDeleteDto.getRecordIds();
        recordsIds.removeIf(Objects::isNull);
        if (recordsIds.isEmpty()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK, "작업실행ID목록은 비워 둘 수 없습니다");
        }
        // 2. 량삭제
        int deleted = baseMapper.deleteRobotExecuteRecords(recordsIds, userId, tenantId);
        if (deleted != recordsIds.size()) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EXCEPTION.getCode(), "량삭제공유파일실패");
        }
        return AppResponse.success("삭제성공");
    }
}