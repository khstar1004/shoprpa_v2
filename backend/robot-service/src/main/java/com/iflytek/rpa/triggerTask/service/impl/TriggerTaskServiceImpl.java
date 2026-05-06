package com.iflytek.rpa.triggerTask.service.impl;

import static com.iflytek.rpa.robot.constants.RobotConstant.CREATE;
import static com.iflytek.rpa.utils.DeBounceUtils.deBounce;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.rpa.base.entity.dto.ParamDto;
import com.iflytek.rpa.base.entity.dto.QueryParamDto;
import com.iflytek.rpa.base.service.CParamService;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.robot.constants.RobotConstant;
import com.iflytek.rpa.robot.dao.RobotDesignDao;
import com.iflytek.rpa.robot.dao.RobotExecuteDao;
import com.iflytek.rpa.robot.dao.RobotVersionDao;
import com.iflytek.rpa.robot.entity.RobotExecute;
import com.iflytek.rpa.task.dao.ScheduleTaskRobotDao;
import com.iflytek.rpa.task.entity.ScheduleTaskRobot;
import com.iflytek.rpa.task.entity.dto.RobotInfo;
import com.iflytek.rpa.triggerTask.dao.TriggerTaskDao;
import com.iflytek.rpa.triggerTask.entity.TriggerTask;
import com.iflytek.rpa.triggerTask.entity.dto.InsertTaskDto;
import com.iflytek.rpa.triggerTask.entity.dto.TaskPageDto;
import com.iflytek.rpa.triggerTask.entity.dto.UpdateTaskDto;
import com.iflytek.rpa.triggerTask.entity.enums.ExceptionalEnum;
import com.iflytek.rpa.triggerTask.entity.enums.TaskTypeEnum;
import com.iflytek.rpa.triggerTask.entity.vo.*;
import com.iflytek.rpa.triggerTask.service.TriggerTaskService;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service("triggerTaskService")
public class TriggerTaskServiceImpl extends ServiceImpl<TriggerTaskDao, TriggerTask> implements TriggerTaskService {

    @Resource
    private RobotExecuteDao robotExecuteDao;

    @Resource
    private RobotDesignDao robotDesignDao;

    @Resource
    private ScheduleTaskRobotDao scheduleTaskRobotDao;

    @Autowired
    private CParamService paramService;

    @Autowired
    RobotVersionDao robotVersionDao;

    @Autowired
    private IdWorker idWorker;

    @Value("${deBounce.prefix}")
    private String doBouncePrefix; // 전

    @Value("${deBounce.window}")
    private Long deBounceWindow; // 창

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    public AppResponse<Boolean> isTaskNameCopy(String name) throws NoLoginException {
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
        return AppResponse.success(checkNameCopy(name, userId, tenantId));
    }

    /**
     * 조회실행기기모든봇, 패키지봇본정보, 여부, 구성 매개변수
     * @param name
     * @return
     * @throws NoLoginException
     */
    @Override
    public AppResponse<List<Executor>> getRobotExeList(String name) throws NoLoginException, JsonProcessingException {
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
        name = StringUtils.trim(name);
        List<RobotExecute> robotExecuteList = robotExecuteDao.getRobotExecuteByName(name, userId, tenantId);
        packageCreateRobotVersion(robotExecuteList);
        // 결과가로 create 본생성  예있음 version의
        if (CollectionUtils.isEmpty(robotExecuteList)) return AppResponse.success(Collections.EMPTY_LIST); // 결과가비어 있습니다, 직선연결반환

        List<Executor> resVoList = getExecutorList(robotExecuteList);

        return AppResponse.success(resVoList);
    }

    private void packageCreateRobotVersion(List<RobotExecute> robotExecuteList) {
        for (RobotExecute robotExecute : robotExecuteList) {
            String dataSource = robotExecute.getDataSource();
            if (CREATE.equals(dataSource)) {
                robotExecute.setAppVersion(robotExecute.getRobotVersion());
            }
        }
    }

    @Override
    public List<String> getUsingTasksByMail(String mailId) {
        return baseMapper
                .selectList(
                        new LambdaQueryWrapper<TriggerTask>()
                                .eq(TriggerTask::getTaskType, TaskTypeEnum.MAIL_TASK.getCode())
                                .eq(TriggerTask::getDeleted, 0)
                                .like(TriggerTask::getTaskJson, mailId) // 아니요일지정가능, 데이터결과계획아니요
                        )
                .stream()
                .map(TriggerTask::getName)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<Boolean> insertTriggerTask(InsertTaskDto queryDto) throws NoLoginException {
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

        checkInsertParam(queryDto); // 매개변수검증

        // 
        String deBounceRedisKey = doBouncePrefix + tenantId + "-" + userId + "-" + queryDto.getName();
        deBounce(deBounceRedisKey, deBounceWindow);

        if (checkNameCopy(queryDto.getName(), userId, tenantId))
            throw new ServiceException(ErrorCodeEnum.E_SERVICE.getCode(), "명령이름재복사");

        // 삽입triggerTask
        String triggerTaskId = insertTask(queryDto, userId, tenantId);
        // 삽입scheduleTaskRobot
        insertTaskRobot(queryDto, userId, tenantId, triggerTaskId);

        return AppResponse.success(true);
    }

    /**
     * 예약 작업--작업정보돌아가기, 패키지봇본정보, 여부, 구성 매개변수
     * @param taskId
     * @return
     * @throws NoLoginException
     */
    @Override
    public AppResponse<TriggerTaskVo> getTriggerTask(String taskId) throws NoLoginException, JsonProcessingException {
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

        if (StringUtils.isBlank(taskId)) throw new ServiceException(ErrorCodeEnum.E_PARAM_CHECK.getCode(), "매개변수 실패");
        TriggerTask triggerTask = baseMapper.getTaskById(userId, tenantId, taskId);
        if (triggerTask == null) throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode());

        TriggerTaskVo triggerTaskVo = getTriggerTaskVo(triggerTask, userId, tenantId);

        return AppResponse.success(triggerTaskVo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<Boolean> deleteTriggerTask(String taskId) throws NoLoginException {
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

        if (StringUtils.isBlank(taskId)) throw new ServiceException(ErrorCodeEnum.E_PARAM_CHECK.getCode(), "매개변수 실패");
        TriggerTask triggerTask = baseMapper.getTaskById(userId, tenantId, taskId);
        if (triggerTask == null) throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "해당 예약 작업을 찾을 수 없어 삭제할 수 없습니다");

        Integer i = baseMapper.deleteTaskById(userId, tenantId, taskId);
        Integer j = scheduleTaskRobotDao.deleteByTaskIdLogically(taskId);

        if (i == 0 || j == 0) throw new ServiceException(ErrorCodeEnum.E_SQL_EXCEPTION.getCode());

        return AppResponse.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<Boolean> updateTriggerTask(UpdateTaskDto queryDto) throws NoLoginException {
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

        TriggerTask oldTriggerTask = baseMapper.getTaskById(userId, tenantId, queryDto.getTaskId());
        if (oldTriggerTask == null) throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "해당 예약 작업을 찾을 수 없습니다");

        // 재이름검증,  정렬제거
        if (checkNameCopy(queryDto.getName(), userId, tenantId, oldTriggerTask.getName()))
            throw new ServiceException(ErrorCodeEnum.E_SERVICE.getCode(), "명령이름재복사");

        List<RobotInfo> robotInfoList = queryDto.getRobotInfoList();
        if (CollectionUtils.isEmpty(robotInfoList)) throw new ServiceException(ErrorCodeEnum.E_PARAM_CHECK.getCode());

        TriggerTask newTriggerTask = new TriggerTask();
        BeanUtils.copyProperties(queryDto, newTriggerTask);
        newTriggerTask.setId(oldTriggerTask.getId());

        // 업데이트 triggerTask테이블
        int i = baseMapper.updateById(newTriggerTask);
        if (i == 0) throw new ServiceException(ErrorCodeEnum.E_SQL_EXCEPTION.getCode(), "예약 작업업데이트실패");
        // 업데이트scheduleTaskRobot테이블
        updateScheduleTaskRobot(queryDto, userId, tenantId);

        return AppResponse.success(true);
    }

    @Override
    public AppResponse<Boolean> enableTriggerTask(String taskId, Integer enable) throws NoLoginException {
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

        TriggerTask oldTriggerTask = baseMapper.getTaskById(userId, tenantId, taskId);
        if (oldTriggerTask == null) throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "해당 예약 작업을 찾을 수 없습니다");

        Boolean b = baseMapper.enableTask(userId, tenantId, taskId, enable);
        if (b) return AppResponse.success(true);
        else throw new ServiceException(ErrorCodeEnum.E_SQL_EXCEPTION.getCode());
    }

    @Override
    public AppResponse<IPage<TaskPageVo>> triggerTaskPage(TaskPageDto queryDto) throws NoLoginException {
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

        IPage<TaskPageVo> resPage = new Page<>();
        IPage<TaskPageVo> pageConfig = new Page<>(queryDto.getPageNo(), queryDto.getPageSize(), true);
        resPage = baseMapper.getExecuteDataList(pageConfig, queryDto, userId, tenantId);
        if (resPage.getTotal() == 0) return AppResponse.success(resPage); // 결과가비어 있습니다, 직선연결반환

        // 봇정보
        setRobotInfo(resPage);

        return AppResponse.success(resPage);
    }

    @Override
    public AppResponse<IPage<TaskPage4TriggerVo>> triggerTaskPage4Trigger(TaskPageDto queryDto)
            throws NoLoginException {
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

        IPage<TaskPage4TriggerVo> resPage = new Page<>();
        IPage<TaskPage4TriggerVo> pageConfig = new Page<>(queryDto.getPageNo(), queryDto.getPageSize(), true);
        resPage = baseMapper.getExecuteDataList4Trigger(pageConfig, queryDto, userId, tenantId);

        if (resPage.getTotal() == 0L) return AppResponse.success(resPage); // 결과가비어 있습니다, 직선연결반환결과

        // 봇데이터 robotInfoList
        setRobotInfoVoList(resPage);

        return AppResponse.success(resPage);
    }

    private void setRobotInfoVoList(IPage<TaskPage4TriggerVo> resPage) {
        List<TaskPage4TriggerVo> records = resPage.getRecords();
        List<String> taskIdList = records.stream().map(TaskPageVo::getTaskId).collect(Collectors.toList());

        List<ScheduleTaskRobot> scheduleTaskRobotList = scheduleTaskRobotDao.queryAll(taskIdList);
        for (TaskPage4TriggerVo record : records) {
            String taskId = record.getTaskId();

            // 현재taskId 의 scheduleTaskRobots
            List<ScheduleTaskRobot> scheduleTaskRobots = scheduleTaskRobotList.stream()
                    .filter(scheduleTaskRobot -> scheduleTaskRobot.getTaskId().equals(taskId))
                    .collect(Collectors.toList());

            List<RobotInfoVo> robotInfoVoList = getRobotInfoVoList(scheduleTaskRobots);

            record.setRobotInfoList(robotInfoVoList);
        }
    }

    private List<RobotInfoVo> getRobotInfoVoList(List<ScheduleTaskRobot> scheduleTaskRobots) {
        List<RobotInfoVo> ansVoList = new ArrayList<>();

        for (ScheduleTaskRobot scheduleTaskRobot : scheduleTaskRobots) {
            RobotInfoVo robotInfoVo = new RobotInfoVo();
            BeanUtils.copyProperties(scheduleTaskRobot, robotInfoVo);
            ansVoList.add(robotInfoVo);
        }

        return ansVoList;
    }

    private void setRobotInfo(IPage<TaskPageVo> resPage) {
        List<TaskPageVo> records = resPage.getRecords();

        List<String> taskIdList = records.stream().map(TaskPageVo::getTaskId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(taskIdList)) {
            return;
        }
        List<ScheduleTaskRobot> scheduleTaskRobotList = scheduleTaskRobotDao.queryAllByTaskId(taskIdList);

        for (TaskPageVo record : records) {
            String taskId = record.getTaskId();
            List<ScheduleTaskRobot> taskRobotList = scheduleTaskRobotList.stream()
                    .filter(scheduleTaskRobot -> scheduleTaskRobot.getTaskId().equals(taskId))
                    .collect(Collectors.toList());
            List<String> robotNameList =
                    taskRobotList.stream().map(ScheduleTaskRobot::getRobotName).collect(Collectors.toList());
            String robotNames = String.join(",", robotNameList);
            record.setRobotNames(robotNames);
        }

        resPage.setRecords(records);
    }

    public void updateScheduleTaskRobot(UpdateTaskDto queryDto, String userId, String tenantId) {
        InsertTaskDto insertTaskDto = new InsertTaskDto();
        BeanUtils.copyProperties(queryDto, insertTaskDto);
        String taskId = queryDto.getTaskId();
        Integer i = scheduleTaskRobotDao.deleteByTaskIdLogically(taskId);
        if (i == 0) throw new ServiceException(ErrorCodeEnum.E_SQL_EXCEPTION.getCode(), "예약 작업업데이트실패: 예약 작업봇사용업데이트실패");

        insertTaskRobot(insertTaskDto, userId, tenantId, taskId);
    }

    private void checkInsertParam(InsertTaskDto queryDto) {
        if (CollectionUtils.isEmpty(queryDto.getRobotInfoList()))
            throw new ServiceException(ErrorCodeEnum.E_PARAM_LOSE.getCode());

        // 작업유형 여부사
        boolean b = Arrays.stream(TaskTypeEnum.values())
                .anyMatch(taskType -> taskType.getCode().equals(queryDto.getTaskType()));
        if (!b) throw new ServiceException(ErrorCodeEnum.E_PARAM_CHECK.getCode(), "작업유형오류");

        // 예외유형여부유형
        boolean f = Arrays.stream(ExceptionalEnum.values())
                .anyMatch(exceptionalType -> exceptionalType.getCode().equals(queryDto.getExceptional()));
        if (!f) throw new ServiceException(ErrorCodeEnum.E_PARAM_CHECK.getCode(), "예외유형오류");
    }

    private TriggerTaskVo getTriggerTaskVo(TriggerTask triggerTask, String userId, String tenantId)
            throws NoLoginException, JsonProcessingException {
        TriggerTaskVo triggerTaskVo = new TriggerTaskVo();
        BeanUtils.copyProperties(triggerTask, triggerTaskVo);

        List<ScheduleTaskRobot> taskRobotList =
                scheduleTaskRobotDao.queryByTaskId(triggerTask.getTaskId(), userId, tenantId);
        // 봇있음데이터, 결과가있음, 설명데이터있음제목
        if (CollectionUtils.isEmpty(taskRobotList)) throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode());

        List<RobotInfoVo> robotInfoVoList = getRobotInfoVos(taskRobotList);
        triggerTaskVo.setRobotInfoVoList(robotInfoVoList);
        triggerTaskVo.setEnable(triggerTaskVo.getEnable());
        triggerTaskVo.setQueueEnable(triggerTaskVo.getQueueEnable());

        return triggerTaskVo;
    }

    private List<RobotInfoVo> getRobotInfoVos(List<ScheduleTaskRobot> taskRobotList)
            throws NoLoginException, JsonProcessingException {
        List<RobotInfoVo> robotInfoVoList = new ArrayList<>();
        for (ScheduleTaskRobot scheduleTaskRobot : taskRobotList) {
            RobotInfoVo robotInfoVo = new RobotInfoVo();
            robotInfoVo.setId(scheduleTaskRobot.getId());
            robotInfoVo.setRobotName(scheduleTaskRobot.getRobotName());
            robotInfoVo.setSort(scheduleTaskRobot.getSort());
            setParam(robotInfoVo, scheduleTaskRobot.getRobotId());
            robotInfoVo.setParamJson(scheduleTaskRobot.getParamJson());
            robotInfoVo.setRobotId(scheduleTaskRobot.getRobotId());
            packageVersion(robotInfoVo, scheduleTaskRobot.getRobotId());
            robotInfoVoList.add(robotInfoVo);
        }
        return robotInfoVoList;
    }

    private void packageVersion(RobotInfoVo robotInfoVo, String robotId) {
        Integer onlineVersionByRobotId = robotVersionDao.getOnlineVersionByRobotId(robotId);
        if (onlineVersionByRobotId != null) {
            robotInfoVo.setRobotVersion(onlineVersionByRobotId);
        } else {
            robotInfoVo.setRobotVersion(0);
        }
    }

    private void setParam(RobotInfoVo robotInfoVo, String robotId) throws JsonProcessingException, NoLoginException {
        QueryParamDto queryParamDto = new QueryParamDto();
        queryParamDto.setRobotId(robotId);
        AppResponse<List<ParamDto>> paramListResponse = paramService.getAllParams(queryParamDto);
        List<ParamDto> paramList = paramListResponse.getData();
        if (CollectionUtils.isEmpty(paramList)) {
            robotInfoVo.setHaveParam(false);
            robotInfoVo.setParamJson(null);
            return;
        }
        robotInfoVo.setHaveParam(true);
        ObjectMapper mapper = new ObjectMapper();
        String paramJson = mapper.writeValueAsString(paramList);
        robotInfoVo.setParamJson(paramJson);
    }

    public void insertTaskRobot(InsertTaskDto queryDto, String userId, String tenantId, String triggerTaskId) {
        List<RobotInfo> robotInfoList = queryDto.getRobotInfoList();
        List<ScheduleTaskRobot> scheduleTaskRobotList = new ArrayList<>();

        for (int i = 0; i < robotInfoList.size(); i++) {
            RobotInfo robotInfo = robotInfoList.get(i);

            ScheduleTaskRobot scheduleTaskRobot = new ScheduleTaskRobot();

            scheduleTaskRobot.setRobotId(robotInfo.getRobotId());
            scheduleTaskRobot.setSort(i + 1);
            scheduleTaskRobot.setTenantId(tenantId);
            scheduleTaskRobot.setCreatorId(userId);
            scheduleTaskRobot.setUpdaterId(userId);
            scheduleTaskRobot.setParamJson(robotInfo.getParamJson());

            scheduleTaskRobotList.add(scheduleTaskRobot);
        }

        scheduleTaskRobotDao.insertRobotBatch(triggerTaskId, scheduleTaskRobotList);
    }

    public String insertTask(InsertTaskDto queryDto, String userId, String tenantId) {
        TriggerTask triggerTask = new TriggerTask();

        String taskId = String.valueOf(idWorker.nextId());

        triggerTask.setTaskId(taskId);
        triggerTask.setName(queryDto.getName());
        triggerTask.setTaskJson(queryDto.getTaskJson());
        triggerTask.setTaskType(queryDto.getTaskType());
        triggerTask.setEnable(queryDto.getEnable());
        triggerTask.setExceptional(queryDto.getExceptional());
        triggerTask.setQueueEnable(queryDto.getQueueEnable());
        triggerTask.setTimeout(queryDto.getTimeout());
        triggerTask.setCreatorId(userId);
        triggerTask.setUpdaterId(userId);
        triggerTask.setTenantId(tenantId);

        baseMapper.insert(triggerTask);

        return taskId;
    }

    private List<Executor> getExecutorList(List<RobotExecute> robotExecuteList)
            throws NoLoginException, JsonProcessingException {
        List<Executor> result = new ArrayList<>();
        for (RobotExecute robotExecute : robotExecuteList) {
            Executor executor = new Executor();
            String robotId = robotExecute.getRobotId();
            executor.setRobotName(robotExecute.getName());
            executor.setRobotId(robotId);

            executor.setRobotVersion(robotExecute.getAppVersion());

            QueryParamDto queryParamDto = new QueryParamDto();
            // Mode 로 EXECUTOR 방식
            queryParamDto.setMode(RobotConstant.EXECUTOR);
            queryParamDto.setRobotId(robotId);
            AppResponse<List<ParamDto>> paramListResponse = paramService.getAllParams(queryParamDto);
            List<ParamDto> paramList = paramListResponse.getData();
            if (CollectionUtils.isEmpty(paramList)) {
                executor.setHaveParam(false);
                executor.setParamJson(null);
                result.add(executor);
                continue;
            }
            executor.setHaveParam(true);
            ObjectMapper mapper = new ObjectMapper();
            String paramJson = mapper.writeValueAsString(paramList);
            executor.setParamJson(paramJson);
            result.add(executor);
        }

        return result;
    }

    // 삽입의시, 아니요사용정렬제거
    private boolean checkNameCopy(String name, String userId, String tenantId) {
        List<String> allTaskName = baseMapper.getAllTaskName(userId, tenantId);
        if (allTaskName.contains(name)) return true;
        return false;
    }

    // 업데이트의시, 필요정렬제거
    private boolean checkNameCopy(String name, String userId, String tenantId, String oldName) {
        if (name.equals(oldName)) return false; // 이름문자및

        // 에서의
        List<String> allTaskName = baseMapper.getAllTaskName(userId, tenantId);
        if (allTaskName.contains(name)) return true;
        return false;
    }
}