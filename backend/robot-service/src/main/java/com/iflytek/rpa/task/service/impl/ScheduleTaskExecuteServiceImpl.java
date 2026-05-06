package com.iflytek.rpa.task.service.impl;

import static com.iflytek.rpa.task.constants.TaskConstant.TASK_RESULT_EXECUTE;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.dispatch.entity.dto.TaskExecuteStatusDto;
import com.iflytek.rpa.dispatch.service.DispatchTaskExecuteRecordService;
import com.iflytek.rpa.robot.dao.RobotExecuteRecordDao;
import com.iflytek.rpa.robot.entity.RobotExecuteRecord;
import com.iflytek.rpa.task.dao.ScheduleTaskExecuteDao;
import com.iflytek.rpa.task.entity.ScheduleTaskExecute;
import com.iflytek.rpa.task.entity.dto.ScheduleTaskRecordDeleteDto;
import com.iflytek.rpa.task.entity.dto.ScheduleTaskRecordDto;
import com.iflytek.rpa.task.entity.dto.TaskExecuteDto;
import com.iflytek.rpa.task.entity.vo.TaskRecordListVo;
import com.iflytek.rpa.task.service.ScheduleTaskExecuteService;
import com.iflytek.rpa.utils.DateUtils;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * 예약 작업실행기록(ScheduleTaskExecute)테이블서비스유형
 *
 * @author mjren
 * @since 2024-10-15 14:59:09
 */
@Slf4j
@Service("scheduleTaskExecuteService")
public class ScheduleTaskExecuteServiceImpl extends ServiceImpl<ScheduleTaskExecuteDao, ScheduleTaskExecute>
        implements ScheduleTaskExecuteService {
    @Resource
    private ScheduleTaskExecuteDao scheduleTaskExecuteDao;

    @Autowired
    private RobotExecuteRecordDao robotExecuteRecordDao;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private DispatchTaskExecuteRecordService dispatchTaskExecuteRecordService;

    /**
     * 작업실행시간 초과시간(시간), 24시간
     */
    @Value("${schedule.task.timeout.hours:24}")
    private Integer taskTimeoutHours;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    public AppResponse<?> setTaskExecuteStatus(TaskExecuteDto executeDto) throws NoLoginException {
        // 여부로dispatch방식
        if (executeDto.getIsDispatch() != null && executeDto.getIsDispatch()) {
            // 호출dispatch서비스
            TaskExecuteStatusDto statusDto = new TaskExecuteStatusDto();
            statusDto.setDispatchTaskId(executeDto.getDispatchTaskId());
            statusDto.setDispatchTaskExecuteId(executeDto.getDispatchTaskExecuteId());
            statusDto.setTerminalId(executeDto.getTerminalId());
            statusDto.setResult(executeDto.getResult());

            return dispatchTaskExecuteRecordService.reportTaskStatus(statusDto);
        }

        // 기존있음서비스
        // 완료executeId
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
        String taskExecuteId = executeDto.getTaskExecuteId();
        if (null == executeDto.getTaskId()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "작업ID비워 둘 수 없습니다");
        }
        if (null == taskExecuteId) {
            // 일실행
            taskExecuteId = idWorker.nextId() + "";
            ScheduleTaskExecute scheduleTaskExecute = new ScheduleTaskExecute();
            scheduleTaskExecute.setTaskId(executeDto.getTaskId());
            scheduleTaskExecute.setTaskExecuteId(taskExecuteId);
            scheduleTaskExecute.setResult(TASK_RESULT_EXECUTE);
            scheduleTaskExecute.setTenantId(tenantId);
            scheduleTaskExecute.setCreatorId(userId);
            scheduleTaskExecute.setUpdaterId(userId);
            scheduleTaskExecute.setStartTime(new Date());
            // 가져오기 대
            Integer maxBatch = scheduleTaskExecuteDao.getMaxBatch(executeDto.getTaskId());
            if (null == maxBatch || 0 == maxBatch) {
                scheduleTaskExecute.setCount(1);
            } else {
                scheduleTaskExecute.setCount(maxBatch + 1);
            }
            // 삽입
            scheduleTaskExecuteDao.insertExecuteRecord(scheduleTaskExecute);
            // 반환실행id
            return AppResponse.success(taskExecuteId);
        }
        Integer executeCount = scheduleTaskExecuteDao.countExecuteRecord(taskExecuteId);
        if (executeCount < 1) {
            return AppResponse.error(ErrorCodeEnum.E_SQL, "계획작업실행기록데이터예외");
        }
        // 아니요예일실행, 업데이트실행상태
        if (null == executeDto.getResult()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "예약 작업실행 결과비워 둘 수 없습니다");
        }
        if (null == executeDto.getEndTime()) {
            executeDto.setEndTime(new Date());
        }
        scheduleTaskExecuteDao.updateExecuteStatus(executeDto);
        return AppResponse.success(taskExecuteId);
    }

    @Override
    public AppResponse<?> getTaskExecuteRecordList(TaskExecuteDto executeDto) throws NoLoginException {
        // 예약 작업기록
        IPage<TaskExecuteDto> pages = new Page<>();
        if (null == executeDto.getPageNo() || null == executeDto.getPageSize()) {
            return AppResponse.success(pages);
        }
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        executeDto.setCreatorId(userId);
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        executeDto.setTenantId(tenantId);
        IPage<TaskExecuteDto> pageConfig = new Page<>(executeDto.getPageNo(), executeDto.getPageSize(), true);
        pages = scheduleTaskExecuteDao.getTaskExecuteRecordList(pageConfig, executeDto);
        List<TaskExecuteDto> taskList = pages.getRecords();
        if (CollectionUtils.isEmpty(taskList)) {
            return AppResponse.success(pages);
        }
        // 순서,에서1까지n
        long offset = pageConfig.offset() + 1;
        for (int i = 0; i < taskList.size(); i++) {
            taskList.get(i).setId(offset + i);
        }
        // 가져오기실행id목록, 조회실행기록
        List<String> executeIdList =
                taskList.stream().map(TaskExecuteDto::getTaskExecuteId).collect(Collectors.toList());
        executeIdList.removeIf(executeId -> null == executeId || executeId.isEmpty());
        if (CollectionUtils.isEmpty(executeIdList)) {
            return AppResponse.success(pages);
        }
        List<RobotExecuteRecord> executeRecordList = robotExecuteRecordDao.getRecordByExecuteIdList(executeIdList);
        // 근거실행id분그룹, 시작 시간정상순서정렬
        Map<String, List<RobotExecuteRecord>> executeRecordMap = executeRecordList.stream()
                .collect(Collectors.groupingBy(RobotExecuteRecord::getTaskExecuteId, Collectors.toList()));
        for (TaskExecuteDto task : taskList) {
            List<RobotExecuteRecord> executeRecordListByExecuteId = executeRecordMap.get(task.getTaskExecuteId());
            if (CollectionUtils.isEmpty(executeRecordListByExecuteId)) {
                continue;
            }
            // 시작 시간정상순서정렬
            executeRecordListByExecuteId.sort(Comparator.comparing(RobotExecuteRecord::getStartTime));
            task.setRobotExecuteRecordList(executeRecordListByExecuteId);
        }

        return AppResponse.success(pages);
    }

    @Override
    public AppResponse<IPage<TaskRecordListVo>> getRecordList(ScheduleTaskRecordDto recordDto) throws NoLoginException {
        // 매개변수검증
        if (recordDto.getPageNo() == null || recordDto.getPageSize() == null) {
            return AppResponse.success(new Page<>());
        }

        // 가져오기사용자및테넌트 정보
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
        recordDto.setUserId(userId);
        recordDto.setTenantId(tenantId);

        // 분객체
        IPage<TaskRecordListVo> pageConfig = new Page<>(recordDto.getPageNo(), recordDto.getPageSize());

        // 호출DAO조회
        IPage<TaskRecordListVo> pages = baseMapper.getTaskRecordList(pageConfig, recordDto);

        List<TaskRecordListVo> taskList = pages.getRecords();
        if (CollectionUtils.isEmpty(taskList)) {
            return AppResponse.success(pages);
        }

        // 가져오기실행ID목록, 조회봇실행기록
        List<String> taskExecuteIdList = taskList.stream()
                .map(TaskRecordListVo::getTaskExecuteId)
                .filter(executeId -> executeId != null && !executeId.isEmpty())
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(taskExecuteIdList)) {
            // 조회봇실행기록
            List<RobotExecuteRecord> executeRecordList =
                    robotExecuteRecordDao.getRecordByExecuteIdList(taskExecuteIdList);

            // 를봇실행기록분그룹까지의작업기록중
            if (!CollectionUtils.isEmpty(executeRecordList)) {
                // 작업실행ID분그룹
                Map<String, List<RobotExecuteRecord>> recordMap =
                        executeRecordList.stream().collect(Collectors.groupingBy(RobotExecuteRecord::getTaskExecuteId));

                // 로매개작업기록의봇실행기록
                taskList.forEach(task -> {
                    List<RobotExecuteRecord> robotRecords = recordMap.get(task.getTaskExecuteId());
                    if (robotRecords != null) {
                        task.setRobotExecuteRecordList(robotRecords);
                    }
                });
            }
        }

        return AppResponse.success(pages);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> batchDelete(ScheduleTaskRecordDeleteDto dto) throws NoLoginException {
        List<String> taskExecuteIdList = dto.getTaskExecuteIdList();
        taskExecuteIdList.removeIf(Objects::isNull);
        if (CollectionUtils.isEmpty(taskExecuteIdList)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK, "작업실행ID목록은 비워 둘 수 없습니다");
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

        // 1. 삭제 schedule_task_execute 테이블중의기록
        Integer scheduleDeleted =
                scheduleTaskExecuteDao.batchDeleteByTaskExecuteIds(taskExecuteIdList, userId, tenantId);

        // 2. 삭제 robot_execute_record 테이블중의기록
        Integer robotDeleted = robotExecuteRecordDao.batchDeleteByTaskExecuteIds(taskExecuteIdList, userId, tenantId);

        return AppResponse.success(String.format("성공삭제 %d 예약 작업실행기록, %d 봇실행기록", scheduleDeleted, robotDeleted));
    }

    /**
     * 예약작업: 매시간실행일, 관리시간 초과의실행기록
     * 를상태로executing시작 시간완료경과지정시간데이터의기록업데이트로cancel상태
     */
    @Scheduled(fixedRate = 3_600_000)
    public void cleanUpTimeoutExecutingRecords() {
        try {
            log.info("열기 실행예약작업: 관리실행시간 초과의실행기록 [시간 초과시간: {} 시간]", taskTimeoutHours);
            // 1. 계획시간 초과시간
            Date currentTime = new Date();
            int timeoutMinutes = taskTimeoutHours * 60;
            Date timeoutTime = DateUtils.getCalMinute(currentTime, -timeoutMinutes);
            // 2. 조회시간 초과의실행기록
            List<ScheduleTaskExecute> timeoutRecords = scheduleTaskExecuteDao.getTimeoutExecutingRecords(timeoutTime);
            timeoutRecords.removeIf(Objects::isNull);
            if (CollectionUtils.isEmpty(timeoutRecords)) {
                return;
            }
            // 3. 가져오기ID목록
            List<Long> idList =
                    timeoutRecords.stream().map(ScheduleTaskExecute::getId).collect(Collectors.toList());
            // 4. 근거ID목록 량업데이트
            Integer updatedCount = scheduleTaskExecuteDao.updateExecutingRecordsToCancelByIds(idList);
            log.info("예약작업실행완료, 공유업데이트완료 {} 시간 초과의실행기록로가져오기 상태", updatedCount);
        } catch (Exception e) {
            log.error("실행관리시간 초과실행기록예약작업시발송예외", e);
        }
    }
}