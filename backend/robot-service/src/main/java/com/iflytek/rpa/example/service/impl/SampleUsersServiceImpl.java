package com.iflytek.rpa.example.service.impl;

import static com.iflytek.rpa.robot.constants.RobotConstant.EXECUTOR;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.iflytek.rpa.base.dao.CElementDao;
import com.iflytek.rpa.base.dao.CGroupDao;
import com.iflytek.rpa.base.dao.CParamDao;
import com.iflytek.rpa.base.dao.CProcessDao;
import com.iflytek.rpa.base.entity.CElement;
import com.iflytek.rpa.base.entity.CGroup;
import com.iflytek.rpa.base.entity.CParam;
import com.iflytek.rpa.base.entity.CProcess;
import com.iflytek.rpa.base.entity.dto.ParamDto;
import com.iflytek.rpa.base.entity.dto.QueryParamDto;
import com.iflytek.rpa.base.service.handler.ExecutorModeHandler;
import com.iflytek.rpa.example.constants.ExampleConstants;
import com.iflytek.rpa.example.dao.SampleTemplatesDao;
import com.iflytek.rpa.example.dao.SampleUsersDao;
import com.iflytek.rpa.example.entity.Do.NewExampleDo;
import com.iflytek.rpa.example.entity.Dto.WorkflowsUpsertDto;
import com.iflytek.rpa.example.entity.SampleTemplates;
import com.iflytek.rpa.example.entity.SampleUsers;
import com.iflytek.rpa.example.service.SampleUsersService;
import com.iflytek.rpa.robot.dao.RobotDesignDao;
import com.iflytek.rpa.robot.dao.RobotExecuteDao;
import com.iflytek.rpa.robot.dao.RobotVersionDao;
import com.iflytek.rpa.robot.entity.RobotDesign;
import com.iflytek.rpa.robot.entity.RobotExecute;
import com.iflytek.rpa.robot.entity.RobotVersion;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.RedisUtils;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

/**
 * 사용자에서시스템중비고입력의데이터(SampleUsers)테이블서비스유형
 *
 * @author makejava
 * @since 2024-12-19
 */
@Service
public class SampleUsersServiceImpl extends ServiceImpl<SampleUsersDao, SampleUsers> implements SampleUsersService {

    private static final Logger log = LoggerFactory.getLogger(SampleUsersServiceImpl.class);

    @Autowired
    private SampleTemplatesDao sampleTemplatesDao;

    @Autowired
    private SampleUsersDao sampleUsersDao;

    @Autowired
    private RobotDesignDao robotDesignDao;

    @Autowired
    private RobotExecuteDao robotExecuteDao;

    @Autowired
    private RobotVersionDao robotVersionDao;

    @Autowired
    private CProcessDao cProcessDao;

    @Autowired
    private CParamDao cParamDao;

    @Autowired
    private CGroupDao cGroupDao;

    @Autowired
    private CElementDao cElementDao;

    @Autowired
    private ExecutorModeHandler executorModeHandler;

    @Autowired
    private IdWorker idWorker;

    @Value("${example.expoUserId}")
    private String expoUserId;

    @Value("${openapi.workflows-upsert-url:http://openapi-service:8020/workflows/upsert}")
    private String workflowsUpsertUrl;

    // type 까지삽입의
    private Map<String, Function<Object, Integer>> typeInsertMap = new HashMap<>();

    @PostConstruct
    public void initTypeInsertMap() {
        typeInsertMap.put("robot_design", (obj) -> robotDesignDao.insert((RobotDesign) obj));
        typeInsertMap.put("robot_execute", (obj) -> robotExecuteDao.insert((RobotExecute) obj));
        typeInsertMap.put("robot_version", (obj) -> robotVersionDao.insert((RobotVersion) obj));
        typeInsertMap.put("c_process", (obj) -> cProcessDao.insert((CProcess) obj));
        typeInsertMap.put("c_param", (obj) -> cParamDao.insert((CParam) obj));
        typeInsertMap.put("c_group", (obj) -> cGroupDao.insert((CGroup) obj));
        typeInsertMap.put("c_element", (obj) -> cElementDao.insert((CElement) obj));
    }

    @Override
    //    @Transactional(rollbackFor = Exception.class)
    public AppResponse<Boolean> insertUserSample(String userId, String tenantId) {
        // 1. 가져오기sample_templates테이블중version대의is_active = 1 의모든기록
        List<SampleTemplates> latestActiveTemplates = getLatestActiveTemplates();
        if (CollectionUtils.isEmpty(latestActiveTemplates)) {
            return AppResponse.success(true);
        }

        String version = latestActiveTemplates.get(0).getVersion();

        // 감지개버전의예시여부완료가져오기경과완료(사용자+테넌트)
        Integer count = sampleUsersDao.getExistSampleUsers(userId, tenantId, version);
        if (count != null && count > 0) {
            return AppResponse.success(true);
        }

        // 분방식,  userId+tenantId+version 재복사
        String lockKey = String.format("lock:sample_users:%s:%s:%s", userId, tenantId, version);
        String lockVal = UUID.randomUUID().toString();
        boolean locked = RedisUtils.tryLock(lockKey, lockVal, 30, TimeUnit.SECONDS);
        if (!locked) {
            log.warn("예시비고입력가져오기 실패, 가능완료있음발송요청 행중, userId={}, tenantId={}, version={}", userId, tenantId, version);
            return AppResponse.success(true);
        }

        // user_sample 테이블중삽입기록
        try {
            addUserSamples(latestActiveTemplates, userId, tenantId);
        } finally {
            RedisUtils.unlock(lockKey, lockVal);
        }

        return AppResponse.success(true);
    }

    /**
     * 요청 openapi서비스 - upsert 일아래매개변수 외부모듈호출
     * @param robotId
     * @param version
     * @param userId
     * @param tenantId
     * @throws JsonProcessingException
     */
    @Override
    @Async
    public void sendOpenApi(String robotId, Integer version, String userId, String tenantId)
            throws JsonProcessingException {

        log.info("봇발송버전의시요청 openapi upsert");
        QueryParamDto queryParamDto = new QueryParamDto();
        queryParamDto.setRobotId(robotId);
        queryParamDto.setMode(EXECUTOR);

        // 가져오기param
        log.info("start get param");
        AppResponse<List<ParamDto>> allParamResponse =
                executorModeHandler.getParamInside4NewVersion(queryParamDto, userId, tenantId, version);
        List<ParamDto> responseData = allParamResponse.getData();
        String parameters = JSON.toJSONString(responseData);
        log.info("새의봇매개변수예아래: " + parameters);

        WorkflowsUpsertDto requestDto = new WorkflowsUpsertDto();
        requestDto.setProject_id(robotId);
        requestDto.setVersion(version);
        requestDto.setParameters(parameters);

        // 를 requestDto 변환로 JSON 문자열
        String requestBody = JSONObject.toJSONString(requestDto);
        log.info("요청 openapi매개변수:" + requestBody);

        // 생성 RestTemplate 
        RestTemplate restTemplate = new RestTemplate();

        // 요청 
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.add("user_id", userId);

        // 생성요청 
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        // 발송 POST 요청 
        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(workflowsUpsertUrl, HttpMethod.POST, requestEntity, String.class);

            log.info(
                    "OpenAPI 요청완료, URL: {}, 상태: {}, : {}",
                    workflowsUpsertUrl,
                    response.getStatusCode(),
                    response.getBody());
        } catch (Exception e) {
            log.error("OpenAPI 요청 실패, URL: {}, 오류정보: {}", workflowsUpsertUrl, e.getMessage(), e);
            throw e;
        }
    }

    public void addUserSamples(List<SampleTemplates> latestActiveTemplates, String userId, String tenantId) {

        // 2. 결과합치기userId, 삽입다중행sample_users테이블기록
        List<SampleUsers> sampleUsersList = new ArrayList<>();
        Date now = new Date();

        // 일개sampleId 일개
        HashMap<String, List<SampleTemplates>> sampleTemplatesMap = getSampleTemplatesMap(latestActiveTemplates);
        for (Map.Entry<String, List<SampleTemplates>> entry : sampleTemplatesMap.entrySet()) {

            List<SampleTemplates> activeTemplates = entry.getValue();
            // id
            NewExampleDo newExampleDo = getNewExampleDo();

            for (SampleTemplates template : activeTemplates) {
                SampleUsers sampleUser = new SampleUsers();
                sampleUser.setCreatorId(userId);
                sampleUser.setTenantId(tenantId);
                sampleUser.setSampleId(template.getSampleId());
                sampleUser.setName(template.getName());
                sampleUser.setData(template.getData());
                sampleUser.setSource("system");
                sampleUser.setVersionInjected(template.getVersion());
                sampleUser.setCreatedTime(now);
                sampleUser.setUpdatedTime(now);

                sampleUsersList.add(sampleUser);

                // 3. 근거type, data중의json데이터사용fastJson변환성공의object, 후삽입까지의서비스테이블중
                processTemplateDataByType(template, userId, tenantId, newExampleDo);
            }
        }

        // 량삽입sample_users테이블
        if (!CollectionUtils.isEmpty(sampleUsersList)) {
            sampleUsersDao.insertBatch(sampleUsersList);
        }
    }

    private HashMap<String, List<SampleTemplates>> getSampleTemplatesMap(List<SampleTemplates> latestActiveTemplates) {
        Set<String> sampleIdSet = new HashSet<>();
        for (SampleTemplates sampleTemplates : latestActiveTemplates) {
            sampleIdSet.add(sampleTemplates.getSampleId());
        }

        HashMap<String, List<SampleTemplates>> ansMap = new HashMap<>();
        for (String sampleId : sampleIdSet) {
            List<SampleTemplates> sampleTemplatesList = latestActiveTemplates.stream()
                    .filter(template -> sampleId.equals(template.getSampleId()))
                    .collect(Collectors.toCollection(ArrayList::new));

            ansMap.put(sampleId, sampleTemplatesList);
        }

        return ansMap;
    }

    private NewExampleDo getNewExampleDo() {
        NewExampleDo newExampleDo = new NewExampleDo();
        newExampleDo.setElementId(String.valueOf(idWorker.nextId()));
        newExampleDo.setParamId(String.valueOf(idWorker.nextId()));
        newExampleDo.setGroupId(String.valueOf(idWorker.nextId()));
        newExampleDo.setProcessId(String.valueOf(idWorker.nextId()));
        newExampleDo.setRobotId(String.valueOf(idWorker.nextId()));
        return newExampleDo;
    }

    /**
     * 가져오기 새버전의
     */
    private List<SampleTemplates> getLatestActiveTemplates() {

        List<String> versionList = sampleTemplatesDao.getVersionList();
        if (CollectionUtils.isEmpty(versionList)) return Collections.EMPTY_LIST;
        String latestVersion = getLatestVersion(versionList);

        return sampleTemplatesDao.getSamples(latestVersion);
    }

    /**
     * 가져오기 새의버전
     * @param versionList
     * @return
     */
    private String getLatestVersion(List<String> versionList) {
        if (versionList == null || versionList.isEmpty()) {
            return null; // 또는출력예외, 근거서비스필요
        }
        String latest = versionList.get(0);

        for (int i = 1; i < versionList.size(); i++) {
            String current = versionList.get(i);
            if (compareVersions(current, latest) > 0) {
                latest = current;
            }
        }

        return latest;
    }

    // 개버전, 버전이면
    // 반환값: 정상데이터테이블일개 > 이개, 0 테이블대기, 데이터테이블일개 < 이개
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (num1 > num2) {
                return 1;
            } else if (num1 < num2) {
                return -1;
            }
        }

        return 0;
    }

    /**
     * 근거유형관리데이터
     * @param template
     * @param userId
     * @param tenantId
     * @param newExampleDo
     */
    public void processTemplateDataByType(
            SampleTemplates template, String userId, String tenantId, NewExampleDo newExampleDo) {
        if (template == null || StringUtils.isBlank(template.getType()) || StringUtils.isBlank(template.getData()))
            return;

        String businessType = template.getType();
        String dataJsonStr = template.getData();
        // 업데이트JSON중의creatorId updaterId tenantId processId robotId필드
        dataJsonStr = updateJsonFields(dataJsonStr, userId, tenantId, newExampleDo);

        Class<?> businessClass = ExampleConstants.TYPE_BUSINESS_CLASS_MAP.get(businessType);
        if (businessClass != null) {
            try {
                JSONArray jsonArray = JSONArray.parseArray(dataJsonStr);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    Object businessObject = jsonObject.toJavaObject(businessClass);

                    // 가져오기 의삽입데이터실행
                    Function<Object, Integer> insertFunction = typeInsertMap.get(businessType);
                    if (insertFunction != null) {
                        // param_id관리
                        if (businessType.equals("c_param")) {
                            CParam param = (CParam) businessObject;
                            param.setId(String.valueOf(idWorker.nextId()));
                            businessObject = param;
                        }

                        insertFunction.apply(businessObject);
                        log.info("성공삽입서비스데이터, 유형: {}", businessType);

                        // 요청 openapi연결
                        if (businessType.equals("robot_execute"))
                            sendOpenApiRequest((RobotExecute) businessObject, userId, tenantId);
                    } else {
                        log.warn("찾을 수 없는 의삽입방법법, 유형: {}", businessType);
                    }
                }
            } catch (Exception e) {
                log.error("관리데이터실패, 유형: {}, 오류정보: {}", template.getType(), e.getMessage(), e);
            }
        }
    }

    /**
     * openapi전송요청 
     *
     * @param robotExecute
     * @param userId
     * @throws NoLoginException
     * @throws JsonProcessingException
     */
    private void sendOpenApiRequest(RobotExecute robotExecute, String userId, String tenantId)
            throws JsonProcessingException {
        log.info("send request to openapi start ... ");
        QueryParamDto queryParamDto = new QueryParamDto();
        queryParamDto.setRobotId(robotExecute.getRobotId());
        queryParamDto.setMode(EXECUTOR);

        // 가져오기expo-user의robot_id
        String expoUserRobotId = robotExecuteDao.getExpoUserRobotId(robotExecute.getName(), expoUserId);

        // 가져오기param
        log.info("start get param");
        AppResponse<List<ParamDto>> allParamResponse =
                executorModeHandler.getParamInside(queryParamDto, userId, tenantId);
        List<ParamDto> responseData = allParamResponse.getData();
        String parameters = JSON.toJSONString(responseData);
        log.info("robot params are as follows:" + parameters);

        WorkflowsUpsertDto requestDto = new WorkflowsUpsertDto();
        requestDto.setProject_id(robotExecute.getRobotId());
        requestDto.setName(robotExecute.getName());
        requestDto.setEnglish_name(robotExecute.getName());
        requestDto.setDescription("");
        requestDto.setVersion(robotExecute.getRobotVersion());
        requestDto.setStatus(1);
        requestDto.setParameters(parameters);
        requestDto.setExample_project_id(expoUserRobotId);

        // 를 requestDto 변환로 JSON 문자열
        String requestBody = JSONObject.toJSONString(requestDto);
        log.info("요청 openapi매개변수:" + requestBody);

        // 생성 RestTemplate 
        RestTemplate restTemplate = new RestTemplate();

        // 요청 
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.add("user_id", userId);

        // 생성요청 
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        // 발송 POST 요청 
        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(workflowsUpsertUrl, HttpMethod.POST, requestEntity, String.class);

            log.info(
                    "OpenAPI 요청완료, URL: {}, 상태: {}, : {}",
                    workflowsUpsertUrl,
                    response.getStatusCode(),
                    response.getBody());
        } catch (Exception e) {
            log.error("OpenAPI 요청 실패, URL: {}, 오류정보: {}", workflowsUpsertUrl, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 업데이트JSON문자열중의creatorId, updaterId및tenantId필드
     * @param jsonStr JSON문자열
     * @param userId 사용자ID
     * @param tenantId 테넌트ID
     * @return 업데이트후의JSON문자열
     */
    private String updateJsonFields(String jsonStr, String userId, String tenantId, NewExampleDo newExampleDo) {
        if (StringUtils.isBlank(jsonStr)) {
            return jsonStr;
        }

        JSONArray jsonArray = JSONObject.parseArray(jsonStr);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            if (jsonObject != null) {
                jsonObject.put("id", null);
                jsonObject.put("creator_id", userId);
                jsonObject.put("updater_id", userId);
                jsonObject.put("tenant_id", tenantId);
                jsonObject.put("robot_id", newExampleDo.getRobotId());
                jsonObject.put("process_id", newExampleDo.getProcessId());
                //                jsonObject.put("element_id", idWorker.nextId());
                jsonObject.put("group_id", newExampleDo.getGroupId());
            }
        }

        return jsonArray.toJSONString();
    }
}
