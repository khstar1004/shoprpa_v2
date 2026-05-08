package com.iflytek.rpa.robot.service.impl;

import static com.iflytek.rpa.market.constants.AuditConstant.AUDIT_ENABLE_STATUS_OFF;
import static com.iflytek.rpa.robot.constants.RobotConstant.EDITING;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.base.dao.*;
import com.iflytek.rpa.base.entity.*;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.component.dao.ComponentRobotBlockDao;
import com.iflytek.rpa.component.dao.ComponentRobotUseDao;
import com.iflytek.rpa.component.entity.ComponentRobotBlock;
import com.iflytek.rpa.component.entity.ComponentRobotUse;
import com.iflytek.rpa.market.entity.vo.AcceptResultVo;
import com.iflytek.rpa.market.entity.vo.LatestVersionRobotVo;
import com.iflytek.rpa.market.service.AppApplicationService;
import com.iflytek.rpa.quota.service.QuotaCheckService;
import com.iflytek.rpa.robot.dao.RobotDesignDao;
import com.iflytek.rpa.robot.dao.RobotExecuteDao;
import com.iflytek.rpa.robot.dao.RobotExecuteRecordDao;
import com.iflytek.rpa.robot.dao.RobotVersionDao;
import com.iflytek.rpa.robot.entity.RobotDesign;
import com.iflytek.rpa.robot.entity.RobotExecute;
import com.iflytek.rpa.robot.entity.RobotVersion;
import com.iflytek.rpa.robot.entity.dto.DeleteDesignDto;
import com.iflytek.rpa.robot.entity.dto.DesignListDto;
import com.iflytek.rpa.robot.entity.dto.ShareDesignDto;
import com.iflytek.rpa.robot.entity.dto.TaskRobotCountDto;
import com.iflytek.rpa.robot.entity.vo.*;
import com.iflytek.rpa.robot.service.RobotDesignService;
import com.iflytek.rpa.task.dao.ScheduleTaskRobotDao;
import com.iflytek.rpa.task.entity.ScheduleTaskRobot;
import com.iflytek.rpa.triggerTask.dao.TriggerTaskDao;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import com.iflytek.rpa.utils.response.QuotaCodeEnum;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

/**
 * 단말봇테이블(Robot)테이블서비스유형
 *
 * @author makejava
 * @since 2024-09-29 15:27:41
 */
@Slf4j
@Service("robotDesignService")
public class RobotDesignServiceImpl extends ServiceImpl<RobotDesignDao, RobotDesign> implements RobotDesignService {
    @Resource
    private RobotDesignDao robotDesignDao;

    @Value("${openapi.workflows-upsert-url:http://openapi-service:8020/workflows/upsert}")
    private String workflowsUpsertUrl;

    @Resource
    private RobotExecuteDao robotExecuteDao;

    @Resource
    private RobotVersionDao robotVersionDao;

    @Autowired
    private CGroupDao groupDao;

    @Resource
    private CElementDao elementDao;

    @Resource
    private CGlobalVarDao globalVarDao;

    @Resource
    private CProcessDao processDao;

    @Resource
    private CRequireDao requireDao;

    @Resource
    private ScheduleTaskRobotDao scheduleTaskRobotDao;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private CProcessDao cProcessDao;

    @Autowired
    private TriggerTaskDao triggerTaskDao;

    @Autowired
    private CParamDao cParamDao;

    @Autowired
    private CModuleDao cModuleDao;

    @Autowired
    private CSmartComponentDao cSmartComponentDao;

    @Autowired
    private ComponentRobotUseDao componentRobotUseDao;

    @Autowired
    private ComponentRobotBlockDao componentRobotBlockDao;

    @Resource
    private RobotExecuteRecordDao robotExecuteRecordDao;

    @Resource
    private AppApplicationService appApplicationService;

    private final String filePathPrefix = "/api/resource/file/download?fileId=";

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Autowired
    private QuotaCheckService quotaCheckService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse createRobot(RobotDesign robot) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        robot.setCreatorId(userId);
        robot.setUpdaterId(userId);
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        robot.setTenantId(tenantId);
        String robotName = robot.getName();
        robotName = robotName.trim();
        if (StringUtils.isBlank(robotName)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "봇이름비워 둘 수 없습니다");
        }
        robot.setName(robotName);
        Long countRobot = robotDesignDao.countRobotByName(robot);
        if (countRobot > 0) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "저장된 이름봇, 요청다시 명령이름");
        }

        // 검증계획기기매칭금액
        if (!quotaCheckService.checkDesignerQuota()) {
            AcceptResultVo resultVo = new AcceptResultVo(QuotaCodeEnum.E_OVER_LIMIT);
            return AppResponse.success(resultVo);
        }

        String robotId = idWorker.nextId() + "";
        robot.setRobotId(robotId);
        robot.setDataSource("create");
        robot.setEditEnable(1);
        robot.setTransformStatus(EDITING);
        robotDesignDao.createRobot(robot);

        // 지우기계획기기수저장(통신경과Redis직선연결지우기)
        String cacheKey = "quota:count:designer:" + tenantId + ":" + userId;
        com.iflytek.rpa.utils.RedisUtils.del(cacheKey);

        // 새생성프로세스,봇버전예0
        CProcess cProcess = new CProcess();
        cProcess.setRobotId(robotId);
        cProcess.setProcessId(idWorker.nextId() + "");
        cProcess.setProcessName("프로세스");
        cProcess.setCreatorId(userId);
        cProcess.setUpdaterId(userId);
        cProcess.setRobotVersion(0);
        cProcessDao.createProcess(cProcess);
        CProcess cProcess1 = new CProcess();
        cProcess1.setRobotId(robotId);
        cProcess1.setProcessId(cProcess.getProcessId());
        return AppResponse.success(cProcess1);
    }

    @Override
    public AppResponse<?> createRobotName() throws NoLoginException {
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
        String robotNameBase = "사용";
        List<String> getRobotNameList = robotDesignDao.getRobotNameList(tenantId, userId, robotNameBase);
        int robotNameIndex = 1;
        List<Integer> robotNameIndexList = new ArrayList<>();
        for (String robotName : getRobotNameList) {
            String[] robotNameSplit = robotName.split(robotNameBase);
            if (robotNameSplit.length == 2 && robotNameSplit[1].matches("^[1-9]\\d*$")) {
                int robotNameNum = Integer.parseInt(robotNameSplit[1]);
                robotNameIndexList.add(robotNameNum);
            }
        }
        Collections.sort(robotNameIndexList);
        for (int i = 0; i < robotNameIndexList.size(); i++) {
            if (robotNameIndexList.get(i) != i + 1) {
                robotNameIndex = i + 1;
                break;
            } else {
                robotNameIndex += 1;
            }
        }
        return AppResponse.success(robotNameBase + robotNameIndex);
    }

    @Override
    public AppResponse<?> designList(DesignListDto queryDto) throws NoLoginException {

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

        Long pageNo = queryDto.getPageNo();
        Long pageSize = queryDto.getPageSize();
        String name = queryDto.getName();
        String sortType = StringUtils.isBlank(queryDto.getSortType()) ? "desc" : queryDto.getSortType();
        String dataSource = queryDto.getDataSource() == null ? "create" : queryDto.getDataSource();

        IPage<RobotDesign> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<RobotDesign> wrapper = new LambdaQueryWrapper<>();

        // userID tenantId 선택
        wrapper.eq(RobotDesign::getCreatorId, userId);
        wrapper.eq(RobotDesign::getTenantId, tenantId);
        wrapper.eq(RobotDesign::getDeleted, 0);

        // dataSource 선택
        wrapper.eq(RobotDesign::getDataSource, dataSource);

        // 이름문자매칭
        if (StringUtils.isNotBlank(name)) {
            wrapper.like(RobotDesign::getName, name);
        }

        // 수정 시간정렬
        if (sortType.equals("asc")) wrapper.orderByAsc(RobotDesign::getUpdateTime);
        else wrapper.orderByDesc(RobotDesign::getUpdateTime);

        IPage<RobotDesign> rePage = this.page(page, wrapper);

        if (CollectionUtils.isEmpty(rePage.getRecords())) return AppResponse.success(rePage);

        IPage<DesignListVo> ansPage = new Page<>(pageNo, pageSize);
        List<DesignListVo> ansRecords = new ArrayList<>();

        ArrayList<String> robotIdList = new ArrayList<>();

        for (RobotDesign record : rePage.getRecords()) {

            DesignListVo designListVo = new DesignListVo();
            designListVo.setRobotName(record.getName());
            designListVo.setUpdateTime(record.getUpdateTime());
            designListVo.setRobotId(record.getRobotId());
            designListVo.setPublishStatus(record.getTransformStatus());
            designListVo.setEditEnable(record.getTransformStatus().equals("locked") ? 0 : 1);

            ansRecords.add(designListVo);
        }

        setAnsRecords(rePage, ansRecords);
        // 위신청상태
        packageApplicationStatus(ansRecords);

        ansPage.setSize(rePage.getSize());
        ansPage.setTotal(rePage.getTotal());
        ansPage.setRecords(ansRecords);

        return AppResponse.success(ansPage);
    }

    private void packageApplicationStatus(List<DesignListVo> ansRecords) throws NoLoginException {
        AppResponse<String> auditStatus = appApplicationService.getAuditStatus();
        if (auditStatus.ok()) {
            if (auditStatus.getData().equals(AUDIT_ENABLE_STATUS_OFF)) {
                ansRecords.forEach(record -> {
                    record.setApplicationStatus(null);
                });
                return;
            }
            // 열기시작완료위검토, 조회위상태
            List<LatestVersionRobotVo> robotVoList = ansRecords.stream()
                    .map(record -> {
                        LatestVersionRobotVo vo = new LatestVersionRobotVo();
                        vo.setRobotId(record.getRobotId());
                        vo.setLatestVersion(record.getLatestVersion());
                        return vo;
                    })
                    .collect(Collectors.toList());

            // 호출방법법가져오기위신청상태
            List<LatestVersionRobotVo> resultList = appApplicationService.getRobotListApplicationStatus(robotVoList);

            Map<String, String> robotStatusMap = resultList.stream()
                    .collect(Collectors.toMap(
                            vo -> vo.getRobotId() + "_" + vo.getLatestVersion(),
                            LatestVersionRobotVo::getApplicationStatus,
                            (existing, replacement) -> existing));
            ansRecords.forEach(record -> {
                String key = record.getRobotId() + "_" + record.getLatestVersion();
                String status = robotStatusMap.get(key);
                record.setApplicationStatus(status != null ? status : "none");
            });
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> rename(String newName, String robotId) throws NoLoginException {

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

        if (StringUtils.isBlank(newName) || StringUtils.isBlank(robotId)) return AppResponse.error("업데이트실패, 새이름문자또는봇Id비어 있습니다");

        // 의빈격식
        newName = trimSpaces(newName);
        String robotName = robotDesignDao.getRobotName(robotId, userId, tenantId);

        if (StringUtils.isBlank(newName)) return AppResponse.error("새이름문자비워 둘 수 없습니다");

        Integer i = robotDesignDao.checkNameDup(userId, tenantId, newName, robotId);
        if (i >= 1) return AppResponse.error("저장에서재복사이름, 요청수정이름");

        boolean b = false;
        // 결과가아니요열기 코드, 아니요수정로editing
        RobotDesign robotDesign = robotDesignDao.getRobot(robotId, userId, tenantId);
        if (robotDesign.getTransformStatus().equals("locked") || newName.equals(robotName)) {
            b = robotDesignDao.updateRobotNameWithoutSetEditing(newName, robotId, userId, tenantId);
        } else {
            b = robotDesignDao.updateRobotName(newName, robotId, userId, tenantId);
        }

        if (b) return AppResponse.success("업데이트성공");
        else return AppResponse.error("업데이트실패");
    }

    @Override
    public AppResponse<?> designNameDup(String newName, String robotId) throws NoLoginException {

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

        if (StringUtils.isNotBlank(newName)) {

            //                String oriRobotName = robotDesignDao.getRobotName(robotId, userId, tenantId);
            //                if (newName.equals(oriRobotName)) return AppResponse.error("할 수 없음및기존이름");
            trimSpaces(newName); // 제거빈격식
            if (StringUtils.isBlank(newName)) return AppResponse.error("새이름문자비워 둘 수 없습니다");

            Integer i = robotDesignDao.checkNameDup(userId, tenantId, newName, robotId);
            if (i >= 1) return AppResponse.error("저장에서재복사이름, 요청수정이름");
            else return AppResponse.success("이름 변경검증통신경과");
        }

        return AppResponse.error("검증실패, 새이름문자비어 있습니다");
    }

    @Override
    public AppResponse<?> myRobotDetail(String robotId) throws NoLoginException {

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

        RobotDesign robot = robotDesignDao.getRobot(robotId, userId, tenantId);
        RobotVersion enableVersion = robotVersionDao.getEnableVersion(robotId, userId, tenantId);

        if (robot.getDataSource().equals("market")) return AppResponse.error("계획기기오류, 확인하세요데이터");

        if (robot == null) return AppResponse.error("봇을 찾을 수 없습니다");

        MyRobotDetailVo resVo = getMyRobotDetailRes(robot, enableVersion);

        return AppResponse.success(resVo);
    }

    @Override
    public AppResponse<?> marketRobotDetail(String robotId) throws Exception {

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

        RobotDesign robot = robotDesignDao.getRobot(robotId, userId, tenantId);
        RobotVersion enableVersion = robotVersionDao.getEnableVersion(robotId, userId, tenantId);

        if (robot.getDataSource().equals("create")) return AppResponse.error("계획기기오류, 확인하세요데이터");

        MarketRobotDetailVo resVo = getMarketRobotDetailRes(robot, enableVersion, userId, tenantId);

        return AppResponse.success(resVo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AppResponse<?> copyDesignRobot(String robotId, String robotName) throws Exception {

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

        RobotDesign robot = robotDesignDao.getRobot(robotId, userId, tenantId);
        if (robot == null) return AppResponse.error(ErrorCodeEnum.E_SQL_EXCEPTION);

        String newName = robot.getName() + "본1";
        if (StringUtils.isNotBlank(robotName)) {
            // 이름 변경검증
            Integer i = robotDesignDao.checkNameDup(userId, tenantId, robotName, robotId);
            if (i >= 1) return AppResponse.error("저장에서재복사이름, 요청수정이름");

            newName = robotName;
        }

        // 검증계획기기매칭금액
        if (!quotaCheckService.checkDesignerQuota()) {
            AcceptResultVo resultVo = new AcceptResultVo(QuotaCodeEnum.E_OVER_LIMIT);
            return AppResponse.success(resultVo);
        }

        // 열기 복사
        designRobotCopy(robot, userId, robotId, newName);

        return AppResponse.success("생성본성공");
    }

    @Override
    public AppResponse<?> deleteRobotRes(String robotId) throws Exception {

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

        RobotExecute robotExecute = robotExecuteDao.getRobotExecute(robotId, userId, tenantId);
        // 가져오기모든사용해당봇의task
        List<ScheduleTaskRobot> taskRobotList = scheduleTaskRobotDao.getAllTaskRobot(robotId, userId, tenantId);

        // 가져오기 결과
        DelDesignRobotVo resVo = getDeleteRobotVo(robotExecute, taskRobotList, robotId, userId, tenantId);

        return AppResponse.success(resVo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AppResponse<?> deleteRobot(DeleteDesignDto queryDto) throws Exception {
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

        Integer situation = queryDto.getSituation();
        String robotId = queryDto.getRobotId();
        String taskIds = queryDto.getTaskIds();

        // 삭제닫기의실행기록: 조회 id, 근거 id 업데이트
        List<Integer> recordIdList = robotExecuteRecordDao.getRecordIds(tenantId, robotId, userId);
        Integer n = 0;
        if (recordIdList != null && !recordIdList.isEmpty()) {
            n = robotExecuteRecordDao.deleteRecordByIds(recordIdList);
        }
        switch (situation) {
            case 1: // 있음계획기기중저장에서
                // 조회 id, 근거 id 업데이트
                Integer designId = robotDesignDao.getDesignId(robotId, userId, tenantId);
                if (designId != null) {
                    robotDesignDao.deleteDesignById(designId);
                    //  openapi 전송삭제요청 
                    sendDeleteRequestToOpenApi(robotId, userId);
                    return AppResponse.success("삭제성공");
                }
            case 2: // 계획기기 실행기기 및 실행기기중저장에서
                // 조회 id, 근거 id 업데이트
                Integer designId2 = robotDesignDao.getDesignId(robotId, userId, tenantId);
                if (designId2 != null) {
                    robotDesignDao.deleteDesignById(designId2);
                }
                Integer executeId = robotExecuteDao.getExecuteId(robotId, userId, tenantId);
                if (executeId != null) {
                    robotExecuteDao.deleteExecuteById(executeId);
                }
                //  openapi 전송삭제요청 
                sendDeleteRequestToOpenApi(robotId, userId);
                return AppResponse.success("삭제성공");
            case 3: // 계획기기,  실행,  예약 작업사용
                if (StringUtils.isBlank(taskIds)) return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK);
                List<String> taskIdList = Arrays.stream(taskIds.split(",")).collect(Collectors.toList());
                // 조회 id, 근거 id 업데이트
                Integer z = 0;
                Integer designId3 = robotDesignDao.getDesignId(robotId, userId, tenantId);
                if (designId3 != null) {
                    robotDesignDao.deleteDesignById(designId3);
                }
                Integer executeId3 = robotExecuteDao.getExecuteId(robotId, userId, tenantId);
                if (executeId3 != null) {
                    robotExecuteDao.deleteExecuteById(executeId3);
                }
                List<Integer> taskRobotIdList =
                        scheduleTaskRobotDao.getTaskRobotIds(robotId, userId, tenantId, taskIdList);
                if (taskRobotIdList != null && !taskRobotIdList.isEmpty()) {
                    z = scheduleTaskRobotDao.taskRobotDeleteByIds(taskRobotIdList);
                }
                taskRobotDeleteAfter(taskIdList);
                int expectedTaskRobotCount = (taskRobotIdList != null) ? taskRobotIdList.size() : 0;
                if (z.equals(expectedTaskRobotCount)) {
                    //  openapi 전송삭제요청 
                    sendDeleteRequestToOpenApi(robotId, userId);
                    return AppResponse.success("삭제성공");
                } else throw new ServiceException("삭제실패");

            default:
                return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK);
        }
    }

    /**
     *  openapi 전송삭제봇의요청 
     *
     * @param robotId 봇ID
     * @param userId   사용자ID
     */
    private void sendDeleteRequestToOpenApi(String robotId, String userId) {
        try {
            log.info("삭제봇시요청 openapi, robotId: {}", robotId);

            // 생성요청 
            JSONObject requestBody = new JSONObject();
            requestBody.put("project_id", robotId);
            requestBody.put("status", 0);

            String requestBodyStr = requestBody.toJSONString();
            log.info("요청 openapi매개변수: {}", requestBodyStr);

            // 생성 RestTemplate 
            RestTemplate restTemplate = new RestTemplate();

            // 요청 
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("user_id", userId);

            // 생성요청 
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBodyStr, headers);

            // 발송 POST 요청 
            ResponseEntity<String> response =
                    restTemplate.exchange(workflowsUpsertUrl, HttpMethod.POST, requestEntity, String.class);

            log.info(
                    "OpenAPI 요청완료, URL: {}, 상태: {}, : {}",
                    workflowsUpsertUrl,
                    response.getStatusCode(),
                    response.getBody());
        } catch (Exception e) {
            log.error("OpenAPI 요청 실패, robotId: {}, 오류정보: {}", robotId, e.getMessage(), e);
            // 아니요출력예외, 삭제
        }
    }

    // 후관리, 조회으로taskIdList로taskId의에서taskRobot중여부저장에서, 결과가찾을 수 없습니다, 이면필요에서schedule task테이블중삭제
    public void taskRobotDeleteAfter(List<String> taskIdList) throws Exception {
        List<TaskRobotCountDto> taskRobotCountDtoList = scheduleTaskRobotDao.taskRobotCount(taskIdList);
        //  taskRobotCountDtoList 및  taskIdList 의 ,   taskIdNotInList중저장요소
        Set<String> taskIdNotInList = taskIdList.stream()
                .filter(taskId -> taskRobotCountDtoList.stream()
                        .noneMatch(taskRobotCountDto ->
                                taskRobotCountDto.getTaskId().equals(taskId)))
                .collect(Collectors.toSet());
        /*for (String taskId : taskIdList) {
            List<TaskRobotCountDto> collect = taskRobotCountDtoList
                    .stream()
                    .filter(taskRobotCountDto -> taskRobotCountDto.getTaskId().equals(taskId))
                    .collect(Collectors.toList());

            if (collect == null || collect.size() == 0){
                taskIdNotInList.add(taskId);
            }
        }*/
        // 삭제아니요저장된 taskRobotschedule task
        Integer i = 0;
        if (!taskIdNotInList.isEmpty()) {
            i = triggerTaskDao.deleteTasks(taskIdNotInList);
        }

        if (!i.equals(taskIdNotInList.size())) {
            throw new ServiceException(ErrorCodeEnum.E_SERVICE.getCode(), "삭제할 데이터가 없습니다");
        }
    }

    private DelDesignRobotVo getDeleteRobotVo(
            RobotExecute robotExecute,
            List<ScheduleTaskRobot> taskRobotList,
            String robotId,
            String userId,
            String tenantId)
            throws Exception {

        DelDesignRobotVo resVo = new DelDesignRobotVo();
        resVo.setRobotId(robotId);

        // 1: 계획기기
        if (robotExecute == null) resVo.setSituation(1);
        // 2: 계획기기 실행기기
        else if (robotExecute != null && (CollectionUtils.isEmpty(taskRobotList))) resVo.setSituation(2);
        // 3: 계획기기 실행기기 예약 작업사용
        else {
            resVo.setSituation(3);
            setDelDesignRobotVo(resVo, taskRobotList, robotId);
        }

        return resVo;
    }

    // 삼
    public void setDelDesignRobotVo(DelDesignRobotVo resVo, List<ScheduleTaskRobot> taskRobotList, String robotId)
            throws Exception {

        List<TaskReferInfo> taskReferInfoList = new ArrayList<>();

        // 가져오기모든사용해당실행기기의taskId
        List<String> taskIdList =
                taskRobotList.stream().map(ScheduleTaskRobot::getTaskId).collect(Collectors.toList());

        // 조회데이터
        List<ScheduleTaskRobot> taskRobots = scheduleTaskRobotDao.getScheduleRobotByTaskIds(taskIdList);
        if (CollectionUtils.isEmpty(taskRobots)) {
            throw new ServiceException(ErrorCodeEnum.E_SERVICE.getCode(), "봇사용닫기시스템예외");
        }

        // 관리데이터
        for (String taskId : taskIdList) {
            TaskReferInfo taskReferInfo = new TaskReferInfo();

            // 선택출력현재taskId의taskRobot
            List<ScheduleTaskRobot> taskRobotsTmp = taskRobots.stream()
                    .filter(taskRobot -> taskRobot.getTaskId().equals(taskId))
                    .collect(Collectors.toList());

            // 통신경과sort필드정렬  정상순서
            taskRobotsTmp.sort((o1, o2) -> o1.getSort().compareTo(o2.getSort()));

            List<String> robotNames = new ArrayList<>();
            List<Integer> highIndex = new ArrayList<>();
            for (int i = 0; i < taskRobotsTmp.size(); i++) {
                ScheduleTaskRobot taskRobot = taskRobotsTmp.get(i);
                String robotNameTmp = taskRobot.getRobotName();
                String robotIdTmp = taskRobot.getRobotId();
                if (robotIdTmp.equals(robotId)) {
                    highIndex.add(i);
                }
                robotNames.add(robotNameTmp);
            }

            taskReferInfo.setTaskId(taskId);
            taskReferInfo.setTaskName(taskRobotsTmp.get(0).getTaskName());
            taskReferInfo.setRobotNames(robotNames);
            taskReferInfo.setHighIndex(highIndex);

            taskReferInfoList.add(taskReferInfo);
        }

        resVo.setTaskReferInfoList(taskReferInfoList);
    }

    public void designRobotCopy(RobotDesign robot, String userId, String robotId, String robotName) throws Exception {

        String newRobotId = String.valueOf(idWorker.nextId());

        robot.setId(null);
        robot.setRobotId(newRobotId);
        robot.setName(robotName);
        robot.setCreateTime(new Date());
        robot.setUpdateTime(new Date());
        robot.setTransformStatus("editing");
        robot.setDataSource("create");
        robotDesignDao.insert(robot);

        // 복사내용, 복사의버전 0 의내용
        copyEditingBase(robotId, newRobotId, userId);
    }

    @Override
    public void copyEditingBase(String oldRobotId, String newRobotId, String userId) throws Exception {
        // 분그룹
        groupCopy(oldRobotId, newRobotId, userId);
        // 요소
        elementCopy(oldRobotId, newRobotId, userId);
        // 전역 변수
        globalValCopy(oldRobotId, newRobotId, userId);
        // 프로세스
        processCopy(oldRobotId, newRobotId, userId);
        // python
        requireCopy(oldRobotId, newRobotId, userId);
        // python모듈
        moduleCopy(oldRobotId, newRobotId, userId);
        // 구성 매개변수
        paramCopy(oldRobotId, newRobotId, userId);
        // 컴포넌트사용데이터
        componentUseCopy(oldRobotId, newRobotId, userId);
        // 컴포넌트데이터
        componentBlockCopy(oldRobotId, newRobotId, userId);
        // 가능컴포넌트
        smartComponentCopy(oldRobotId, newRobotId, userId);
    }

    public void smartComponentCopy(String oldRobotId, String newRobotId, String userId) {
        // 조회봇의smartComponent목록
        List<CSmartComponent> oldSmartComponentList =
                cSmartComponentDao.getAllSmartComponentList(oldRobotId, 0, userId);

        if (CollectionUtils.isEmpty(oldSmartComponentList)) {
            return;
        }

        // 생성id까지새id의
        Map<String, String> oldNewSmartComponentIdMap = new HashMap<>();

        // 복사smartComponent생성닫기시스템
        List<CSmartComponent> smartComponentList = new ArrayList<>();
        for (CSmartComponent oldSmartComponent : oldSmartComponentList) {
            CSmartComponent newSmartComponent = new CSmartComponent();
            newSmartComponent.setSmartId(String.valueOf(idWorker.nextId()));
            newSmartComponent.setRobotId(newRobotId);
            newSmartComponent.setContent(oldSmartComponent.getContent());
            newSmartComponent.setSmartType(oldSmartComponent.getSmartType());
            newSmartComponent.setRobotVersion(oldSmartComponent.getRobotVersion());
            newSmartComponent.setCreatorId(oldSmartComponent.getCreatorId());
            newSmartComponent.setUpdaterId(oldSmartComponent.getUpdaterId());

            oldNewSmartComponentIdMap.put(oldSmartComponent.getSmartId(), newSmartComponent.getSmartId());
            smartComponentList.add(newSmartComponent);
        }

        // 가져오기 봇의process내용
        List<CProcess> oldProcessList = processDao.getProcess(oldRobotId, 0, userId);

        // 결과가있음process, 직선연결삽입smartComponent반환
        if (CollectionUtils.isEmpty(oldProcessList)) {
            cSmartComponentDao.insertBatch(smartComponentList);
            return;
        }

        // 가져오기새봇의process목록(필요업데이트smartId사용)
        List<CProcess> newProcessList = processDao.getProcess(newRobotId, 0, userId);
        // 결과가있음process, 직선연결삽입smartComponent반환
        if (CollectionUtils.isEmpty(newProcessList)) {
            cSmartComponentDao.insertBatch(smartComponentList);
            return;
        }

        // 업데이트모든process content중의smart ID사용(프로세스및하위 프로세스)
        for (int i = 0; i < newProcessList.size() && i < oldProcessList.size(); i++) {
            String currentContent = newProcessList.get(i).getProcessContent();
            // 결과가content비어 있습니다또는null, 건너뛰기관리
            if (currentContent == null || currentContent.trim().isEmpty()) {
                continue;
            }

            // 사용닫기시스템업데이트smartComponent사용(currentContent완료예업데이트경과process ID의버전)
            String newContent = replaceSmartIdsWithMap(currentContent, oldNewSmartComponentIdMap);
            newProcessList.get(i).setProcessContent(newContent);
            processDao.updateById(newProcessList.get(i));
        }

        cSmartComponentDao.insertBatch(smartComponentList);
    }

    public void moduleCopy(String oldRobotId, String newRobotId, String userId) {
        // 조회봇의module목록
        List<CModule> oldModuleList = cModuleDao.getAllModuleListOrderByIdAsc(oldRobotId, 0, userId);
        if (CollectionUtils.isEmpty(oldModuleList)) {
            return;
        }

        // 생성moduleId까지새moduleId의
        Map<String, String> oldNewModuleIdMap = new HashMap<>();

        // 복사module생성닫기시스템
        List<CModule> moduleList = new ArrayList<>();
        for (CModule oldModule : oldModuleList) {
            CModule newModule = new CModule();
            newModule.setModuleId(String.valueOf(idWorker.nextId()));
            newModule.setRobotId(newRobotId);
            newModule.setModuleContent(oldModule.getModuleContent());
            newModule.setModuleName(oldModule.getModuleName());
            newModule.setRobotVersion(oldModule.getRobotVersion());
            newModule.setDeleted(oldModule.getDeleted());
            newModule.setCreatorId(oldModule.getCreatorId());
            newModule.setUpdaterId(oldModule.getUpdaterId());
            newModule.setCreateTime(new Date());
            newModule.setUpdateTime(new Date());

            oldNewModuleIdMap.put(oldModule.getModuleId(), newModule.getModuleId());
            moduleList.add(newModule);
        }

        // 가져오기 봇의process내용
        List<CProcess> oldProcessList = processDao.getProcess(oldRobotId, 0, userId);
        if (CollectionUtils.isEmpty(oldProcessList)) {
            // 결과가있음process, 직선연결삽입module반환
            cModuleDao.insertBatch(moduleList);
            return;
        }

        // 가져오기새봇의process목록(필요업데이트module ID사용)
        List<CProcess> newProcessList = processDao.getProcess(newRobotId, 0, userId);
        if (CollectionUtils.isEmpty(newProcessList)) {
            // 결과가있음process, 직선연결삽입module반환
            cModuleDao.insertBatch(moduleList);
            return;
        }

        // 업데이트모든process content중의module ID사용(프로세스및하위 프로세스)
        for (int i = 0; i < newProcessList.size() && i < oldProcessList.size(); i++) {
            String currentContent = newProcessList.get(i).getProcessContent();
            // 결과가content비어 있습니다또는null, 건너뛰기관리
            if (currentContent == null || currentContent.trim().isEmpty()) {
                continue;
            }

            // 사용닫기시스템업데이트module사용(currentContent완료예업데이트경과process ID의버전)
            String newContent = replaceModuleIdsWithMap(currentContent, oldNewModuleIdMap);
            newProcessList.get(i).setProcessContent(newContent);
            processDao.updateById(newProcessList.get(i));
        }

        cModuleDao.insertBatch(moduleList);
    }

    public void paramCopy(String oldRobotId, String newRobotId, String userId) {
        List<CParam> params = cParamDao.getParams(oldRobotId, userId);
        copyProcessParam(oldRobotId, newRobotId, userId, params);
        copyModuleParam(oldRobotId, newRobotId, userId, params);
    }

    private void copyProcessParam(String oldRobotId, String newRobotId, String userId, List<CParam> params) {
        List<CParam> processParams = params.stream()
                .filter(param -> StringUtils.isNotEmpty(param.getProcessId()))
                .collect(Collectors.toList());
        processParams.removeIf(Objects::isNull);
        if (CollectionUtils.isEmpty(processParams)) return;
        // 기존봇의프로세스id  및  본봇의프로세스id 의Map:(k,v) 로 (oldProcessId,newProcessId)
        List<CProcess> oldProcessList = processDao.getProcess(oldRobotId, 0, userId);
        List<CProcess> newProcessList = processDao.getProcess(newRobotId, 0, userId);
        Map<String, String> oldNewProcessIdMap = getOldNewProcessIdMap(newProcessList, oldProcessList);
        for (CParam cParam : processParams) {
            cParam.setId(idWorker.nextId() + "");
            cParam.setRobotId(newRobotId);
            // 보관인증하위 프로세스의processId및구성 매개변수 
            cParam.setProcessId(oldNewProcessIdMap.get(cParam.getProcessId()));
            cParam.setRobotVersion(0); // 새버전로0
            cParam.setCreateTime(new Date());
            cParam.setUpdateTime(new Date());
            cParam.setCreatorId(userId);
            cParam.setUpdaterId(userId);
            cParam.setDeleted(0);
        }
        if (!processParams.isEmpty()) {
            cParamDao.insertParamBatch(processParams);
        }
    }

    private void copyModuleParam(String oldRobotId, String newRobotId, String userId, List<CParam> params) {
        List<CParam> moduleParams = params.stream()
                .filter(param -> StringUtils.isNotEmpty(param.getModuleId()))
                .collect(Collectors.toList());
        moduleParams.removeIf(Objects::isNull);
        if (CollectionUtils.isEmpty(moduleParams)) return;
        // 기존봇의프로세스id  및  본봇의프로세스id 의Map:(k,v) 로 (oldProcessId,newProcessId)
        List<CModule> oldModuleList = cModuleDao.getAllModuleListOrderByIdAsc(oldRobotId, 0, userId);
        List<CModule> newModuleList = cModuleDao.getAllModuleListOrderByIdAsc(newRobotId, 0, userId);
        Map<String, String> OldNewModuleIdMap = getOldNewModuleIdMap(newModuleList, oldModuleList);
        for (CParam cParam : moduleParams) {
            cParam.setId(idWorker.nextId() + "");
            cParam.setRobotId(newRobotId);
            // 보관인증하위 프로세스의moduleId및구성 매개변수 
            cParam.setModuleId(OldNewModuleIdMap.get(cParam.getModuleId()));
            cParam.setRobotVersion(0); // 새버전로0
            cParam.setCreateTime(new Date());
            cParam.setUpdateTime(new Date());
            cParam.setCreatorId(userId);
            cParam.setUpdaterId(userId);
            cParam.setDeleted(0);
        }
        if (!moduleParams.isEmpty()) {
            cParamDao.insertParamBatch(moduleParams);
        }
    }

    private Map<String, String> getOldNewModuleIdMap(List<CModule> newModuleList, List<CModule> oldModuleList) {
        if (newModuleList.size() != oldModuleList.size()) throw new ServiceException(ErrorCodeEnum.E_SQL.getCode());

        Map<String, String> OldNewModuleIdMap = new HashMap<>();
        for (int i = 0; i < oldModuleList.size(); i++) {
            OldNewModuleIdMap.put(
                    oldModuleList.get(i).getModuleId(), newModuleList.get(i).getModuleId());
        }

        return OldNewModuleIdMap;
    }

    private Map<String, String> getOldNewProcessIdMap(List<CProcess> newProcessList, List<CProcess> oldProcessList) {
        if (newProcessList.size() != oldProcessList.size()) throw new ServiceException(ErrorCodeEnum.E_SQL.getCode());

        Map<String, String> oldNewProcessIdMap = new HashMap<>();
        for (int i = 0; i < oldProcessList.size(); i++) {
            oldNewProcessIdMap.put(
                    oldProcessList.get(i).getProcessId(), newProcessList.get(i).getProcessId());
        }

        return oldNewProcessIdMap;
    }

    public void groupCopy(String oldRobotId, String newRobotId, String userId) {
        groupDao.copyGroupBatch(oldRobotId, newRobotId, userId);
    }

    public void elementCopy(String oldRobotId, String newRobotId, String userId) {

        List<CElement> elementList = elementDao.getElement(oldRobotId, 0, userId);
        if (CollectionUtils.isEmpty(elementList)) return;

        for (CElement element : elementList) {
            //            String nextId = String.valueOf(idWorker.nextId());

            element.setId(null);
            //            element.setElementId(nextId);
            element.setRobotId(newRobotId);
            element.setCreateTime(new Date());
            element.setUpdateTime(new Date());
        }

        // 후량삽입
        elementDao.insertEleBatch(elementList);
    }

    public void globalValCopy(String oldRobotId, String newRobotId, String userId) {

        List<CGlobalVar> globalVarList = globalVarDao.getGlobalVar(oldRobotId, 0, userId);
        if (CollectionUtils.isEmpty(globalVarList)) return;

        for (CGlobalVar globalVar : globalVarList) {
            String nextId = String.valueOf(idWorker.nextId());

            globalVar.setId(null);
            globalVar.setGlobalId(nextId);
            globalVar.setRobotId(newRobotId);
            globalVar.setCreateTime(new Date());
            globalVar.setUpdateTime(new Date());
        }

        globalVarDao.insertGloBatch(globalVarList);
    }

    public void processCopy(String oldRobotId, String newRobotId, String userId) throws Exception {
        // 조회봇의process목록
        List<CProcess> oldProcessList = processDao.getProcess(oldRobotId, 0, userId);
        if (CollectionUtils.isEmpty(oldProcessList)) {
            throw new ServiceException("복사할 프로세스를 찾을 수 없습니다");
        }

        // 생성processId까지새processId의
        Map<String, String> oldNewProcessIdMap = new HashMap<>();

        // 복사process생성닫기시스템
        List<CProcess> processList = new ArrayList<>();
        for (CProcess oldProcess : oldProcessList) {
            CProcess newProcess = new CProcess();
            newProcess.setProcessId(String.valueOf(idWorker.nextId()));
            newProcess.setRobotId(newRobotId);
            newProcess.setProcessContent(oldProcess.getProcessContent());
            newProcess.setProcessName(oldProcess.getProcessName());
            newProcess.setRobotVersion(oldProcess.getRobotVersion());
            newProcess.setDeleted(oldProcess.getDeleted());
            newProcess.setCreatorId(oldProcess.getCreatorId());
            newProcess.setUpdaterId(oldProcess.getUpdaterId());
            newProcess.setCreateTime(new Date());
            newProcess.setUpdateTime(new Date());

            // 생성processId까지새processId의
            oldNewProcessIdMap.put(oldProcess.getProcessId(), newProcess.getProcessId());
            processList.add(newProcess);
        }

        // 프로세스중하위 프로세스의사용다시 완료새의하위 프로세스 processId
        subProcessWrite(processList, oldProcessList, oldNewProcessIdMap);

        processDao.insertProcessBatch(processList);
    }

    /**
     * 프로세스중하위 프로세스의사용다시 완료새의하위 프로세스 processId
     *
     * @param processList
     * @param oldProcessList
     * @param oldNewProcessIdMap
     */
    public void subProcessWrite(
            List<CProcess> processList, List<CProcess> oldProcessList, Map<String, String> oldNewProcessIdMap) {
        // 설명찾을 수 없습니다하위 프로세스
        if (processList.size() == 1) return;
        if (processList.size() != oldProcessList.size())
            throw new ServiceException("the size of new process is not equal to the size of old process");

        // 매개프로세스, 사용정상이면설치전체process사용
        for (int i = 0; i < oldProcessList.size(); i++) {
            String oldContent = oldProcessList.get(i).getProcessContent();
            // 결과가content비어 있습니다, 직선연결사용
            if (oldContent == null || oldContent.trim().isEmpty()) {
                processList.get(i).setProcessContent(oldContent);
                continue;
            }

            // 사용정상이면테이블방식설치전체process사용
            String newContent = replaceProcessIdsWithMap(oldContent, oldNewProcessIdMap);
            processList.get(i).setProcessContent(newContent);
        }
    }

    private String replaceModuleIds(String processContent, List<String> moduleIdList) {
        if (processContent == null || moduleIdList == null) {
            throw new IllegalArgumentException("processContent and moduleIdList cannot be null.");
        }

        String patternString = Pattern.quote("\"key\":\"content\",\"value\":\"") + "(\\d+)";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(processContent);

        StringBuffer resultBuffer = new StringBuffer();
        int matchCount = 0;

        // 조회모든매칭
        while (matcher.find()) {
            matchCount++;
            if (matchCount > moduleIdList.size()) {
                // 결과가매칭까지의위치수초과경과완료의ID수, 설명아니요매칭
                throw new IllegalArgumentException("Number of matched positions (" + matchCount
                        + ") exceeds the size of moduleIdList (" + moduleIdList.size() + ").");
            }
            // 가져오기현재매칭까지의ID
            String replacementId = moduleIdList.get(moduleIdList.size() - matchCount); // moduleIdList 예0-indexed

            // 생성텍스트: 모듈분 + 의ID
            String replacementText = "\"key\":\"content\",\"value\":\"" + replacementId;
            matcher.appendReplacement(resultBuffer, replacementText);
        }

        // 조회매칭까지의위치수여부및moduleIdList의size
        if (matchCount != moduleIdList.size()) log.info("프로세스모듈가능있음사용전체");

        // 를의문자열추가 입력까지결과중
        matcher.appendTail(resultBuffer);

        return resultBuffer.toString();
    }

    /**
     * 사용IDmodule사용, 지요소일개module다중사용
     */
    private String replaceModuleIdsWithMap(String processContent, Map<String, String> oldNewModuleIdMap) {
        if (processContent == null || oldNewModuleIdMap == null || oldNewModuleIdMap.isEmpty()) {
            if (processContent == null) {
                throw new IllegalArgumentException("processContent cannot be null.");
            }
            return processContent;
        }

        String patternString = Pattern.quote("\"key\":\"content\",\"value\":\"") + "(\\d+)";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(processContent);

        StringBuffer resultBuffer = new StringBuffer();

        // 조회모든매칭
        while (matcher.find()) {
            String oldModuleId = matcher.group(1);
            String newModuleId = oldNewModuleIdMap.get(oldModuleId);

            // 결과가까지닫기시스템, 사용새의ID;아니요이면보관기존ID
            if (newModuleId != null) {
                String replacementText = "\"key\":\"content\",\"value\":\"" + newModuleId;
                matcher.appendReplacement(resultBuffer, replacementText);
            } else {
                // 결과가있음까지닫기시스템, 보관기존
                matcher.appendReplacement(resultBuffer, matcher.group(0));
            }
        }

        // 를의문자열추가 입력까지결과중
        matcher.appendTail(resultBuffer);

        return resultBuffer.toString();
    }

    /**
     * 사용IDsmartComponent사용, 지요소일개smartComponent다중사용
     */
    private String replaceSmartIdsWithMap(String processContent, Map<String, String> oldNewSmartComponentIdMap) {
        if (processContent == null || oldNewSmartComponentIdMap == null || oldNewSmartComponentIdMap.isEmpty()) {
            if (processContent == null) {
                throw new IllegalArgumentException("processContent cannot be null.");
            }
            return processContent;
        }

        String patternString = "\"key\":\"Smart\\.run_code\\.(\\d+)\"";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(processContent);

        StringBuffer resultBuffer = new StringBuffer();

        // 조회모든매칭
        while (matcher.find()) {
            String oldSmartId = matcher.group(1);
            String newSmartId = oldNewSmartComponentIdMap.get(oldSmartId);

            // 결과가까지닫기시스템, 사용새의ID;아니요이면보관기존ID
            if (newSmartId != null) {
                String replacementText = String.format("\"key\":\"Smart.run_code.%s\"", newSmartId);
                matcher.appendReplacement(resultBuffer, replacementText);
            } else {
                // 결과가있음까지닫기시스템, 보관기존
                matcher.appendReplacement(resultBuffer, matcher.group(0));
            }
        }

        // 를의문자열추가 입력까지결과중
        matcher.appendTail(resultBuffer);

        return resultBuffer.toString();
    }

    private String replaceProcessIds(String processContent, List<String> processIdList) {
        if (processContent == null || processIdList == null) {
            throw new IllegalArgumentException("processContent and processIdList cannot be null.");
        }

        String patternString = Pattern.quote("\"key\":\"process\",\"value\":\"") + "(\\d+)";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(processContent);

        StringBuffer resultBuffer = new StringBuffer();
        int matchCount = 0;

        // 조회모든매칭
        while (matcher.find()) {
            matchCount++;
            if (matchCount > processIdList.size()) {
                // 결과가매칭까지의위치수초과경과완료의ID수, 설명아니요매칭
                throw new IllegalArgumentException("Number of matched positions (" + matchCount
                        + ") exceeds the size of processIdList (" + processIdList.size() + ").");
            }
            // 가져오기현재매칭까지의ID
            String replacementId = processIdList.get(matchCount - 1); // processIdList 예0-indexed

            // 생성텍스트: 모듈분 + 의ID
            String replacementText = "\"key\":\"process\",\"value\":\"" + replacementId;
            matcher.appendReplacement(resultBuffer, replacementText);
        }

        // 조회매칭까지의위치수여부및processIdList의size
        if (matchCount != processIdList.size()) log.info("프로세스하위 프로세스가능있음사용전체");

        // 를의문자열추가 입력까지결과중
        matcher.appendTail(resultBuffer);

        return resultBuffer.toString();
    }

    /**
     * 사용IDprocess사용, 지요소일개process다중사용
     */
    private String replaceProcessIdsWithMap(String processContent, Map<String, String> oldNewProcessIdMap) {
        if (processContent == null || oldNewProcessIdMap == null || oldNewProcessIdMap.isEmpty()) {
            if (processContent == null) {
                throw new IllegalArgumentException("processContent cannot be null.");
            }
            return processContent;
        }

        String patternString = Pattern.quote("\"key\":\"process\",\"value\":\"") + "(\\d+)";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(processContent);

        StringBuffer resultBuffer = new StringBuffer();

        // 조회모든매칭
        while (matcher.find()) {
            String oldProcessId = matcher.group(1);
            String newProcessId = oldNewProcessIdMap.get(oldProcessId);

            // 결과가까지닫기시스템, 사용새의ID;아니요이면보관기존ID
            if (newProcessId != null) {
                String replacementText = "\"key\":\"process\",\"value\":\"" + newProcessId;
                matcher.appendReplacement(resultBuffer, replacementText);
            } else {
                // 결과가있음까지닫기시스템, 보관기존
                matcher.appendReplacement(resultBuffer, matcher.group(0));
            }
        }

        // 를의문자열추가 입력까지결과중
        matcher.appendTail(resultBuffer);

        return resultBuffer.toString();
    }

    public void requireCopy(String oldRobotId, String newRobotId, String userId) {
        List<CRequire> requireList = requireDao.getRequire(oldRobotId, 0, userId);
        if (CollectionUtils.isEmpty(requireList)) return;

        for (CRequire require : requireList) {

            require.setId(null);
            require.setRobotId(newRobotId);
            require.setCreateTime(new Date());
            require.setUpdateTime(new Date());
        }

        requireDao.insertReqBatch(requireList);
    }

    private MarketRobotDetailVo getMarketRobotDetailRes(
            RobotDesign robot, RobotVersion enableVersion, String userId, String tenantId) throws Exception {

        String appId = robot.getAppId();
        Integer appVersion = robot.getAppVersion();

        MyRobotDetailVo myRobotDetailRes = getMyRobotDetailRes(robot, enableVersion);

        // useDescription 및 file닫기정보
        setAddInfo(myRobotDetailRes, robot);

        String robotId = robotDesignDao.getRobotIdFromAppResourceRegardlessDel(appId);

        List<RobotVersion> allVersion = robotVersionDao.getAllVersionWithoutUserId(robotId, tenantId);
        List<VersionInfo> versionInfoList = new ArrayList<>();

        for (int i = 0; i < allVersion.size(); i++) {
            RobotVersion robotVersion = allVersion.get(i);
            Integer online = 0;

            VersionInfo versionInfo = new VersionInfo();
            versionInfo.setVersionNum(robotVersion.getVersion());
            versionInfo.setCreateTime(robotVersion.getCreateTime());

            // 예가져오기시의version
            online = appVersion.equals(robotVersion.getVersion()) ? 1 : 0;

            versionInfo.setOnline(online);

            versionInfoList.add(versionInfo);
        }

        MarketRobotDetailVo resVo = new MarketRobotDetailVo();
        resVo.setMyRobotDetailVo(myRobotDetailRes);
        resVo.setSourceName("팀마켓");
        resVo.setVersionInfoList(versionInfoList);

        return resVo;
    }

    private void setAddInfo(MyRobotDetailVo myRobotDetailRes, RobotDesign robot) throws Exception {
        String appId = robot.getAppId();
        String sourceRobotId = robotDesignDao.getRobotIdFromAppResourceRegardlessDel(appId);

        RobotVersion latestRobotVersion = robotVersionDao.getLatestVersionRegardlessDel(sourceRobotId);
        if (latestRobotVersion == null) {
            throw new ServiceException("원본 로봇의 최신 버전을 찾을 수 없습니다");
        }

        String fileId = latestRobotVersion.getAppendixId();
        String videoId = latestRobotVersion.getVideoId();
        String fileName = robotExecuteDao.getFileName(fileId);
        String videoName = robotExecuteDao.getFileName(videoId);

        myRobotDetailRes.setUseDescription(latestRobotVersion.getUseDescription());
        myRobotDetailRes.setFileName(fileName);
        myRobotDetailRes.setFilePath(StringUtils.isEmpty(fileId) ? null : (filePathPrefix + fileId));
        myRobotDetailRes.setVideoName(videoName);
        myRobotDetailRes.setVideoPath(StringUtils.isEmpty(videoId) ? null : (filePathPrefix + videoId));
    }

    private MyRobotDetailVo getMyRobotDetailRes(RobotDesign robot, RobotVersion enableVersion) {
        MyRobotDetailVo resVo = new MyRobotDetailVo();
        String introduction = "";
        Integer version = 0;
        String fileId = null;
        String videoId = null;
        String fileName = null;
        String videoName = null;
        String useDescription = null;

        if (enableVersion != null) {
            introduction = enableVersion.getIntroduction();
            version = enableVersion.getVersion();
            fileId = enableVersion.getAppendixId();
            videoId = enableVersion.getVideoId();
            fileName = robotExecuteDao.getFileName(fileId);
            videoName = robotExecuteDao.getFileName(videoId);
            useDescription = enableVersion.getUseDescription();
        }

        AppResponse<String> realNameResp = rpaAuthFeign.getNameById(robot.getCreatorId());
        if (realNameResp == null || realNameResp.getData() == null) {
            throw new ServiceException("사용자명가져오기실패");
        }
        String creatorName = realNameResp.getData();

        resVo.setName(robot.getName());
        resVo.setVersion(version);
        resVo.setIntroduction(introduction);
        resVo.setUseDescription(useDescription);
        resVo.setCreatorName(creatorName);
        resVo.setCreateTime(robot.getCreateTime());
        resVo.setFileName(fileName);
        resVo.setFilePath(StringUtils.isEmpty(fileId) ? null : (filePathPrefix + fileId));
        resVo.setVideoName(videoName);
        resVo.setVideoPath(StringUtils.isEmpty(videoId) ? null : (filePathPrefix + videoId));

        return resVo;
    }

    private void setAnsRecords(IPage<RobotDesign> rePage, List<DesignListVo> ansRecords) {

        List<RobotDesign> robotDesignList = rePage.getRecords();

        List<String> robotIdList =
                robotDesignList.stream().map(RobotDesign::getRobotId).collect(Collectors.toList());

        List<RobotVersion> robotVersionList = robotDesignDao.getRobotVersionList(robotIdList);

        for (DesignListVo ansRecord : ansRecords) {
            String robotId = ansRecord.getRobotId();

            // 필터링출력현재robotId의robotVersion
            List<RobotVersion> robotVersionsTmp = robotVersionList.stream()
                    .filter(robotVersion -> robotVersion.getRobotId().equals(robotId))
                    .collect(Collectors.toList());

            if (robotVersionsTmp.size() == 0 || robotVersionsTmp == null) {
                // 설명있음발송경과버전,  팀마켓가져오기의계획기기있음버전, 및제품품목경과완료
                ansRecord.setVersion(0);
            } else {

                List<RobotVersion> enableList = robotVersionsTmp.stream()
                        .filter(robotVersion1 -> robotVersion1.getOnline().equals(1))
                        .collect(Collectors.toList());

                if (CollectionUtils.isEmpty(enableList))
                    throw new ServiceException(ErrorCodeEnum.E_SQL_EXCEPTION.getCode(), "데이터예외, 발송경과버전의봇없음사용버전");

                // 발송경과버전의, 사용사용버전
                RobotVersion enableRobotVersion = enableList.get(0);

                // 발송경과버전의, 새버전
                Optional<RobotVersion> optionalRobotVersion =
                        robotVersionsTmp.stream().max(Comparator.comparing(RobotVersion::getVersion));

                ansRecord.setLatestVersion(optionalRobotVersion.get().getVersion());
                ansRecord.setVersion(enableRobotVersion.getVersion());
                ansRecord.setIconUrl(enableRobotVersion.getIcon());
            }
        }
    }

    public String trimSpaces(String input) {
        if (input == null) {
            return null;
        }
        return input.trim();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AppResponse<?> shareRobot(ShareDesignDto queryDto) throws Exception {

        String robotId = queryDto.getRobotId();
        String sharedUserId = queryDto.getSharedUserId();
        String sharedTenantId = queryDto.getSharedTenantId();
        String receivedUserId = queryDto.getReceivedUserId();
        String receivedTenantId = queryDto.getReceivedTenantId();

        // 가져오기필요공유의봇
        RobotDesign robot = robotDesignDao.getRobot(robotId, sharedUserId, sharedTenantId);
        if (robot == null) return AppResponse.error(ErrorCodeEnum.E_SQL_EXCEPTION);
        String receivedRobotName = robot.getName() + "공유";
        // 조회수신사용자 봇여부재이름
        Integer i = robotDesignDao.checkNameDupWithoutRobotId(receivedUserId, receivedTenantId, receivedRobotName);
        while (i > 1) {
            receivedRobotName += "공유";
            i = robotDesignDao.checkNameDupWithoutRobotId(receivedUserId, receivedTenantId, receivedRobotName);
        }

        String newRobotId = designRobotShare(robot, sharedUserId, receivedUserId, receivedTenantId, receivedRobotName);
        return AppResponse.success(newRobotId);
    }

    /**
     * 계획기기공유
     *
     * @param robot             공유의봇
     * @param receivedUserId    수신봇의사용자id
     * @param receivedTenantId  수신봇의사용자의테넌트id
     * @param receivedRobotName 봇이름
     */
    public String designRobotShare(
            RobotDesign robot,
            String sharedUserId,
            String receivedUserId,
            String receivedTenantId,
            String receivedRobotName)
            throws Exception {
        String oldRobotId = robot.getRobotId();
        // 수정 robotDesign 의 정보
        robot.setId(null);
        String newRobotId = String.valueOf(idWorker.nextId());
        robot.setRobotId(newRobotId);
        robot.setName(receivedRobotName);
        // 테넌트id
        robot.setTenantId(receivedTenantId);
        // 사용자id
        robot.setCreatorId(receivedUserId);
        robot.setUpdaterId(receivedUserId);
        robot.setCreateTime(new Date());
        robot.setUpdateTime(new Date());
        robot.setTransformStatus("editing");
        robot.setDataSource("create");
        robotDesignDao.insert(robot);
        // 닫기의데이터
        shareRobotBaseInfo(oldRobotId, newRobotId, sharedUserId, receivedUserId);

        return robot.getRobotId();
    }

    public void shareRobotBaseInfo(String oldRobotId, String newRobotId, String sharedUserId, String receivedUserId)
            throws Exception {
        // 분그룹
        groupShare(oldRobotId, sharedUserId, newRobotId, receivedUserId);
        // 요소
        elementShare(oldRobotId, sharedUserId, newRobotId, receivedUserId);
        // 전역 변수
        globalValShare(oldRobotId, sharedUserId, newRobotId, receivedUserId);
        // 프로세스
        processShare(oldRobotId, sharedUserId, newRobotId, receivedUserId);
        // python
        requireShare(oldRobotId, sharedUserId, newRobotId, receivedUserId);
        // 구성 매개변수
        paramShare(oldRobotId, sharedUserId, newRobotId, receivedUserId);
    }

    private void groupShare(String oldRobotId, String sharedUserId, String newRobotId, String receivedUserId) {
        groupDao.shareGroupBatch(oldRobotId, sharedUserId, newRobotId, receivedUserId);
    }

    private void elementShare(String oldRobotId, String sharedUserId, String newRobotId, String receivedUserId) {
        List<CElement> elementList = elementDao.getElement(oldRobotId, 0, sharedUserId);
        if (CollectionUtils.isEmpty(elementList)) return;
        for (CElement element : elementList) {
            element.setId(null);
            element.setRobotId(newRobotId);
            element.setCreateTime(new Date());
            element.setUpdateTime(new Date());
            element.setCreatorId(receivedUserId);
            element.setUpdaterId(receivedUserId);
        }
        elementDao.insertEleBatch(elementList);
    }

    private void globalValShare(String oldRobotId, String sharedUserId, String newRobotId, String receivedUserId) {
        List<CGlobalVar> globalVarList = globalVarDao.getGlobalVar(oldRobotId, 0, sharedUserId);
        if (CollectionUtils.isEmpty(globalVarList)) return;
        for (CGlobalVar globalVar : globalVarList) {
            String nextId = String.valueOf(idWorker.nextId());
            globalVar.setId(null);
            globalVar.setGlobalId(nextId);
            globalVar.setRobotId(newRobotId);
            globalVar.setCreateTime(new Date());
            globalVar.setUpdateTime(new Date());
            globalVar.setCreatorId(receivedUserId);
            globalVar.setUpdaterId(receivedUserId);
        }
        globalVarDao.insertGloBatch(globalVarList);
    }

    private void processShare(String oldRobotId, String sharedUserId, String newRobotId, String receivedUserId)
            throws Exception {
        List<CProcess> processList = processDao.getProcess(oldRobotId, 0, sharedUserId);
        if (CollectionUtils.isEmpty(processList)) {
            throw new ServiceException("공유할 프로세스를 찾을 수 없습니다");
        }
        for (CProcess process : processList) {
            String nextId = String.valueOf(idWorker.nextId());
            process.setId(null);
            process.setProcessId(nextId);
            process.setRobotId(newRobotId);
            process.setCreateTime(new Date());
            process.setUpdateTime(new Date());
            process.setCreatorId(receivedUserId);
            process.setUpdaterId(receivedUserId);
        }
        processDao.insertProcessBatch(processList);
    }

    private void requireShare(String oldRobotId, String sharedUserId, String newRobotId, String receivedUserId) {
        List<CRequire> requireList = requireDao.getRequire(oldRobotId, 0, sharedUserId);
        if (CollectionUtils.isEmpty(requireList)) return;
        for (CRequire require : requireList) {
            require.setId(null);
            require.setRobotId(newRobotId);
            require.setCreateTime(new Date());
            require.setUpdateTime(new Date());
            require.setCreatorId(receivedUserId);
            require.setUpdaterId(receivedUserId);
        }
        requireDao.insertReqBatch(requireList);
    }

    private void paramShare(String oldRobotId, String sharedUserId, String newRobotId, String receivedUserId) {
        List<CParam> params = cParamDao.getParams(oldRobotId, sharedUserId);
        // 기존봇의프로세스id  및  본봇의프로세스id 의Map:(k,v) 로 (oldProcessId,newProcessId)
        List<CProcess> oldProcessList = processDao.getProcess(oldRobotId, 0, sharedUserId);
        List<CProcess> newProcessList = processDao.getProcess(newRobotId, 0, receivedUserId);
        Map<String, String> oldNewProcessIdMap = getOldNewProcessIdMap(newProcessList, oldProcessList);
        for (CParam cParam : params) {
            cParam.setId(idWorker.nextId() + "");
            cParam.setRobotId(newRobotId);
            // 보관인증하위 프로세스의processId및구성 매개변수 
            cParam.setProcessId(oldNewProcessIdMap.get(cParam.getProcessId()));
            cParam.setRobotVersion(0); // 새버전로0
            cParam.setCreateTime(new Date());
            cParam.setUpdateTime(new Date());
            cParam.setCreatorId(receivedUserId);
            cParam.setUpdaterId(receivedUserId);
            cParam.setDeleted(0);
        }
        if (!params.isEmpty()) {
            cParamDao.insertParamBatch(params);
        }
    }

    /**
     * 복사컴포넌트사용데이터
     *
     * @param oldRobotId 기존봇ID
     * @param newRobotId 새봇ID
     * @param userId     사용자ID
     */
    private void componentUseCopy(String oldRobotId, String newRobotId, String userId) {
        // 조회기존봇의컴포넌트사용기록(버전0)
        List<ComponentRobotUse> componentRobotUseList =
                componentRobotUseDao.getComponentRobotUse(oldRobotId, 0, userId);
        if (CollectionUtils.isEmpty(componentRobotUseList)) return;

        // 관리매기록: id로null, robotId수정로새ID, 수정 시간
        for (ComponentRobotUse componentRobotUse : componentRobotUseList) {
            componentRobotUse.setId(null);
            componentRobotUse.setRobotId(newRobotId);
            componentRobotUse.setCreateTime(new Date());
            componentRobotUse.setUpdateTime(new Date());
        }

        // 량삽입새기록
        componentRobotUseDao.insertBatch(componentRobotUseList);
    }

    /**
     * 복사컴포넌트데이터
     *
     * @param oldRobotId 기존봇ID
     * @param newRobotId 새봇ID
     * @param userId     사용자ID
     */
    private void componentBlockCopy(String oldRobotId, String newRobotId, String userId) {
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        // 조회기존봇의컴포넌트기록(버전0)
        List<ComponentRobotBlock> componentRobotBlockList =
                componentRobotBlockDao.getComponentRobotBlockForCopy(oldRobotId, 0, tenantId);
        if (CollectionUtils.isEmpty(componentRobotBlockList)) return;

        // 관리매기록: id로null, robotId수정로새ID, 수정 시간
        for (ComponentRobotBlock componentRobotBlock : componentRobotBlockList) {
            componentRobotBlock.setId(null);
            componentRobotBlock.setRobotId(newRobotId);
            componentRobotBlock.setCreateTime(new Date());
            componentRobotBlock.setUpdateTime(new Date());
        }

        // 량삽입새기록
        componentRobotBlockDao.insertBatch(componentRobotBlockList);
    }
}
