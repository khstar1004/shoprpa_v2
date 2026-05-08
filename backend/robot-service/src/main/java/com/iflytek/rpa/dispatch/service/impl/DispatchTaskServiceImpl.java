package com.iflytek.rpa.dispatch.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.dispatch.dao.DispatchTaskDao;
import com.iflytek.rpa.dispatch.entity.CronJson;
import com.iflytek.rpa.dispatch.entity.DispatchTask;
import com.iflytek.rpa.dispatch.entity.RedisListBo;
import com.iflytek.rpa.dispatch.entity.enums.DispatchTaskFromType;
import com.iflytek.rpa.dispatch.entity.enums.DispatchTaskStatus;
import com.iflytek.rpa.dispatch.entity.vo.TerminalTaskDetailVo;
import com.iflytek.rpa.dispatch.service.DispatchTaskService;
import com.iflytek.rpa.utils.DateUtils;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.RedisKeyUtils;
import com.iflytek.rpa.utils.RedisUtils;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service("dispatchTaskService")
@Slf4j
public class DispatchTaskServiceImpl extends ServiceImpl<DispatchTaskDao, DispatchTask> implements DispatchTaskService {
    @Autowired
    private DispatchTaskDao dispatchTaskDao;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private DispatchTaskService self; // 비고입력

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    /**
     * 조회업데이트작업경과상태
     * 관리active및expired상태의작업, 근거time_expression행상태변환
     *
     * @param tasks 작업목록
     */
    private void checkAndUpdateTaskExpiredStatus(List<DispatchTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        Date currentTime = new Date();
        List<DispatchTask> tasksToUpdate = new ArrayList<>();

        for (DispatchTask task : tasks) {
            // 관리active및expired상태의작업
            if (!DispatchTaskStatus.ACTIVE.getValue().equals(task.getStatus())
                    && !DispatchTaskStatus.EXPIRED.getValue().equals(task.getStatus())) {
                continue;
            }

            // 조회cronJson중여부있음time_expression
            if (task.getCronJson() != null && !task.getCronJson().trim().isEmpty()) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    CronJson cronJson = objectMapper.readValue(task.getCronJson(), CronJson.class);

                    // 결과가있음end_time아니요비어 있습니다
                    if (cronJson.getEndTime() != null
                            && !cronJson.getEndTime().trim().isEmpty()) {
                        // 파싱시간테이블방식
                        Date taskEndTime = DateUtils.sdfdaytime.parse(cronJson.getEndTime());

                        String currentStatus = task.getStatus();
                        String newStatus = null;

                        // 현재상태및경과상태, 지정여부필요업데이트
                        if (DispatchTaskStatus.ACTIVE.getValue().equals(currentStatus)) {
                            // 결과가현재예active상태, 시간완료경과, 이면업데이트로expired
                            if (taskEndTime.before(currentTime)) {
                                newStatus = DispatchTaskStatus.EXPIRED.getValue();
                                log.info(
                                        "작업{}에서active업데이트로expired, 종료 시간: {}, 현재시간: {}",
                                        task.getDispatchTaskId(),
                                        cronJson.getTimeExpression(),
                                        DateUtils.getDayTimeFormat(currentTime));
                            }
                        } else if (DispatchTaskStatus.EXPIRED.getValue().equals(currentStatus)) {
                            // 결과가현재예expired상태, 시간미완료경과, 이면업데이트로active
                            if (taskEndTime.after(currentTime) || taskEndTime.equals(currentTime)) {
                                newStatus = DispatchTaskStatus.ACTIVE.getValue();
                                log.info(
                                        "작업{}에서expired업데이트로active, 종료 시간: {}, 현재시간: {}",
                                        task.getDispatchTaskId(),
                                        cronJson.getTimeExpression(),
                                        DateUtils.getDayTimeFormat(currentTime));
                            }
                        }

                        // 결과가필요업데이트상태
                        if (newStatus != null) {
                            task.setStatus(newStatus);
                            task.setUpdateTime(currentTime);
                            tasksToUpdate.add(task);
                        }
                    } else if (DispatchTaskStatus.EXPIRED.getValue().equals(task.getStatus())) {
                        // 결과가현재예expired상태, 예있음매칭cron_json,이면보관ac상태
                        task.setStatus(DispatchTaskStatus.ACTIVE.getValue());
                        task.setUpdateTime(currentTime);
                        tasksToUpdate.add(task);
                    }
                } catch (Exception e) {
                    log.error("파싱작업{}의cronJson실패: {}", task.getDispatchTaskId(), task.getCronJson(), e);
                }
            } else if (DispatchTaskStatus.EXPIRED.getValue().equals(task.getStatus())) {
                // 결과가현재예expired상태, 예있음매칭cron_json,이면보관ac상태
                task.setStatus(DispatchTaskStatus.ACTIVE.getValue());
                task.setUpdateTime(currentTime);
                tasksToUpdate.add(task);
            }
        }

        // 량업데이트필요수정상태의작업
        if (!tasksToUpdate.isEmpty()) {
            try {
                for (DispatchTask taskToUpdate : tasksToUpdate) {
                    this.updateById(taskToUpdate);
                }
                log.info("성공업데이트{}개작업의상태", tasksToUpdate.size());
            } catch (Exception e) {
                log.error("업데이트작업상태실패", e);
            }
        }
    }

    @Override
    public AppResponse<TerminalTaskDetailVo> getTerminalTaskDetail(String terminalId) {
        try {
            // 생성단말작업
            TerminalTaskDetailVo terminalTaskDetail = buildTerminalTaskDetail(terminalId);
            return AppResponse.success(terminalTaskDetail);
        } catch (Exception e) {
            log.error("가져오기단말작업실패, terminalId: {}", terminalId, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "단말 작업 정보를 가져오지 못했습니다");
        }
    }

    /**
     * 생성단말작업
     *
     * @param terminalId 단말ID
     * @return 단말작업
     */
    private TerminalTaskDetailVo buildTerminalTaskDetail(String terminalId) {
        TerminalTaskDetailVo terminalTaskDetail =
                TerminalTaskDetailVo.builder().terminalId(terminalId).build();

        // 1. 조회데이터베이스중의정상일반작업
        terminalTaskDetail.getDispatchTaskInfos().addAll(dispatchTaskDao.selectTaskInfoByTerminalId(terminalId));

        // 2. 재Redis상태
        RedisUtils.set(RedisKeyUtils.getDispatchTaskStatusKey(terminalId), "0");

        // 3. 에서Redis가져오기 관리작업(, 재시도, 중지)
        processRedisTasks(terminalId, terminalTaskDetail);

        // 4. 로모든작업량봇정보
        populateRobotInfoForAllTasks(terminalTaskDetail);

        return terminalTaskDetail;
    }

    /**
     * 로모든작업봇정보
     */
    private void populateRobotInfoForAllTasks(TerminalTaskDetailVo terminalTaskDetail) {
        // 모든작업ID
        List<String> allTaskIds = getAllTaskIds(terminalTaskDetail);

        if (allTaskIds.isEmpty()) {
            return;
        }

        // 량조회봇정보 생성
        Map<String, List<TerminalTaskDetailVo.DispatchRobotInfo>> taskRobotInfoMap = buildTaskRobotInfoMap(allTaskIds);

        // 로모든작업목록 봇정보
        setRobotInfoForAllTaskLists(terminalTaskDetail, taskRobotInfoMap);
    }

    /**
     * 모든작업ID
     */
    private List<String> getAllTaskIds(TerminalTaskDetailVo terminalTaskDetail) {
        List<String> allTaskIds = new ArrayList<>();

        // 사용Stream API, 추가null설치전체조회
        Stream.of(
                        terminalTaskDetail.getDispatchTaskInfos(),
                        terminalTaskDetail.getRetryTaskInfos(),
                        terminalTaskDetail.getStopTaskInfos())
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull) // 금액외부보관, 중지taskInfo로null
                .map(TerminalTaskDetailVo.DispatchTaskInfo::getTaskId)
                .filter(Objects::nonNull) // 금액외부보관, 중지taskId로null
                .forEach(allTaskIds::add);

        return allTaskIds;
    }

    /**
     * 생성작업ID까지봇정보의
     */
    private Map<String, List<TerminalTaskDetailVo.DispatchRobotInfo>> buildTaskRobotInfoMap(List<String> taskIds) {
        List<TerminalTaskDetailVo.DispatchRobotInfo> allRobotInfos = dispatchTaskDao.selectRobotInfoByTaskIds(taskIds);

        return allRobotInfos.stream()
                .filter(robotInfo -> robotInfo.getTaskId() != null)
                .collect(Collectors.groupingBy(TerminalTaskDetailVo.DispatchRobotInfo::getTaskId, Collectors.toList()));
    }

    /**
     * 로모든작업목록 봇정보
     */
    private void setRobotInfoForAllTaskLists(
            TerminalTaskDetailVo terminalTaskDetail,
            Map<String, List<TerminalTaskDetailVo.DispatchRobotInfo>> taskRobotInfoMap) {
        Stream.of(
                        terminalTaskDetail.getDispatchTaskInfos(),
                        terminalTaskDetail.getRetryTaskInfos(),
                        terminalTaskDetail.getStopTaskInfos())
                .filter(Objects::nonNull) // 확인List아니요로null
                .forEach(taskList -> setRobotInfoForTaskList(taskList, taskRobotInfoMap));
    }

    /**
     * 로작업목록 봇정보
     */
    private void setRobotInfoForTaskList(
            List<TerminalTaskDetailVo.DispatchTaskInfo> taskList,
            Map<String, List<TerminalTaskDetailVo.DispatchRobotInfo>> taskRobotInfoMap) {
        if (taskList == null || taskList.isEmpty()) {
            return;
        }

        taskList.forEach(taskInfo -> {
            // 설치전체봇정보목록
            if (taskInfo.getDispatchRobotInfos() == null) {
                taskInfo.setDispatchRobotInfos(new ArrayList<>());
            }

            List<TerminalTaskDetailVo.DispatchRobotInfo> robotInfos =
                    taskRobotInfoMap.getOrDefault(taskInfo.getTaskId(), new ArrayList<>());
            taskInfo.setDispatchRobotInfos(robotInfos);
        });
    }

    /**
     * 관리Redis중의작업
     */
    private void processRedisTasks(String terminalId, TerminalTaskDetailVo terminalTaskDetail) {
        try {
            String redisListKey = RedisKeyUtils.getDispatchTaskListKey(terminalId);
            long listSize = RedisUtils.lGetListSize(redisListKey);

            if (listSize == 0) {
                log.debug("단말{}의Redis작업큐비어 있습니다", terminalId);
                return;
            }

            // 량가져오기Redis작업
            List<Object> redisTasks = getRedisTasks(redisListKey, listSize);
            if (redisTasks.isEmpty()) {
                log.warn("단말{}의Redis작업큐출력실패", terminalId);
                return;
            }

            log.info("에서Redis출력단말{}의{}개작업", terminalId, redisTasks.size());

            // 관리작업까지의목록중
            processAndSetRedisTasks(redisTasks, terminalTaskDetail);

        } catch (Exception e) {
            log.error("에서Redis가져오기 작업실패: terminalId={}", terminalId, e);
        }
    }

    /**
     * 에서Redis가져오기작업목록
     */
    private List<Object> getRedisTasks(String redisListKey, long listSize) {
        List<Object> redisTasks = new ArrayList<>();

        for (int i = 0; i < listSize; i++) {
            Object task = RedisUtils.redisTemplate.opsForList().leftPop(redisListKey);
            if (task != null) {
                redisTasks.add(task);
            }
        }

        return redisTasks;
    }

    /**
     * 관리Redis작업까지의작업목록중
     */
    private void processAndSetRedisTasks(List<Object> redisTasks, TerminalTaskDetailVo terminalTaskDetail) {
        // 작업유형분그룹
        Map<DispatchTaskFromType, List<String>> taskTypeMap = redisTasks.stream()
                .filter(Objects::nonNull)
                .map(this::extractTaskInfo)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        taskInfo -> taskInfo.getDispatchTaskFromType(),
                        Collectors.mapping(taskInfo -> taskInfo.getDispatchTaskId(), Collectors.toList())));

        // 량조회작업정보
        taskTypeMap.forEach((taskType, taskIds) -> {
            if (!taskIds.isEmpty()) {
                List<TerminalTaskDetailVo.DispatchTaskInfo> taskInfos = new ArrayList<>();
                taskIds.forEach(taskId -> {
                    TerminalTaskDetailVo.DispatchTaskInfo taskInfo = dispatchTaskDao.selectTaskInfoByTaskId(taskId);
                    if (taskInfo != null) {
                        taskInfos.add(taskInfo);
                    }
                });

                switch (taskType) {
                    case NORMAL:
                        terminalTaskDetail.getDispatchTaskInfos().addAll(taskInfos);
                        break;
                    case RETRY:
                        terminalTaskDetail.getRetryTaskInfos().addAll(taskInfos);
                        break;
                    case STOP:
                        terminalTaskDetail.getStopTaskInfos().addAll(taskInfos);
                        break;
                    default:
                        log.warn("지원하지 않는의작업유형: {}", taskType);
                        break;
                }
                log.debug("관리완료{}개{}작업", taskInfos.size(), taskType.name().toLowerCase());
            }
        });
    }

    /**
     * 에서Redis작업객체중가져오기작업정보
     */
    private RedisListBo extractTaskInfo(Object redisTaskObj) {
        try {
            return (RedisListBo) redisTaskObj;
        } catch (Exception e) {
            log.error("관리Redis중의작업정보실패: redisTaskObj={}", redisTaskObj, e);
            return null;
        }
    }

    /**
     * 문의조회지정단말여부있음작업업데이트
     *
     * @param terminalId 단말ID
     * @return true테이블있음데이터업데이트, false테이블데이터 없음업데이트
     */
    public boolean checkTaskUpdate(String terminalId) {
        if (terminalId == null || terminalId.trim().isEmpty()) {
            log.warn("단말ID비어 있습니다, 불가조회작업업데이트");
            return false;
        }

        try {
            // 조회작업큐여부있음데이터
            String manualTaskKey = RedisKeyUtils.getDispatchTaskListKey(terminalId);
            long manualTaskSize = RedisUtils.lGetListSize(manualTaskKey);
            if (manualTaskSize > 0) {
                log.info("단말{}있음작업업데이트, 큐크기: {}", terminalId, manualTaskSize);
                return true;
            }

            // 조회작업여부있음데이터
            String scriptTaskKey = RedisKeyUtils.getDispatchTaskStatusKey(terminalId);
            Object scriptTaskValue = RedisUtils.get(scriptTaskKey);
            if (scriptTaskValue != null && "1".equals(scriptTaskValue.toString())) {
                log.info("단말{}있음본작업업데이트", terminalId);
                return true;
            }

            log.debug("단말{}없음작업업데이트", terminalId);
            return false;

        } catch (Exception e) {
            log.error("조회단말{}작업업데이트시발송예외: {}", terminalId, e.getMessage(), e);
            return false;
        }
    }
}
