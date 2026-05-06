package com.iflytek.rpa.astronAgent.service.impl;

import static com.iflytek.rpa.robot.constants.RobotConstant.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.rpa.astronAgent.dao.ShoprpaAgentDao;
import com.iflytek.rpa.astronAgent.entity.dto.CopyRobotDto;
import com.iflytek.rpa.astronAgent.entity.dto.CopyRobotResponseDto;
import com.iflytek.rpa.astronAgent.entity.dto.GetUserIdDto;
import com.iflytek.rpa.astronAgent.entity.dto.GetUserIdResponseDto;
import com.iflytek.rpa.astronAgent.service.ShoprpaAgentService;
import com.iflytek.rpa.base.dao.CParamDao;
import com.iflytek.rpa.base.entity.CParam;
import com.iflytek.rpa.base.entity.dto.ParamDto;
import com.iflytek.rpa.component.dao.ComponentRobotUseDao;
import com.iflytek.rpa.component.entity.ComponentRobotUse;
import com.iflytek.rpa.conf.dao.UapUserDao;
import com.iflytek.rpa.market.dao.AppMarketResourceDao;
import com.iflytek.rpa.market.entity.AppMarketResource;
import com.iflytek.rpa.market.entity.MarketDto;
import com.iflytek.rpa.robot.dao.RobotExecuteDao;
import com.iflytek.rpa.robot.dao.RobotVersionDao;
import com.iflytek.rpa.robot.entity.RobotExecute;
import com.iflytek.rpa.robot.entity.RobotVersion;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * ShoprpaAgent서비스유형
 */
@Service
@RequiredArgsConstructor
public class ShoprpaAgentServiceImpl implements ShoprpaAgentService {

    @Autowired
    private UapUserDao uapUserDao;

    @Autowired
    private ShoprpaAgentDao astronAgentDao;

    @Autowired
    private RobotVersionDao robotVersionDao;

    @Autowired
    private RobotExecuteDao robotExecuteDao;

    @Autowired
    private AppMarketResourceDao appMarketResourceDao;

    @Autowired
    private ComponentRobotUseDao componentUseDao;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private CParamDao cParamDao;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${uap.database.name:uap_db}")
    private String databaseName;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<CopyRobotResponseDto> copyRobot(CopyRobotDto copyRobotDto) {
        String robotId = copyRobotDto.getRobotId();
        Integer version = copyRobotDto.getVersion();
        String targetPhone = copyRobotDto.getTargetPhone();

        // 1. 근거휴대폰 번호가져오기사용자ID
        String userId = uapUserDao.getUserIdByLoginNameOrPhone(databaseName, targetPhone, targetPhone);
        if (StringUtils.isBlank(userId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "대상 사용자를 찾을 수 없습니다");
        }

        // 2. 봇여부저장에서(에서robot_execute테이블조회, 추가위deleted=0파일)
        RobotExecute sourceRobot = robotExecuteDao.selectOne(new LambdaQueryWrapper<RobotExecute>()
                .eq(RobotExecute::getRobotId, robotId)
                .eq(RobotExecute::getDeleted, 0)
                .last("LIMIT 1"));
        if (sourceRobot == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "봇을 찾을 수 없습니다");
        }

        // 3. 봇의creator_id여부예목록사용자id, 결과가예이면아니요필요복사, 직선연결반환robotId
        if (userId.equals(sourceRobot.getCreatorId())) {
            CopyRobotResponseDto responseDto = buildCopyRobotResponse(robotId, version, sourceRobot.getName());
            return AppResponse.success(responseDto);
        }

        // 4. 근거사용자ID가져오기테넌트ID목록
        List<String> tenantIds = astronAgentDao.getTenantIdsByUserId(databaseName, userId);
        if (CollectionUtils.isEmpty(tenantIds)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "대상 사용자의 테넌트를 찾을 수 없습니다");
        }

        // 5. 가져오기개사람테넌트ID
        String tenantId = astronAgentDao.getPersonalTenantId(databaseName, tenantIds);
        if (StringUtils.isBlank(tenantId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "대상 사용자의 개인 테넌트를 찾을 수 없습니다");
        }

        // 6. 근거dataSource버전정보 , 까지봇
        RobotVersion robotVersion = null;
        String dataSource = sourceRobot.getDataSource();
        String rootRobotId = robotId; // 봇ID예robotId
        String rootRobotName = null; // 봇이름, 사용후복사

        if (CREATE.equals(dataSource)) {
            // create유형: robotId예봇ID
            rootRobotId = robotId;
            robotVersion = robotVersionDao.getVersion(robotId, version);
            if (null == robotVersion) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM, "해당 버전의 봇을 찾을 수 없습니다");
            }
        } else if (MARKET.equals(dataSource)) {
            // market유형: 필요통신경과appId및marketId까지봇
            String appId = sourceRobot.getAppId();
            String marketId = sourceRobot.getMarketId();
            if (StringUtils.isBlank(appId) || StringUtils.isBlank(marketId)) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM, "마켓 봇 정보가 올바르지 않습니다");
            }
            // 조회app_market_resource가져오기 봇ID
            MarketDto marketDto = new MarketDto();
            marketDto.setAppId(appId);
            marketDto.setMarketId(marketId);
            AppMarketResource appResource = appMarketResourceDao.getAppInfoByAppId(marketDto);
            if (appResource == null || StringUtils.isBlank(appResource.getRobotId())) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM, "봇 정보를 가져올 수 없습니다");
            }
            rootRobotId = appResource.getRobotId();
            // 사용봇ID및지정version조회버전정보
            robotVersion = robotVersionDao.getVersion(rootRobotId, version);
            if (null == robotVersion) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM, "해당 버전의 봇을 찾을 수 없습니다");
            }
        } else if (DEPLOY.equals(dataSource)) {
            // deploy유형: appId예봇ID
            String appId = sourceRobot.getAppId();
            if (StringUtils.isBlank(appId)) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM, "모듈 봇 정보가 올바르지 않습니다");
            }
            rootRobotId = appId;
            // 사용봇ID및지정version조회버전정보
            robotVersion = robotVersionDao.getVersion(rootRobotId, version);
            if (null == robotVersion) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM, "해당 버전의 봇을 찾을 수 없습니다");
            }
        } else {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "지원하지 않는 데이터 유형입니다");
        }

        // 가져오기 봇이름(에서robot_execute테이블조회봇)
        RobotExecute rootRobot = robotExecuteDao.selectOne(new LambdaQueryWrapper<RobotExecute>()
                .eq(RobotExecute::getRobotId, rootRobotId)
                .eq(RobotExecute::getDeleted, 0)
                .last("LIMIT 1"));
        if (rootRobot != null && StringUtils.isNotBlank(rootRobot.getName())) {
            rootRobotName = rootRobot.getName();
        }

        // 7. 생성봇이름(관리재이름, 추가순서후)
        String baseRobotName;
        if (StringUtils.isNotBlank(rootRobotName)) {
            baseRobotName = rootRobotName + "-지능형 에이전트복사봇";
        } else {
            baseRobotName = "지능형 에이전트복사봇";
        }
        String robotName = generateUniqueRobotName(baseRobotName, userId, tenantId, null);

        // 8. 조회해당봇여부완료복사경과(통신경과dataSource로deployappId로rootRobotId, 추가위deleted=0파일)
        RobotExecute existingRobot = robotExecuteDao.selectOne(new LambdaQueryWrapper<RobotExecute>()
                .eq(RobotExecute::getAppId, rootRobotId)
                .eq(RobotExecute::getCreatorId, userId)
                .eq(RobotExecute::getTenantId, tenantId)
                .eq(RobotExecute::getDataSource, DEPLOY)
                .eq(RobotExecute::getDeleted, 0)
                .last("LIMIT 1"));

        String newRobotId;
        if (existingRobot != null) {
            // 결과가버전아니요, 업데이트
            if (!version.equals(existingRobot.getAppVersion())) {
                // 업데이트시다시 완료일이름(정렬제거현재봇본)
                robotName = generateUniqueRobotName(baseRobotName, userId, tenantId, existingRobot.getRobotId());

                existingRobot.setName(robotName);
                existingRobot.setAppVersion(version);
                existingRobot.setUpdateTime(new Date());
                existingRobot.setUpdaterId(userId);
                existingRobot.setResourceStatus(null);
                existingRobot.setDataSource(DEPLOY);
                existingRobot.setAppId(rootRobotId);
                existingRobot.setMarketId(null);
                robotExecuteDao.updateById(existingRobot);
                newRobotId = existingRobot.getRobotId();
            } else {
                // 버전, 직선연결반환
                newRobotId = existingRobot.getRobotId();
            }
        } else {
            // 삽입새기록
            newRobotId = idWorker.nextId() + "";
            RobotExecute robotExecute = new RobotExecute();
            robotExecute.setRobotId(newRobotId);
            robotExecute.setName(robotName);
            robotExecute.setCreatorId(userId);
            robotExecute.setUpdaterId(userId);
            robotExecute.setTenantId(tenantId);
            robotExecute.setAppId(rootRobotId);
            robotExecute.setAppVersion(version);
            robotExecute.setMarketId(null);
            robotExecute.setResourceStatus(null);
            robotExecute.setDataSource(DEPLOY);
            robotExecute.setUpdateTime(new Date());
            robotExecuteDao.insertObtainedRobot(robotExecute);

            // 컴포넌트의사용필요삽입일아래(봇있음버전테이블기록, 필요복사컴포넌트사용)
            addCompUseList(newRobotId, version, robotVersion, tenantId, userId);
        }

        CopyRobotResponseDto responseDto = buildCopyRobotResponse(newRobotId, version, robotName);

        return AppResponse.success(responseDto);
    }

    /**
     * 생성복사봇DTO
     * @param robotId 봇ID
     * @param version 버전
     * @return CopyRobotResponseDto
     */
    private CopyRobotResponseDto buildCopyRobotResponse(String robotId, Integer version, String robotName) {
        CopyRobotResponseDto responseDto = new CopyRobotResponseDto();
        responseDto.setRobotId(robotId);
        responseDto.setName(robotName);
        responseDto.setEnglishName("");
        responseDto.setDescription("");
        responseDto.setVersion(version != null ? version.toString() : "1.0.0");
        responseDto.setStatus(1);

        // 호출getAllParams가져오기매개변수(필요하지 않습니다인증)
        try {
            List<ParamDto> params = getAllParamsWithoutAuth(robotId, null, null);
            responseDto.setParameters(params);
        } catch (Exception e) {
            // 결과가가져오기매개변수실패, 비어 있습니다목록
            responseDto.setParameters(new ArrayList<>());
        }

        return responseDto;
    }

    /**
     * 완료일의봇이름(결과가재이름이면추가순서후)
     * @param baseName 이름
     * @param userId 사용자ID
     * @param tenantId 테넌트ID
     * @param excludeRobotId 정렬제거의봇ID(업데이트시사용, 정렬제거현재봇본)
     * @return 일의봇이름
     */
    private String generateUniqueRobotName(String baseName, String userId, String tenantId, String excludeRobotId) {
        // 일조회모든이름의봇(지원이름및숫자후의이름)
        LambdaQueryWrapper<RobotExecute> queryWrapper = new LambdaQueryWrapper<RobotExecute>()
                .eq(RobotExecute::getCreatorId, userId)
                .eq(RobotExecute::getTenantId, tenantId)
                .eq(RobotExecute::getDeleted, 0)
                .and(wrapper -> {
                    // 매칭이름또는이름+숫자후의형식
                    wrapper.eq(RobotExecute::getName, baseName).or().likeRight(RobotExecute::getName, baseName);
                });

        // 결과가예지정필요정렬제거의봇ID(업데이트), 이면정렬제거
        if (StringUtils.isNotBlank(excludeRobotId)) {
            queryWrapper.ne(RobotExecute::getRobotId, excludeRobotId);
        }

        List<RobotExecute> existingRobots = robotExecuteDao.selectList(queryWrapper);

        // 결과가이름찾을 수 없습니다, 직선연결반환
        boolean baseNameExists = existingRobots.stream().anyMatch(robot -> baseName.equals(robot.getName()));
        if (!baseNameExists) {
            return baseName;
        }

        // 출력모든숫자후의이름, 가져오기 대순서
        int maxSuffix = 0;
        for (RobotExecute robot : existingRobots) {
            String name = robot.getName();
            if (name.startsWith(baseName) && name.length() > baseName.length()) {
                // 조회후여부로숫자
                String suffixStr = name.substring(baseName.length());
                try {
                    int suffix = Integer.parseInt(suffixStr);
                    if (suffix > maxSuffix) {
                        maxSuffix = suffix;
                    }
                } catch (NumberFormatException e) {
                    // 형식아니요정상의이름(아니요예숫자후)
                }
            }
        }

        // 완료새의일이름(직선연결에서이름문자후숫자)
        return baseName + (maxSuffix + 1);
    }

    /**
     * 추가컴포넌트사용목록
     */
    private void addCompUseList(
            String newRobotId,
            Integer newRobotVersion,
            RobotVersion authorRobotVersion,
            String tenantId,
            String userId) {
        String authorRobotId = authorRobotVersion.getRobotId();
        Integer authorVersion = authorRobotVersion.getVersion();
        List<ComponentRobotUse> compUseListAuth =
                componentUseDao.getByRobotIdAndVersion(authorRobotId, authorVersion, tenantId);

        List<ComponentRobotUse> newCompUseList = new ArrayList<>();

        for (ComponentRobotUse compRobotUse : compUseListAuth) {
            ComponentRobotUse newCompUse = new ComponentRobotUse();
            BeanUtils.copyProperties(compRobotUse, newCompUse);
            newCompUse.setRobotId(newRobotId);
            newCompUse.setRobotVersion(newRobotVersion);
            newCompUse.setCreatorId(userId);
            newCompUse.setUpdaterId(userId);
            newCompUse.setTenantId(tenantId);
            newCompUse.setCreateTime(new Date());
            newCompUse.setUpdateTime(new Date());

            newCompUseList.add(newCompUse);
        }
        if (!newCompUseList.isEmpty()) {
            componentUseDao.insertBatch(newCompUseList);
        }
    }

    @Override
    public AppResponse<GetUserIdResponseDto> getUserIdByPhone(GetUserIdDto getUserIdDto) {
        String phone = getUserIdDto.getPhone();

        // 근거휴대폰 번호가져오기사용자ID
        String userId = uapUserDao.getUserIdByLoginNameOrPhone(databaseName, phone, phone);
        if (StringUtils.isBlank(userId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "사용자를 찾을 수 없습니다");
        }

        GetUserIdResponseDto responseDto = new GetUserIdResponseDto();
        responseDto.setUserId(userId);

        return AppResponse.success(responseDto);
    }

    /**
     * 필요하지 않습니다인증가져오기봇매개변수(복사ExecutorModeHandler, 수정로아니요필요인증)
     * @param robotId 봇ID
     * @param processId 프로세스ID(가능선택)
     * @param moduleId 모듈ID(가능선택)
     * @return 매개변수목록
     */
    private List<ParamDto> getAllParamsWithoutAuth(String robotId, String processId, String moduleId)
            throws JsonProcessingException {
        // 사용아니요필요인증의방법법가져오기봇정보
        RobotExecute executeInfo = robotExecuteDao.getRobotExecuteByRobotId(robotId);
        if (executeInfo == null) {
            throw new ServiceException(ErrorCodeEnum.E_SQL.getCode(), "실행할 로봇 정보를 찾을 수 없습니다");
        }

        return handleDataSource(executeInfo, processId, moduleId, null);
    }

    /**
     * 관리아니요데이터의매개변수가져오기
     */
    private List<ParamDto> handleDataSource(
            RobotExecute executeInfo, String processId, String moduleId, Integer robotVersion)
            throws JsonProcessingException {
        if (robotVersion != null) {
            executeInfo.setAppVersion(robotVersion);
            executeInfo.setRobotVersion(robotVersion);
        }
        if (CREATE.equals(executeInfo.getDataSource())) {
            return handleCreateSource(executeInfo, processId, moduleId);
        } else if (MARKET.equals(executeInfo.getDataSource())) {
            return handleMarketSource(executeInfo, processId, moduleId);
        } else if (DEPLOY.equals(executeInfo.getDataSource())) {
            return handleDeploySource(executeInfo, processId, moduleId);
        }

        throw new ServiceException(ErrorCodeEnum.E_PARAM.getCode(), "지원하지 않는 데이터 출처입니다");
    }

    /**
     * 관리모듈의매개변수
     */
    private List<ParamDto> handleDeploySource(RobotExecute executeInfo, String processId, String moduleId) {
        String originRobotId = cParamDao.getDeployOriginalRobotId(executeInfo);

        // python모듈
        if (!StringUtils.isEmpty(moduleId)) {
            return deployModuleHandle(executeInfo, moduleId, originRobotId);
        }

        return deployProcessHandle(executeInfo, processId, originRobotId);
    }

    /**
     * 관리모듈의모듈매개변수
     */
    private List<ParamDto> deployModuleHandle(RobotExecute executeInfo, String moduleId, String originRobotId) {
        List<CParam> params = cParamDao.getParamsByModuleId(moduleId, originRobotId, executeInfo.getAppVersion());
        return convertParams(params);
    }

    /**
     * 관리모듈의프로세스매개변수
     */
    private List<ParamDto> deployProcessHandle(RobotExecute executeInfo, String processId, String originRobotId) {
        if (StringUtils.isBlank(processId)) {
            processId = cParamDao.getMianProcessId(originRobotId, executeInfo.getAppVersion());
        }
        List<CParam> params = cParamDao.getAllParams(processId, originRobotId, executeInfo.getAppVersion());
        return convertParams(params);
    }

    /**
     * 관리마켓의매개변수
     */
    private List<ParamDto> handleMarketSource(RobotExecute executeInfo, String processId, String moduleId) {
        validateMarketInfo(executeInfo);
        String originRobotId = cParamDao.getMarketRobotId(executeInfo);
        // python모듈
        if (!StringUtils.isEmpty(moduleId)) {
            return marketModuleHandle(executeInfo, moduleId, originRobotId);
        }
        // 프로세스
        return marketProcessHandle(executeInfo, processId, originRobotId);
    }

    /**
     * 관리마켓의모듈매개변수
     */
    private List<ParamDto> marketModuleHandle(RobotExecute executeInfo, String moduleId, String originRobotId) {
        List<CParam> params = cParamDao.getParamsByModuleId(moduleId, originRobotId, executeInfo.getAppVersion());
        return convertParams(params);
    }

    /**
     * 관리마켓의프로세스매개변수
     */
    private List<ParamDto> marketProcessHandle(RobotExecute executeInfo, String processId, String originRobotId) {
        if (StringUtils.isBlank(processId)) {
            processId = cParamDao.getMianProcessId(originRobotId, executeInfo.getAppVersion());
        }
        List<CParam> params = cParamDao.getAllParams(processId, originRobotId, executeInfo.getAppVersion());
        return convertParams(params);
    }

    /**
     * 관리생성의매개변수
     */
    private List<ParamDto> handleCreateSource(RobotExecute executeInfo, String processId, String moduleId)
            throws JsonProcessingException {
        Integer enabledVersion = cParamDao.getRobotVersion(executeInfo.getRobotId());
        if (executeInfo.getRobotVersion() != null) {
            enabledVersion = executeInfo.getRobotVersion();
        }
        // python모듈
        if (!StringUtils.isEmpty(moduleId)) {
            return createModuleHandle(executeInfo, moduleId, enabledVersion);
        }
        // 프로세스
        return createProcessHandle(executeInfo, processId, enabledVersion);
    }

    /**
     * 관리생성의모듈매개변수
     */
    private List<ParamDto> createModuleHandle(RobotExecute executeInfo, String moduleId, Integer enabledVersion) {
        List<CParam> params = cParamDao.getSelfRobotParamByModuleId(executeInfo.getRobotId(), moduleId, enabledVersion);
        return convertParams(params);
    }

    /**
     * 관리생성의프로세스매개변수
     */
    private List<ParamDto> createProcessHandle(RobotExecute executeInfo, String processId, Integer enabledVersion)
            throws JsonProcessingException {
        String mainProcessId = cParamDao.getMianProcessId(executeInfo.getRobotId(), enabledVersion);
        if (mainProcessId.equals(processId)) {
            if (executeInfo.getParamDetail() != null) {
                return parseCustomParams(executeInfo.getParamDetail());
            }
        } else {
            processId = mainProcessId;
        }
        List<CParam> params = cParamDao.getSelfRobotParam(executeInfo.getRobotId(), processId, enabledVersion);
        return convertParams(params);
    }

    /**
     * 인증마켓정보
     */
    private void validateMarketInfo(RobotExecute executeInfo) {
        if (StringUtils.isAnyBlank(executeInfo.getMarketId(), executeInfo.getAppId())
                || executeInfo.getAppVersion() == null) {
            throw new ServiceException(ErrorCodeEnum.E_SQL.getCode(), "로봇 마켓 정보가 올바르지 않습니다");
        }
    }

    /**
     * 파싱지정매개변수
     */
    private List<ParamDto> parseCustomParams(String paramDetail) throws JsonProcessingException {
        List<CParam> params = objectMapper.readValue(paramDetail, new TypeReference<List<CParam>>() {});
        return convertParams(params);
    }

    /**
     * 변환매개변수 로DTO
     */
    private List<ParamDto> convertParams(List<CParam> params) {
        if (CollectionUtils.isEmpty(params)) {
            return Collections.emptyList();
        }
        return params.stream()
                .map(p -> {
                    ParamDto dto = new ParamDto();
                    BeanUtils.copyProperties(p, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
