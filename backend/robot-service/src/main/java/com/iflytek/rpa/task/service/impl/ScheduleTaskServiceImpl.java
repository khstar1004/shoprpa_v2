package com.iflytek.rpa.task.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.task.dao.ScheduleTaskDao;
import com.iflytek.rpa.task.dao.ScheduleTaskPullLogDao;
import com.iflytek.rpa.task.dao.ScheduleTaskRobotDao;
import com.iflytek.rpa.task.entity.ScheduleTask;
import com.iflytek.rpa.task.entity.ScheduleTaskRobot;
import com.iflytek.rpa.task.entity.bo.ScheduleRule;
import com.iflytek.rpa.task.entity.bo.TimeTask;
import com.iflytek.rpa.task.entity.dto.NextTaskDto;
import com.iflytek.rpa.task.entity.dto.ScheduleTaskDto;
import com.iflytek.rpa.task.entity.dto.TaskDto;
import com.iflytek.rpa.task.entity.dto.TaskInfoDto;
import com.iflytek.rpa.task.entity.enums.CycleWeekEnum;
import com.iflytek.rpa.task.service.CronExpression;
import com.iflytek.rpa.task.service.ScheduleTaskService;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * <p>
 * 스케줄링작업 서비스유형
 * </p>
 *
 * @author keler
 * @since 2021-10-08
 */
@Slf4j
@Service("scheduleTaskService")
public class ScheduleTaskServiceImpl extends ServiceImpl<ScheduleTaskDao, ScheduleTask> implements ScheduleTaskService {

    @Autowired
    private ScheduleTaskDao scheduleTaskDao;

    @Autowired
    private ScheduleTaskRobotDao scheduleTaskRobotDao;

    @Autowired
    private ScheduleTaskPullLogDao scheduleTaskPullLogDao;

    @Autowired
    private IdWorker idWorker;

    private int taskMaxSize = 100;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    public Date generateNextValidTimeNew(ScheduleTask task, Date fromTime) throws Exception {
        if ("fixed".equals(task.getRunMode()) || "custom".equals(task.getRunMode())) {
            return new CronExpression(task.getScheduleConf()).getNextValidTimeAfter(fromTime);
        } else if ("cycle".equals(task.getRunMode())) {
            return generateValidTimeByStartTime(Integer.parseInt(task.getScheduleConf()), task.getStartAt());
        }
        return null;
    }

    public static Date generateValidTimeByStartTime(Integer time, Date fromTime) {
        Date date = new Date();
        if (date.before(fromTime)) {
            // 결과가현재시간까지일실행시간, 직선연결반환일실행시간
            return new Date(fromTime.getTime());
        }
        // 계획현재시간및시작 시간의초데이터:
        int seconds = (int) Math.ceil(Double.valueOf((date.getTime() - fromTime.getTime()) / (1000)));
        // 계획현재시간전후일기호합치기시간의실행시간
        long newTime = (long) (Math.ceil((double) seconds / time) * time * 1000) + fromTime.getTime();
        return new Date(newTime);
    }

    public Date getCalSecond(Date date, int calSeconds) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.SECOND, calSeconds);
        return c.getTime();
    }

    private void timeTransToCron(ScheduleTask task) {
        // 유형, 변환로cron테이블방식
        // 시간트리거
        if ("cycle".equals(task.getRunMode())) {
            // 
            long seconds = 0L;
            // 
            if ("custom".equals(task.getCycleFrequency())) {
                // 지정시길이
                if ("minute".equals(task.getCycleUnit())) {
                    // 분
                    seconds = 60 * Long.parseLong(task.getCycleNum());
                } else if ("hour".equals(task.getCycleUnit())) {
                    // 시간
                    seconds = 3600 * Long.parseLong(task.getCycleNum());
                }
            } else {
                seconds = Long.parseLong(task.getCycleFrequency());
            }
            // todo 실행시간
            task.setScheduleConf(Long.toString(seconds));
        } else if ("fixed".equals(task.getRunMode())) {
            // 예약

            String cron = "";
            // 예약
            ScheduleRule rule = JSONObject.parseObject(task.getScheduleRule(), ScheduleRule.class);
            if ("month".equals(task.getScheduleType())) {
                // 매월예약
                cron = rule.getSecond() + " " + rule.getMinute() + " " + rule.getHour() + " " + rule.getDate() + " * ?";
            } else if ("week".equals(task.getScheduleType())) {
                // 매주예약
                cron = rule.getSecond() + " " + rule.getMinute() + " " + rule.getHour() + " ? * "
                        + CycleWeekEnum.getCodeByNum(rule.getDayOfWeek());
            } else if ("day".equals(task.getScheduleType())) {
                // 매일예약
                cron = rule.getSecond() + " " + rule.getMinute() + " " + rule.getHour() + " * * ?";
            }
            task.setScheduleConf(cron);
        } else if ("custom".equals(task.getRunMode())) {
            // 지정
            String cron = task.getCronExpression();
            task.setScheduleConf(cron);
        }
    }

    @Override
    public AppResponse<?> getTaskList(TaskDto taskDto) throws NoLoginException {
        IPage<ScheduleTask> pages = new Page<>();
        if (null == taskDto.getPageNo() || null == taskDto.getPageSize()) {
            return AppResponse.success(pages);
        }
        AppResponse<User> resp = rpaAuthFeign.getLoginUser();
        if (resp == null || !resp.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = resp.getData();
        String userId = loginUser.getId();

        taskDto.setUserId(userId);
        IPage<ScheduleTask> pageConfig = new Page<>(taskDto.getPageNo(), taskDto.getPageSize(), true);
        pages = scheduleTaskDao.getTaskList(pageConfig, taskDto);
        List<ScheduleTask> taskList = pages.getRecords();
        if (CollectionUtils.isEmpty(taskList)) {
            return AppResponse.success(pages);
        }
        // 가져오기봇이름
        // 가져오기봇id및버전
        //        Map<String, List<TaskRobotBo>> taskRobotMap = new HashMap<>();
        //        List<TaskRobotBo> allRobotList = new ArrayList<>();
        // 가져오기taskId목록
        List<String> taskIdList = taskList.stream().map(ScheduleTask::getTaskId).collect(Collectors.toList());
        // 조회모든예약 작업봇
        List<ScheduleTaskRobot> scheduleTaskRobotList = scheduleTaskRobotDao.queryAllByTaskId(taskIdList);
        // 봇근거taskId분그룹
        Map<String, List<ScheduleTaskRobot>> scheduleTaskRobotMap =
                scheduleTaskRobotList.stream().collect(Collectors.groupingBy(ScheduleTaskRobot::getTaskId));
        if (CollectionUtils.isEmpty(scheduleTaskRobotMap)) {
            return AppResponse.success(pages);
        }

        //        for (ScheduleTask task: taskList) {
        //            if(null == task || null == task.getExecuteSequence()){
        //                continue;
        //            }
        //            List<TaskRobotBo> taskRobotList = JSONObject.parseArray(task.getExecuteSequence(),
        // TaskRobotBo.class);
        //            taskRobotMap.put(task.getTaskId(), taskRobotList);
        //            allRobotList.addAll(taskRobotList);
        //        }
        //        if(CollectionUtils.isEmpty(allRobotList)){
        //            return AppResponse.error(ErrorCodeEnum.E_SQL,"데이터예외, 예약 작업없음봇정보");
        //        }
        // 근거id및버전조회이름문자
        //        List<RobotVersion> robotVersionList = robotVersionDao.getRobotNameList(allRobotList);
        //        if(CollectionUtils.isEmpty(robotVersionList)){
        //            return AppResponse.success(pages);
        //        }
        //        //근거robotId분그룹
        //        Map<String, String> robotVersionMap =
        // robotVersionList.stream().collect(Collectors.toMap(RobotVersion::getRobotId,RobotVersion::getName));
        // 이름문자
        for (ScheduleTask task : taskList) {
            String taskId = task.getTaskId();
            List<ScheduleTaskRobot> taskRobotList = scheduleTaskRobotMap.get(taskId);
            StringBuilder robotNameStr = new StringBuilder();
            if (CollectionUtils.isEmpty(taskRobotList)) {
                continue;
            }
            for (ScheduleTaskRobot taskRobot : taskRobotList) {
                if (null == taskRobot) {
                    continue;
                }
                robotNameStr.append(taskRobot.getRobotName()).append(",");
            }
            robotNameStr.deleteCharAt(robotNameStr.length() - 1);
            task.setAllRobotName(robotNameStr.toString());
            // 아래실행시간
            // 결과가예사용 안 함, 또는중지시간소현재, 이면아니요가져오기시간, 
            if (!needNextTime(task)) {
                task.setNextTime(null);
            } else {
                try {
                    setNextTime(task);
                } catch (Exception e) {
                    task.setNextTime(null);
                    log.error("getTaskList가져오기아래실행시간오류: {}", e.getMessage());
                }
            }
        }

        return AppResponse.success(pages);
    }

    private void setNextTime(ScheduleTask task) throws Exception {
        Date date = new Date();
        //        task.setLastTime(task.getNextTime());
        //        task.setPullTime(new Date());
        timeTransToCron(task);
        //        try {
        task.setNextTime(generateNextValidTimeNew(task, getCalSecond(date, 5)));
        //        }catch (Exception e){
        //            log.error(e.getMessage());
        //        task.setNextTime(null);
        //        }
    }

    private boolean needNextTime(ScheduleTask task) {
        if (null == task || null == task.getEnable()) {
            return false;
        }
        if (0 == task.getEnable()) {
            return false;
        }
        if (null == task.getStartAt() || null == task.getEndAt()) {
            return false;
        }
        if (task.getEndAt().before(new Date())) {
            return false;
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> saveTask(ScheduleTaskDto task) throws NoLoginException {
        ScheduleTask scheduleTask = new ScheduleTask();
        BeanUtils.copyProperties(task, scheduleTask);
        // 에서timeTask가져오기정보
        TimeTask timeTask = task.getTimeTask();
        if (null != timeTask) {
            BeanUtils.copyProperties(timeTask, scheduleTask);
            if (null != timeTask.getScheduleRule()) {
                scheduleTask.setScheduleRule(JSONObject.toJSONString(timeTask.getScheduleRule()));
            }
        }
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        scheduleTask.setTenantId(tenantId);
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        scheduleTask.setCreatorId(userId);
        scheduleTask.setUpdaterId(userId);
        // 아래실행시간
        try {
            setNextTime(scheduleTask);
        } catch (Exception e) {
            log.error(e.getMessage());
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "시간 형식이 올바르지 않습니다");
        }
        List<String> taskRobotIdList = task.getExecuteSequence();
        if (CollectionUtils.isEmpty(taskRobotIdList)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "선택하세요봇");
        }
        AppResponse<String> res = rpaAuthFeign.getTenantId();
        if (res == null || res.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String nowTenantId = res.getData();
        // 실행순서
        List<ScheduleTaskRobot> taskRobotList = new ArrayList<>();
        for (int i = 0; i < taskRobotIdList.size(); i++) {
            if (null == taskRobotIdList.get(i)) {
                continue;
            }
            ScheduleTaskRobot scheduleTaskRobot = new ScheduleTaskRobot();
            scheduleTaskRobot.setRobotId(taskRobotIdList.get(i));
            scheduleTaskRobot.setSort(i + 1);

            scheduleTaskRobot.setTenantId(nowTenantId);
            scheduleTaskRobot.setCreatorId(userId);
            scheduleTaskRobot.setUpdaterId(userId);
            taskRobotList.add(scheduleTaskRobot);
        }
        if (StringUtils.isNotBlank(task.getTaskId())) {
            Integer count = scheduleTaskDao.queryCountByTaskId(task.getTaskId());
            if (count > 0) {
                scheduleTask.setTaskId(task.getTaskId());
                scheduleTaskDao.updateScheduleTask(scheduleTask);
                // 조회봇
                List<String> hisTaskRobotIdList =
                        scheduleTaskRobotDao.queryHisRobotIdListByTaskId(scheduleTask.getTaskId());
                // 예업데이트, 데이터수정완료, 이면삭제후증가
                if (!hisTaskRobotIdList.equals(taskRobotIdList)) {
                    // 삽입또는업데이트예약 작업패키지의봇및실행순서
                    scheduleTaskRobotDao.deleteByTaskId(scheduleTask.getTaskId());
                    scheduleTaskRobotDao.insertRobotBatch(scheduleTask.getTaskId(), taskRobotList);
                }
            } else {
                return AppResponse.error(ErrorCodeEnum.E_SQL, "데이터예외, 작업찾을 수 없습니다");
            }
        } else {
            scheduleTask.setTaskId(idWorker.nextId() + "");
            scheduleTask.setEnable(1);
            scheduleTaskDao.createScheduleTask(scheduleTask);
            // 생성예약 작업, 직선연결삽입봇목록
            scheduleTaskRobotDao.insertRobotBatch(scheduleTask.getTaskId(), taskRobotList);
        }
        return AppResponse.success(true);
    }

    @Override
    public AppResponse<?> getTaskInfoByTaskId(String taskId) throws NoLoginException {
        if (StringUtils.isBlank(taskId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "적음예약 작업id");
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
        TaskInfoDto taskInfoDto = new TaskInfoDto();
        ScheduleTask task = scheduleTaskDao.getTaskInfoByTaskId(taskId, userId, tenantId);
        if (null == task) {
            return AppResponse.error(ErrorCodeEnum.E_SQL, "데이터예외, 작업찾을 수 없습니다");
        }
        // 조회패키지봇
        List<String> robotIdList = scheduleTaskRobotDao.queryRobotIdListByTaskId(taskId);
        task.setExecuteSequence(robotIdList);
        TimeTask timeTask = new TimeTask();
        BeanUtils.copyProperties(task, timeTask, "scheduleRule");
        if (null != task.getScheduleRule()) {
            timeTask.setScheduleRule(JSONObject.parseObject(task.getScheduleRule(), ScheduleRule.class));
        }
        BeanUtils.copyProperties(task, taskInfoDto);
        taskInfoDto.setTimeTask(timeTask);
        return AppResponse.success(taskInfoDto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> getNextTimeInfoAndUpdate() throws NoLoginException {
        // todo 봇, 예약 작업및본예약 작업
        /**
         * 본예약 작업실행, 아래의실행시간까지완료, 아래의직선연결로실패
         */
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
        // 조회사용상태의예약 작업
        ScheduleTask task = getRecentlyTask(userId, tenantId);
        //        ScheduleTask task = scheduleTaskDao.getTaskListOrderByNextTime(userId, tenantId);
        if (null == task || null == task.getNextTime()) {
            return AppResponse.success(null);
        }
        task.setPullTime(new Date());
        task.setLastTime(task.getNextTime());
        NextTaskDto nextTaskDto = new NextTaskDto();
        // 본예약 작업아래실행시간
        nextTaskDto.setNextTime(task.getNextTime());
        //        return AppResponse.error(ErrorCodeEnum.E_SQL,"데이터예외");
        nextTaskDto.setTaskId(task.getTaskId());
        nextTaskDto.setTaskName(task.getName());
        nextTaskDto.setExceptionHandleWay(task.getExceptionHandleWay());
        // 조회봇목록
        List<String> robotIdList = scheduleTaskRobotDao.queryRobotIdListByTaskId(task.getTaskId());
        if (CollectionUtils.isEmpty(robotIdList)) {
            return AppResponse.error(ErrorCodeEnum.E_SQL, "데이터예외, 예약 작업없음있음봇");
        }
        nextTaskDto.setRobotIdList(robotIdList);
        // 업데이트아래실행시간
        scheduleTaskDao.updateScheduleTask(task);
        if (null != task.getLogEnable() && "T".equals(task.getLogEnable())) {
            scheduleTaskPullLogDao.insetLog(task);
        }

        return AppResponse.success(nextTaskDto);
    }

    public ScheduleTask getRecentlyTask(String userId, String tenantId) {
        int total = scheduleTaskDao.countTaskTotal(userId, tenantId);
        if (0 == total) {
            return null;
        }
        if (total > taskMaxSize) {
            throw new IllegalStateException("사용중예약 작업수초과경과" + taskMaxSize + "개" + ", 요청를모듈분작업사용 안 함");
        }
        //        PageBatch pageBatch = new PageBatch();
        // 량, 중지수경과대, 메모리출력또는mybatis오류
        ScheduleTask recentlyTask = new ScheduleTask();
        //        pageBatch.process(total, batchSize, (start, end) -> {
        List<ScheduleTask> taskList = scheduleTaskDao.getTaskListByPage(userId, tenantId);
        for (ScheduleTask scheduleTask : taskList) {
            if (null == scheduleTask) {
                continue;
            }
            try {
                setNextTime(scheduleTask);
            } catch (Exception e) {
                throw new IllegalArgumentException("데이터예외");
            }

            if (null == recentlyTask.getNextTime()) {
                BeanUtils.copyProperties(scheduleTask, recentlyTask);
            }
            if (recentlyTask.getNextTime().after(scheduleTask.getNextTime())) {
                BeanUtils.copyProperties(scheduleTask, recentlyTask);
            }
        }
        //            return new ArrayList<>();
        //        });

        return recentlyTask;
    }

    @Override
    public AppResponse<?> enableTask(ScheduleTask task) {
        if (null == task.getTaskId() || null == task.getEnable()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM);
        }
        if (1 == task.getEnable()) {
            try {
                // 매사용업데이트일아래아래실행시간
                setNextTime(task);
            } catch (Exception e) {
                return AppResponse.error(ErrorCodeEnum.E_SQL, "데이터예외");
            }
        }
        scheduleTaskDao.updateTask(task);
        return AppResponse.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> deleteTask(ScheduleTask task) {
        if (null == task.getTaskId()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM);
        }
        task.setDeleted(1);
        scheduleTaskDao.updateTask(task);
        // 삭제schedule——robot
        scheduleTaskRobotDao.deleteByTaskId(task.getTaskId());
        return AppResponse.success(true);
    }

    @Override
    public AppResponse<?> checkSameName(ScheduleTask task) {
        String taskName = task.getName();
        if (StringUtils.isBlank(taskName)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "작업이름비워 둘 수 없습니다");
        }
        Integer count = scheduleTaskDao.countByTaskName(task);
        return AppResponse.success(count > 0);
    }

    @Override
    public AppResponse<?> checkCorn(ScheduleTask task) {
        if (StringUtils.isBlank(task.getCronExpression())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "corn테이블방식비워 둘 수 없습니다");
        }
        try {
            new CronExpression(task.getCronExpression());
        } catch (Exception e) {
            return AppResponse.success(false);
        }
        return AppResponse.success(true);
    }
}