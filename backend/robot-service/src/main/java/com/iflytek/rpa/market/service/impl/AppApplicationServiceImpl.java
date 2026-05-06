package com.iflytek.rpa.market.service.impl;

import static com.iflytek.rpa.market.constants.AuditConstant.*;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.market.dao.AppApplicationDao;
import com.iflytek.rpa.market.dao.AppApplicationTenantDao;
import com.iflytek.rpa.market.dao.AppMarketResourceDao;
import com.iflytek.rpa.market.dao.AppMarketUserDao;
import com.iflytek.rpa.market.dao.AppMarketVersionDao;
import com.iflytek.rpa.market.entity.*;
import com.iflytek.rpa.market.entity.bo.PublishInfoBo;
import com.iflytek.rpa.market.entity.dto.*;
import com.iflytek.rpa.market.entity.vo.LatestVersionRobotVo;
import com.iflytek.rpa.market.entity.vo.MarketInfoVo;
import com.iflytek.rpa.market.entity.vo.MyApplicationPageListVo;
import com.iflytek.rpa.market.service.AppApplicationService;
import com.iflytek.rpa.market.service.AppMarketResourceService;
import com.iflytek.rpa.notify.entity.dto.ApplicationNotifyDto;
import com.iflytek.rpa.notify.service.impl.NotifySendServiceImpl;
import com.iflytek.rpa.robot.dao.RobotDesignDao;
import com.iflytek.rpa.robot.entity.RobotDesign;
import com.iflytek.rpa.robot.entity.RobotExecute;
import com.iflytek.rpa.robot.entity.vo.ExecuteListVo;
import com.iflytek.rpa.robot.service.RobotVersionService;
import com.iflytek.rpa.utils.DateUtils;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * @author mjren
 * @date 2025-07-02 11:00
 * @copyright Copyright (c) 2025 mjren
 */
@Slf4j
@Service("appApplicationService")
public class AppApplicationServiceImpl extends ServiceImpl<AppApplicationDao, AppApplication>
        implements AppApplicationService {

    @Autowired
    private AppApplicationDao appApplicationDao;

    @Resource
    private NotifySendServiceImpl notifySendService;

    @Autowired
    private AppApplicationTenantDao appApplicationTenantDao;

    @Autowired
    private AppMarketResourceDao appMarketResourceDao;

    @Autowired
    private AppMarketVersionDao appMarketVersionDao;

    @Autowired
    private AppMarketUserDao appMarketUserDao;

    @Autowired
    private RobotDesignDao robotDesignDao;

    @Autowired
    private AppMarketResourceService appMarketResourceService;

    @Autowired
    private RobotVersionService robotVersionService;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    private void releaseHandle(AuditApplicationDto auditApplicationDto, AppApplication awaitingUpdate)
            throws Exception {
        // 가져오기위신청정보
        AppApplication application = this.getById(auditApplicationDto.getId());
        if (AUDIT_STATUS_APPROVED.equals(auditApplicationDto.getStatus())) {
            if (application != null) {
                if (StringUtils.isBlank(application.getPublishInfo())
                        && StringUtils.isNotBlank(application.getMarketInfo())) {
                    // 공유
                    shareHandle(awaitingUpdate, application);
                }
                if (StringUtils.isBlank(application.getMarketInfo())
                        && StringUtils.isNotBlank(application.getPublishInfo())) {
                    // 발송버전
                    publishHandle(awaitingUpdate, application);
                }
                // 일발송버전
                if (StringUtils.isNotBlank(application.getMarketInfo())
                        && StringUtils.isNotBlank(application.getPublishInfo())) {
                    publishHandle(awaitingUpdate, application);
                    shareHandle(awaitingUpdate, application);
                }
            }
        }
        // 메시지알림
        sendNotify(awaitingUpdate, application);
    }

    private void publishHandle(AppApplication awaitingUpdate, AppApplication application) throws Exception {
        PublishInfoBo publishInfoBo = parsePublishInfoBoFromJson(application.getPublishInfo());
        RobotExecute robotExecute = publishInfoBo.getRobotExecute();
        Integer nextVersion = publishInfoBo.getNextVersion();
        // 완료 앱 마켓 의 버전 
        robotVersionService.updateAppAndRobot(robotExecute, nextVersion);
    }

    private void shareHandle(AppApplication awaitingUpdate, AppApplication application) {
        // 파싱마켓정보
        MarketInfoDto marketInfo = parseMarketInfoFromJson(application.getMarketInfo());
        if (marketInfo != null && !CollectionUtils.isEmpty(marketInfo.getMarketIdList())) {
            // 생성공유매개변수
            ShareRobotDto marketResourceDto = new ShareRobotDto();
            marketResourceDto.setRobotId(application.getRobotId());
            marketResourceDto.setMarketIdList(marketInfo.getMarketIdList());
            marketResourceDto.setEditFlag(marketInfo.getEditFlag());
            marketResourceDto.setCategory(marketInfo.getCategory());

            // 가져오기봇정보
            RobotDesign robotDesign = robotDesignDao.getRobotRegardlessLogicDel(
                    application.getRobotId(), application.getCreatorId(), application.getTenantId());
            if (robotDesign == null) {
                throw new ServiceException("불가가져오기봇정보");
            }
            marketResourceDto.setAppName(robotDesign.getName());
            // 실행공유
            AppResponse<?> shareResponse = appMarketResourceService.executeShareRobotLogic(
                    marketResourceDto, application.getCreatorId(), application.getTenantId());
            if (!shareResponse.ok()) {
                throw new ServiceException(shareResponse.getMessage());
            }
            if (!marketInfo.getMarketIdList().isEmpty()) {
                awaitingUpdate.setMarketId(marketInfo.getMarketIdList().get(0));
            }
        }
    }

    /**
     * 전송알림
     *
     * @param application
     */
    private void sendNotify(AppApplication appApplication, AppApplication application) {
        ApplicationNotifyDto applicationNotifyDto = new ApplicationNotifyDto();
        BeanUtil.copyProperties(appApplication, applicationNotifyDto);
        applicationNotifyDto.setUserId(application.getCreatorId());
        applicationNotifyDto.setTenantId(application.getTenantId());
        applicationNotifyDto.setMarketId(appApplication.getMarketId());
        notifySendService.createNotify4Application(applicationNotifyDto);
    }

    @Override
    public AppResponse<String> getAuditStatus() throws NoLoginException {
        // 1. 가져오기현재테넌트ID
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        if (StringUtils.isBlank(tenantId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "테넌트 ID는 비워 둘 수 없습니다");
        }

        try {
            // 2. 조회현재검토열기닫기상태
            AppApplicationTenant currentConfig = appApplicationTenantDao.getByTenantId(tenantId);

            // 3. 결과가매칭찾을 수 없습니다, 반환사용 안 함상태
            if (currentConfig == null) {
                return AppResponse.success(AUDIT_ENABLE_STATUS_OFF);
            }

            // 4. 근거데이터베이스중의상태반환의문자열
            String status = AUDIT_ENABLE_ON.equals(currentConfig.getAuditEnable())
                    ? AUDIT_ENABLE_STATUS_ON
                    : AUDIT_ENABLE_STATUS_OFF;

            return AppResponse.success(status);

        } catch (Exception e) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EXCEPTION, "조회검토열기닫기상태예외: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<Integer> preReleaseCheck(PreReleaseCheckDto dto) throws Exception {
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        // 1. 테넌트여부열기시작위검토?미완료열기시작직선연결반환0
        AppApplicationTenant auditConfig = appApplicationTenantDao.getByTenantId(tenantId);
        if (auditConfig == null || AUDIT_ENABLE_OFF.equals(auditConfig.getAuditEnable())) {
            return AppResponse.success(0);
        }
        // 2. 테넌트열기시작위검토;조회여부있음삭제되지 않음통신경과의위검토?
        AppApplication approvedApplication = this.getOne(new LambdaQueryWrapper<AppApplication>()
                .eq(AppApplication::getRobotId, dto.getRobotId())
                .eq(AppApplication::getApplicationType, "release")
                .eq(AppApplication::getStatus, AUDIT_STATUS_APPROVED)
                .eq(AppApplication::getDeleted, 0)
                .eq(AppApplication::getCloudDeleted, 0)
                .orderByDesc(AppApplication::getRobotVersion)
                .last("LIMIT 1"));
        if (approvedApplication != null) {
            // 결과가예에서버전의, 직선연결반환0
            if (Objects.equals(dto.getVersion(), approvedApplication.getRobotVersion())) {
                return AppResponse.success(0);
            }
            // 3. 결과가예전버전의: 비밀단계로색상선택통신경과이면직선연결반환0, 아니요이면반환1
            String securityLevel = approvedApplication.getSecurityLevel();
            Integer defaultPass = approvedApplication.getDefaultPass();
            if ("green".equals(securityLevel) && Integer.valueOf(1).equals(defaultPass)) {
                return AppResponse.success(0);
            } else {
                return AppResponse.success(1);
            }
        }
        // 4. 결과가있음위검토기록, 이면반환1
        return AppResponse.success(1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<String> submitReleaseApplication(ReleaseApplicationDto applicationDto) throws Exception {
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

        AppResponse<String> greenPass = greenPassHandle(applicationDto, userId, tenantId);
        if (greenPass != null) return greenPass;

        AppResponse<String> E_SERVICE = beforeSubmitCheck(applicationDto, userId);
        if (E_SERVICE != null) return E_SERVICE;

        // 생성위검토신청
        AppApplication application = new AppApplication();
        application.setRobotId(applicationDto.getRobotId());
        application.setRobotVersion(applicationDto.getRobotVersion());
        application.setApplicationType("release");
        application.setStatus("pending");
        application.setCreatorId(userId);
        application.setTenantId(tenantId);
        application.setCreateTime(new Date());
        application.setUpdateTime(new Date());
        application.setDeleted(0);
        // 저장마켓정보
        String marketInfoJson = convertMarketInfoToJson(
                applicationDto.getMarketIdList(), applicationDto.getEditFlag(), applicationDto.getCategory());
        application.setMarketInfo(marketInfoJson);

        appApplicationDao.insert(application);

        return AppResponse.success("위신청제출성공, 요청대기검토");
    }

    private AppResponse<String> greenPassHandle(ReleaseApplicationDto applicationDto, String userId, String tenantId)
            throws Exception {
        // 조회 위일개신청단일 -> 비밀단계로색상선택통신경과의 완료의신청단일
        AppApplication greenPass = appApplicationDao.selectOne(new LambdaQueryWrapper<AppApplication>()
                .eq(AppApplication::getRobotId, applicationDto.getRobotId())
                .eq(
                        AppApplication::getRobotVersion,
                        applicationDto.getRobotVersion() > 1 ? applicationDto.getRobotVersion() - 1 : 1)
                .eq(AppApplication::getCreatorId, userId)
                .eq(AppApplication::getApplicationType, "release")
                .eq(AppApplication::getStatus, AUDIT_STATUS_APPROVED)
                .eq(AppApplication::getDeleted, 0)
                .eq(AppApplication::getSecurityLevel, "green")
                .eq(AppApplication::getDefaultPass, 1));
        if (greenPass != null) {
            AppApplication application = createGreenPassApplication(applicationDto, userId, tenantId);
            appApplicationDao.insert(application);
            AuditApplicationDto auditApplicationDto = new AuditApplicationDto();
            BeanUtil.copyProperties(application, auditApplicationDto);
            releaseHandle(auditApplicationDto, application);
            return AppResponse.success("현재봇통신경과위검토, 요청 앱 마켓조회업데이트");
        }
        return null;
    }

    private AppApplication createGreenPassApplication(
            ReleaseApplicationDto applicationDto, String userId, String tenantId) throws JsonProcessingException {
        AppApplication application = new AppApplication();
        application.setDeleted(0);
        String robotId = applicationDto.getRobotId();
        application.setRobotId(robotId);
        application.setRobotVersion(applicationDto.getRobotVersion());

        application.setApplicationType("release");
        application.setStatus(AUDIT_STATUS_APPROVED);
        application.setCreatorId(userId);
        application.setTenantId(tenantId);
        application.setCreateTime(new Date());
        application.setUpdateTime(new Date());
        application.setSecurityLevel("green");
        application.setDefaultPass(1);
        application.setAuditOpinion("통신경과");
        RobotExecute robotExecute = new RobotExecute();
        robotExecute.setRobotId(applicationDto.getRobotId());
        robotExecute.setCreatorId(userId);
        robotExecute.setTenantId(tenantId);
        robotExecute.setName(applicationDto.getAppName());
        PublishInfoBo bo = new PublishInfoBo();
        bo.setRobotExecute(robotExecute);
        bo.setNextVersion(applicationDto.getRobotVersion());
        ObjectMapper objectMapper = new ObjectMapper();
        String publishInfo = objectMapper.writeValueAsString(bo);
        application.setPublishInfo(publishInfo);

        // 저장마켓정보
        String marketInfoJson = convertMarketInfoToJson(
                applicationDto.getMarketIdList(), applicationDto.getEditFlag(), applicationDto.getCategory());
        application.setMarketInfo(marketInfoJson);
        return application;
    }

    private AppResponse<String> beforeSubmitCheck(ReleaseApplicationDto applicationDto, String userId) {
        // 해당봇완료검토통신경과의위신청, 완료가져오기또는모듈의봇할 수 없음사용, 직선까지일신청통신경과검토
        // status 로 null
        //        this.update(
        //                new LambdaUpdateWrapper<AppApplication>()
        //                        .eq(AppApplication::getRobotId, applicationDto.getRobotId())
        //                        .eq(AppApplication::getCreatorId, userId)
        //                        .eq(AppApplication::getApplicationType, "release")
        //                        .eq(AppApplication::getStatus, AUDIT_STATUS_APPROVED)
        //                        .eq(AppApplication::getDeleted, 0)
        //                        .set(AppApplication::getStatus, AUDIT_STATUS_NULLIFY)
        //                        .set(AppApplication::getUpdateTime, new Date())
        //                        .set(AppApplication::getUpdaterId, userId)
        //        );

        // 조회여부완료있음대기검토의신청
        AppApplication existingApplication = appApplicationDao.selectOne(new LambdaQueryWrapper<AppApplication>()
                .eq(AppApplication::getRobotId, applicationDto.getRobotId())
                .eq(AppApplication::getCreatorId, userId)
                .eq(AppApplication::getApplicationType, "release")
                .eq(AppApplication::getStatus, "pending")
                .eq(AppApplication::getDeleted, 0));

        if (existingApplication != null) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "현재완료저장에서정상에서위검토의신청단일, 요청 관리후행공유");
        }
        return null;
    }

    @Override
    public AppResponse<?> preSubmitAfterPublishCheck(PreReleaseCheckDto dto) throws NoLoginException {
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        // 1. 테넌트여부열기시작위검토?미완료열기시작직선연결반환0
        AppApplicationTenant auditConfig = appApplicationTenantDao.getByTenantId(tenantId);
        if (auditConfig == null || AUDIT_ENABLE_OFF.equals(auditConfig.getAuditEnable())) {
            return AppResponse.success(0);
        }

        String robotId = dto.getRobotId();
        AppApplication robotApplication = appApplicationDao.getLatestApplicationByRobotId(robotId, tenantId);
        if (robotApplication != null) {
            if ("green".equals(robotApplication.getSecurityLevel()) && 1 == robotApplication.getDefaultPass()) {
                return AppResponse.success(0);
            }
        }

        // 2. 조회여부봇여부위경과
        List<AppMarketResource> appInfoList = appMarketResourceDao.getAppInfoByRobotId(dto.getRobotId(), userId);
        if (!CollectionUtils.isEmpty(appInfoList)) {
            // 위경과  조회
            AppMarketResource appMarketResourceAnyOne = appInfoList.get(0);
            MarketResourceDto marketResourceDto = new MarketResourceDto();
            marketResourceDto.setMarketId(appMarketResourceAnyOne.getMarketId());
            marketResourceDto.setAppId(appMarketResourceAnyOne.getAppId());
            AppMarketVersion latestAppVersion = appMarketVersionDao.getLatestAppVersionInfo(marketResourceDto);
            String category = latestAppVersion.getCategory();
            Integer editFlag = latestAppVersion.getEditFlag();
            List<String> marketIdList =
                    appInfoList.stream().map(AppMarketResource::getMarketId).collect(Collectors.toList());
            MarketInfoVo vo = new MarketInfoVo();
            vo.setMarketIdList(marketIdList);
            vo.setEditFlag(editFlag);
            vo.setCategory(category);
            return AppResponse.success(vo);
        }
        return AppResponse.success(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public AppResponse<String> submitAfterPublish(SubmitAfterPublishDto dto) throws Exception {
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
        /**
         * 선택색상비밀단계통신경과
         */
        AppResponse<String> greenPass = greenPassHandle(dto, userId, tenantId);
        if (greenPass != null) return greenPass;

        AppResponse<String> E_SERVICE = beforeSubmitCheck(dto, userId);
        if (E_SERVICE != null) return E_SERVICE;

        // 생성위검토신청
        AppApplication application = new AppApplication();
        application.setRobotId(dto.getRobotId());
        application.setRobotVersion(dto.getRobotVersion());
        application.setApplicationType("release");
        application.setStatus("pending");
        application.setCreatorId(userId);
        application.setTenantId(tenantId);
        application.setCreateTime(new Date());
        application.setUpdateTime(new Date());
        application.setDeleted(0);

        dto.setCreatorId(userId);
        dto.setTenantId(tenantId);
        String publishInfo = convertPublishInfoBoToJson(dto);

        // 결과가예일 발송버전의
        if (dto.getRobotVersion() <= 1) {
            // 저장마켓정보
            String marketInfoJson =
                    convertMarketInfoToJson(dto.getMarketIdList(), dto.getEditFlag(), dto.getCategory());
            application.setMarketInfo(marketInfoJson);
        }

        application.setPublishInfo(publishInfo);
        appApplicationDao.insert(application);

        return AppResponse.success("위신청제출성공, 요청대기검토");
    }

    /**
     * 에서JSON문자열파싱마켓정보
     */
    private MarketInfoDto parseMarketInfoFromJson(String marketInfoJson) {
        if (StringUtils.isBlank(marketInfoJson)) {
            return null;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(marketInfoJson, MarketInfoDto.class);
        } catch (JsonProcessingException e) {
            log.error("파싱마켓정보JSON실패", e);
            return null;
        }
    }

    /**
     * 를마켓정보변환로JSON문자열
     */
    private String convertMarketInfoToJson(List<String> marketIdList, Integer editFlag, String category) {
        try {
            MarketInfoDto marketInfoDto = new MarketInfoDto();
            marketInfoDto.setMarketIdList(marketIdList);
            marketInfoDto.setEditFlag(editFlag);
            marketInfoDto.setCategory(category);
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(marketInfoDto);
        } catch (JsonProcessingException e) {
            log.error("변환마켓정보로JSON실패", e);
            return null;
        }
    }

    private String convertPublishInfoBoToJson(SubmitAfterPublishDto dto) throws JsonProcessingException {
        String creatorId = dto.getCreatorId();
        String tenantId = dto.getTenantId();
        String robotId = dto.getRobotId();
        String name = dto.getName();
        RobotExecute robotExecute = new RobotExecute();
        robotExecute.setRobotId(robotId);
        robotExecute.setCreatorId(creatorId);
        robotExecute.setTenantId(tenantId);
        robotExecute.setName(name);

        PublishInfoBo bo = new PublishInfoBo();
        bo.setRobotExecute(robotExecute);
        bo.setNextVersion(dto.getRobotVersion());
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(bo);
    }

    private PublishInfoBo parsePublishInfoBoFromJson(String PublishInfoBoJson) throws JsonProcessingException {
        if (StringUtils.isBlank(PublishInfoBoJson)) {
            return null;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(PublishInfoBoJson, PublishInfoBo.class);
    }

    /**
     * 조회여부열기시작완료위검토공가능
     */
    public boolean isAuditFunctionEnabled(String tenantId) {
        // 필요조회검토공가능열기닫기상태
        AppApplicationTenant currentConfig = appApplicationTenantDao.getByTenantId(tenantId);
        if (currentConfig == null) {
            return false; // 닫기, 있음공가능
        }
        return currentConfig.getAuditEnable() == 1;
    }

    public List<LatestVersionRobotVo> getRobotListApplicationStatus(List<LatestVersionRobotVo> voList) {
        voList.removeIf(Objects::isNull);
        if (voList.isEmpty()) {
            return voList;
        }
        // 가져오기robotId및latestVersion생성조회파일
        List<String> robotIds = voList.stream()
                .map(LatestVersionRobotVo::getRobotId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (robotIds.isEmpty()) {
            // 결과가있음robotId, 직선연결로none
            voList.forEach(vo -> vo.setApplicationStatus("none"));
            return voList;
        }

        // 조회검토기록
        List<AppApplication> applications = this.list(
                new LambdaQueryWrapper<AppApplication>()
                        .in(AppApplication::getRobotId, robotIds)
                        .eq(AppApplication::getDeleted, 0)
                        .eq(AppApplication::getCloudDeleted, 0)
                        .eq(AppApplication::getApplicationType, "release") // 조회위신청
                );

        // 생성Map조회: robotId_robotVersion -> status
        Map<String, String> robotVersionStatusMap = applications.stream()
                .collect(Collectors.toMap(
                        app -> app.getRobotId() + "_" + app.getRobotVersion(),
                        AppApplication::getStatus,
                        (existing, replacement) -> existing // 결과가있음재복사, 보관있음의
                        ));

        // applicationStatus
        voList.forEach(vo -> {
            vo.setApplicationStatus("none");

            String key = vo.getRobotId() + "_" + vo.getLatestVersion();
            String status = robotVersionStatusMap.get(key);
            if (status != null) {
                if (Objects.equals(status, AUDIT_STATUS_PENDING) || Objects.equals(status, AUDIT_STATUS_APPROVED)) {
                    vo.setApplicationStatus(status);
                }
            }
        });
        return voList;
    }

    @Override
    public AppResponse<IPage<MyApplicationPageListVo>> getMyApplicationPageList(MyApplicationPageListDto queryDto)
            throws NoLoginException {
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        queryDto.setTenantId(tenantId);

        // 결과가있음입력userId, 이면사용현재로그인사용자ID
        if (StringUtils.isBlank(queryDto.getUserId())) {
            AppResponse<User> res = rpaAuthFeign.getLoginUser();
            if (res == null || res.getData() == null) {
                throw new ServiceException("사용자 정보 조회 실패");
            }
            User loginUser = res.getData();
            String userId = loginUser.getId();

            queryDto.setUserId(userId);
        }

        IPage<MyApplicationPageListVo> pageConfig = new Page<>(queryDto.getPageNo(), queryDto.getPageSize(), true);
        IPage<MyApplicationPageListVo> myApplicationPage =
                appApplicationDao.getMyApplicationPageList(pageConfig, queryDto);
        List<MyApplicationPageListVo> records = myApplicationPage.getRecords();
        records.removeIf(Objects::isNull);

        return AppResponse.success(myApplicationPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<String> cancelMyApplication(MyApplicationDto dto) throws NoLoginException {
        if (dto == null || StringUtils.isBlank(dto.getId())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "신청ID비워 둘 수 없습니다");
        }
        AppApplication application = this.getById(dto.getId());
        if (application == null || application.getDeleted() == 1 || application.getCloudDeleted() == 1) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EMPTY, "신청을 찾을 수 없거나 이미 삭제되었습니다");
        }
        if (!AUDIT_STATUS_PENDING.equals(application.getStatus())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK, "가능판매대기검토상태의신청");
        }
        AppResponse<User> res = rpaAuthFeign.getLoginUser();
        if (res == null || !res.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = res.getData();
        String userId = loginUser.getId();

        if (!userId.equals(application.getCreatorId())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK, "가능판매의신청");
        }
        application.setStatus(AUDIT_STATUS_CANCELED);
        application.setUpdateTime(new Date());
        application.setUpdaterId(userId);
        boolean updateResult = this.updateById(application);
        if (!updateResult) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EXCEPTION, "판매신청실패");
        }
        return AppResponse.success("판매신청성공");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<String> deleteMyApplication(MyApplicationDto dto) throws NoLoginException {
        if (dto == null || StringUtils.isBlank(dto.getId())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "신청ID비워 둘 수 없습니다");
        }
        AppApplication application = this.getById(dto.getId());
        if (application == null || application.getDeleted() == 1) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EMPTY, "신청을 찾을 수 없거나 이미 삭제되었습니다");
        }
        AppResponse<User> res = rpaAuthFeign.getLoginUser();
        if (res == null || !res.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = res.getData();
        String userId = loginUser.getId();
        if (!userId.equals(application.getCreatorId())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK, "가능삭제의신청");
        }
        // 근거상태지정삭제방식
        String status = application.getStatus();

        if (AUDIT_STATUS_APPROVED.equals(status) || AUDIT_STATUS_REJECTED.equals(status)) {
            // 완료통신경과/완료돌아가기의신청기록, 클라이언트단일방법삭제
            application.setClientDeleted(1);
        } else if (AUDIT_STATUS_PENDING.equals(status)) {
            // 대기검토의신청기록직선연결삭제
            application.setDeleted(1);
        } else if (AUDIT_STATUS_CANCELED.equals(status)) {
            // 완료판매의신청기록삭제
            application.setCloudDeleted(1);
            application.setDeleted(1);
        } else {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK, "지원하지 않는의신청상태: " + status);
        }
        application.setUpdateTime(new Date());
        AppResponse<User> resp = rpaAuthFeign.getLoginUser();
        if (resp == null || !resp.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User nowUser = resp.getData();
        String nowUserId = nowUser.getId();
        application.setUpdaterId(nowUserId);
        boolean updateResult = this.updateById(application);
        if (!updateResult) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EXCEPTION, "삭제신청실패");
        }
        return AppResponse.success("삭제신청성공");
    }

    @Override
    public void packageApplicationInfo(
            List<AppMarketResource> appResourceList, List<ResVerDto> resVerDtoList, String userId) {
        if (appResourceList == null || resVerDtoList == null || appResourceList.isEmpty() || resVerDtoList.isEmpty()) {
            return;
        }
        List<String> robotIds = resVerDtoList.stream()
                .map(ResVerDto::getRobotId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (robotIds.isEmpty()) return;

        // 통신경과위신청비밀단계식별자
        packageReleaseApplicationInfo(appResourceList, resVerDtoList, robotIds);

        // 통신경과사용신청경과시간
        packageUseApplicationInfo(appResourceList, resVerDtoList, robotIds, userId);
    }

    private void packageReleaseApplicationInfo(
            List<AppMarketResource> appResourceList, List<ResVerDto> resVerDtoList, List<String> robotIds) {
        List<AppApplication> releaseApplicationList = this.list(new LambdaQueryWrapper<AppApplication>()
                .in(AppApplication::getRobotId, robotIds)
                .eq(AppApplication::getApplicationType, "release")
                .eq(AppApplication::getStatus, AUDIT_STATUS_APPROVED)
                .eq(AppApplication::getDeleted, 0)
                .eq(AppApplication::getCloudDeleted, 0));
        Map<String, AppApplication> appMap = releaseApplicationList.stream()
                .collect(Collectors.toMap(
                        app -> app.getRobotId() + "_" + app.getRobotVersion(),
                        app -> app,
                        (existing, replacement) -> existing));
        Map<String, Integer> robotVersionMap = resVerDtoList.stream()
                .collect(Collectors.toMap(
                        ResVerDto::getRobotId, ResVerDto::getLatestAppVersion, (existing, replacement) -> existing));
        appResourceList.forEach(resource -> {
            String robotId = resource.getRobotId();
            if (StringUtils.isBlank(robotId)) return;
            Integer version = robotVersionMap.get(robotId);
            if (version == null) return;
            AppApplication app = appMap.get(robotId + "_" + version);
            if (app != null) {
                resource.setSecurity_level(app.getSecurityLevel());
            }
        });
    }

    private void packageUseApplicationInfo(
            List<AppMarketResource> appResourceList,
            List<ResVerDto> resVerDtoList,
            List<String> robotIds,
            String userId) {
        List<AppApplication> useApplicationList = this.list(new LambdaQueryWrapper<AppApplication>()
                .in(AppApplication::getRobotId, robotIds)
                .eq(AppApplication::getApplicationType, "use")
                .eq(AppApplication::getStatus, AUDIT_STATUS_APPROVED)
                .eq(AppApplication::getCreatorId, userId)
                .eq(AppApplication::getDeleted, 0)
                .eq(AppApplication::getCloudDeleted, 0));
        Map<String, AppApplication> appMap = useApplicationList.stream()
                .collect(Collectors.toMap(
                        app -> app.getRobotId() + "_" + app.getRobotVersion(),
                        app -> app,
                        (existing, replacement) -> existing));
        Map<String, Integer> robotVersionMap = resVerDtoList.stream()
                .collect(Collectors.toMap(
                        ResVerDto::getRobotId, ResVerDto::getLatestAppVersion, (existing, replacement) -> existing));
        appResourceList.forEach(resource -> {
            String robotId = resource.getRobotId();
            if (StringUtils.isBlank(robotId)) return;

            Integer version = robotVersionMap.get(robotId);
            if (version == null) return;

            AppApplication app = appMap.get(robotId + "_" + version);
            if (app != null) {
                // 사용제한 에서 use유형의데이터중가져오기
                resource.setExpiry_date(app.getExpireTime());
                resource.setExpiry_date_str(getExpiryDateString(app.getExpireTime()));
            }
        });
    }

    @Override
    public void packageUsePermission(List<ExecuteListVo> ansRecords) throws NoLoginException {
        ansRecords.removeIf(Objects::isNull);
        if (ansRecords.isEmpty()) {
            return;
        }
        // 1. 조회현재테넌트여부열기시작완료위검토
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        boolean isAuditEnabled = isAuditFunctionEnabled(tenantId);
        AppResponse<User> res = rpaAuthFeign.getLoginUser();
        if (res == null || !res.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = res.getData();
        String currentUserId = loginUser.getId();

        // ===== 가능: 량조회적음데이터베이스IO =====
        // 모든마켓사용의appId, 통신경과재재복사조회
        List<String> enterpriseAppIds = ansRecords.stream()
                .filter(record -> "마켓".equals(record.getSourceName()))
                .map(ExecuteListVo::getAppId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        // 조회데이터저장, 에서중재복사데이터베이스조회
        Map<String, AppApplication> approvedApplicationMap = new HashMap<>();
        Map<String, AppApplication> userApplicationMap = new HashMap<>();

        if (isAuditEnabled && !enterpriseAppIds.isEmpty()) {
            // 1. 조회모든닫기의위신청(조회사용자에서마켓중의사용)
            approvedApplicationMap = batchGetApprovedApplications(enterpriseAppIds, tenantId, currentUserId);

            // 2. 모든robotId, 량조회사용자사용신청(필요)
            Set<String> robotIds = approvedApplicationMap.values().stream()
                    .map(AppApplication::getRobotId)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());

            if (!robotIds.isEmpty()) {
                userApplicationMap = batchGetUserApplications(robotIds, currentUserId);
            }
        }

        // 2. 관리매개기록
        for (ExecuteListVo record : ansRecords) {
            // 권한로null
            record.setUsePermission(null);
            // 관리마켓사용
            if (!"마켓".equals(record.getSourceName())) {
                continue;
            }
            // 검토공가능미완료열기시작, 직선연결로있음권한
            if (!isAuditEnabled) {
                record.setUsePermission(1);
                continue;
            }
            // 검토공가능완료열기시작, 조회권한경과시간
            checkMarketAppPermissionOptimized(record, currentUserId, approvedApplicationMap, userApplicationMap);
        }
    }

    /**
     * 량조회위신청
     * 비고: DAOgetApplicationByObtainedAppId방법법의제한제어, 필요개조회
     * 통신경과및재, 적음완료의조회데이터
     * 조회사용자에서팀마켓중의사용의위신청
     */
    private Map<String, AppApplication> batchGetApprovedApplications(
            List<String> appIds, String tenantId, String userId) {
        Map<String, AppApplication> resultMap = new HashMap<>();

        for (String appId : appIds) {
            try {
                AppApplication approvedApp = appApplicationDao.getApplicationByObtainedAppId(appId, tenantId, userId);
                if (approvedApp != null) {
                    resultMap.put(appId, approvedApp);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return resultMap;
    }

    /**
     * 량조회사용자사용신청
     */
    private Map<String, AppApplication> batchGetUserApplications(Set<String> robotIds, String currentUserId) {
        List<AppApplication> userApplications = this.list(new LambdaQueryWrapper<AppApplication>()
                .in(AppApplication::getRobotId, robotIds)
                .eq(AppApplication::getApplicationType, "use")
                .eq(AppApplication::getStatus, AUDIT_STATUS_APPROVED)
                .eq(AppApplication::getCreatorId, currentUserId)
                .eq(AppApplication::getDeleted, 0)
                .eq(AppApplication::getCloudDeleted, 0));

        return userApplications.stream()
                .collect(Collectors.toMap(
                        AppApplication::getRobotId, app -> app, (existing, replacement) -> existing // 보관일개매칭의기록
                        ));
    }

    /**
     * 조회마켓사용의사용권한(버전, 사용조회의데이터)
     */
    private void checkMarketAppPermissionOptimized(
            ExecuteListVo record,
            String currentUserId,
            Map<String, AppApplication> approvedApplicationMap,
            Map<String, AppApplication> userApplicationMap) {
        String appId = record.getAppId();
        if (StringUtils.isBlank(appId)) {
            record.setUsePermission(0);
            return;
        }

        // 에서조회의중가져오기위신청
        AppApplication approvedApplication = approvedApplicationMap.get(appId);
        if (approvedApplication == null) {
            record.setUsePermission(1);
            return;
        }

        // 조회비밀단계권한
        String securityLevel = approvedApplication.getSecurityLevel();
        if (securityLevel == null || StringUtils.isBlank(securityLevel)) {
            record.setUsePermission(1);
            return;
        }

        // 사용신청의robotId
        String useApplicationRobotId = approvedApplication.getRobotId();

        // 근거비밀단계조회권한
        boolean hasPermission = checkUserPermissionForSecurityLevelOptimized(
                currentUserId,
                securityLevel,
                approvedApplication.getAllowedDept(),
                useApplicationRobotId,
                userApplicationMap);
        record.setUsePermission(hasPermission ? 1 : 0);

        // 경과시간
        if ("red".equals(securityLevel)) {
            // 에서조회의중가져오기사용자신청
            AppApplication userApplication = userApplicationMap.get(useApplicationRobotId);
            if (userApplication != null) {
                Date expireDate = userApplication.getExpireTime();
                record.setExpiryDate(expireDate);
                record.setExpiryDateStr(getExpiryDateString(expireDate));
            }
        }
    }

    /**
     * 근거비밀단계식별자조회사용자권한(버전, 사용량관리)
     */
    private boolean checkUserPermissionForSecurityLevelOptimized(
            String currentUserId,
            String securityLevel,
            String allowedDept,
            String robotId,
            Map<String, AppApplication> userApplicationMap) {
        switch (securityLevel) {
            case "green":
                return true;
            case "yellow":
                return checkYellowLevelPermissionOptimized(currentUserId, allowedDept, robotId, userApplicationMap);
            case "red":
                return checkRedLevelPermissionOptimized(currentUserId, robotId, userApplicationMap);
            default:
                return false;
        }
    }

    /**
     * 조회색상비밀단계권한(버전)
     */
    private boolean checkYellowLevelPermissionOptimized(
            String currentUserId, String allowedDept, String robotId, Map<String, AppApplication> userApplicationMap) {
        if (StringUtils.isBlank(allowedDept)) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "모듈 ID를 찾을 수 없습니다");
        }
        // 1. 조회사용자여부지정모듈
        if (isUserInAllowedDept(currentUserId, allowedDept)) {
            return true;
        }
        // 2. 조회사용자여부있음통신경과의사용신청(에서조회중가져오기)
        return userApplicationMap.containsKey(robotId);
    }

    /**
     * 조회색상비밀단계권한(버전)
     */
    private boolean checkRedLevelPermissionOptimized(
            String currentUserId, String robotId, Map<String, AppApplication> userApplicationMap) {
        // 에서조회중가져오기사용자신청
        AppApplication userApplication = userApplicationMap.get(robotId);
        if (userApplication == null) {
            return false;
        }

        // 조회여부경과
        Date expireDate = userApplication.getExpireTime();
        return expireDate == null || expireDate.after(new Date());
    }

    /**
     * 계획경과시간문자열
     */
    private String getExpiryDateString(Date expireTime) {
        if (expireTime == null) return null;

        int days = DateUtils.differentDaysByMillisecond(new Date(), expireTime);
        if (days > 0) return days + "후까지";
        if (days == 0) return "일까지";
        return "완료경과";
    }

    /**
     * 근거비밀단계식별자조회사용자권한(사용권한조회연결)
     */
    private boolean checkUserPermissionForSecurityLevel(
            String currentUserId, String securityLevel, String allowedDept, String robotId) {
        switch (securityLevel) {
            case "green":
                return true;
            case "yellow":
                return checkYellowLevelPermission(currentUserId, allowedDept, robotId);
            case "red":
                return checkRedLevelPermission(currentUserId, robotId);
            default:
                return false;
        }
    }

    /**
     * 조회색상비밀단계권한
     */
    private boolean checkYellowLevelPermission(String currentUserId, String allowedDept, String robotId) {
        if (StringUtils.isBlank(allowedDept)) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "모듈 ID를 찾을 수 없습니다");
        }
        // 1. 조회사용자여부지정모듈
        if (isUserInAllowedDept(currentUserId, allowedDept)) {
            return true;
        }
        // 2. 조회사용자여부있음통신경과의사용신청
        return hasApprovedUseApplication(currentUserId, robotId);
    }

    /**
     * 조회색상비밀단계권한
     */
    private boolean checkRedLevelPermission(String currentUserId, String robotId) {
        // 조회사용자여부있음통신경과의사용신청
        AppApplication userApplication = getUserUseApplication(currentUserId, robotId);
        if (userApplication == null) {
            return false;
        }

        // 조회여부경과
        Date expireDate = userApplication.getExpireTime();
        return expireDate == null || expireDate.after(new Date());
    }

    /**
     * 조회사용자여부지정모듈
     */
    private boolean isUserInAllowedDept(String currentUserId, String allowedDept) {
        AppResponse<String> appResponse = rpaAuthFeign.getDeptIdByUserId(
                currentUserId, rpaAuthFeign.getTenantId().getData());
        String userDeptId = appResponse.getData();
        return StringUtils.isNotBlank(userDeptId) && allowedDept.contains(userDeptId);
    }

    /**
     * 조회사용자여부있음통신경과의사용신청
     */
    private boolean hasApprovedUseApplication(String currentUserId, String robotId) {
        return getUserUseApplication(currentUserId, robotId) != null;
    }

    /**
     * 가져오기 봇,사용자의사용신청
     */
    private AppApplication getUserUseApplication(String currentUserId, String robotId) {
        return this.getOne(new LambdaQueryWrapper<AppApplication>()
                .eq(AppApplication::getRobotId, robotId)
                .eq(AppApplication::getApplicationType, "use")
                .eq(AppApplication::getStatus, AUDIT_STATUS_APPROVED)
                .eq(AppApplication::getCreatorId, currentUserId)
                .eq(AppApplication::getDeleted, 0)
                .eq(AppApplication::getCloudDeleted, 0)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<String> submitUseApplication(UsePermissionCheckDto dto) throws Exception {
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        dto.setTenantId(tenantId);
        AppResponse<User> res = rpaAuthFeign.getLoginUser();
        if (res == null || !res.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = res.getData();
        String userId = loginUser.getId();
        dto.setUserId(userId);

        List<String> appIdList = Collections.singletonList(dto.getAppId());
        List<ResVerDto> resVerDtoList = appMarketVersionDao.getResVerJoin(dto.getMarketId(), appIdList);
        if (resVerDtoList.isEmpty()) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EMPTY, "사용 버전 정보가 없습니다");
        }
        String robotId = resVerDtoList.get(0).getRobotId();
        Integer version = resVerDtoList.get(0).getLatestAppVersion();

        // 조회여부재복사제출
        AppApplication existingUseApplication = this.getOne(new LambdaQueryWrapper<AppApplication>()
                .eq(AppApplication::getRobotId, robotId)
                .eq(AppApplication::getApplicationType, "use")
                .eq(AppApplication::getCreatorId, dto.getUserId())
                .eq(AppApplication::getDeleted, 0)
                .eq(AppApplication::getCloudDeleted, 0)
                .in(AppApplication::getStatus, Arrays.asList(AUDIT_STATUS_PENDING, AUDIT_STATUS_APPROVED))
                .last("LIMIT 1"));
        if (existingUseApplication != null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK, "완료있음해당사용의사용신청, 요청 재복사제출");
        }
        AppApplication approvedReleaseApplication = this.getOne(new LambdaQueryWrapper<AppApplication>()
                .eq(AppApplication::getRobotId, robotId)
                .eq(AppApplication::getRobotVersion, version)
                .eq(AppApplication::getApplicationType, "release")
                .eq(AppApplication::getStatus, AUDIT_STATUS_APPROVED)
                .eq(AppApplication::getDeleted, 0)
                .eq(AppApplication::getCloudDeleted, 0)
                .last("LIMIT 1"));
        if (approvedReleaseApplication == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK, "위신청찾을 수 없습니다");
        }
        AppApplication useApplication = new AppApplication();
        useApplication.setSecurityLevel(approvedReleaseApplication.getSecurityLevel());
        useApplication.setRobotId(robotId);
        useApplication.setRobotVersion(version);
        useApplication.setApplicationType("use");
        useApplication.setStatus(AUDIT_STATUS_PENDING);
        useApplication.setCreatorId(dto.getUserId());
        useApplication.setTenantId(dto.getTenantId());
        useApplication.setCreateTime(new Date());
        useApplication.setUpdaterId(dto.getUserId());
        useApplication.setUpdateTime(new Date());
        useApplication.setDeleted(0);
        useApplication.setClientDeleted(0);
        useApplication.setCloudDeleted(0);
        boolean saved = this.save(useApplication);
        if (!saved) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EXCEPTION, "제출사용신청실패");
        }
        return AppResponse.success("사용신청제출성공");
    }

    @Override
    public AppResponse<Integer> usePermissionCheck(UsePermissionCheckDto dto) throws Exception {
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        // 1. 매개변수인증
        dto.setTenantId(tenantId);

        AppResponse<User> res = rpaAuthFeign.getLoginUser();
        if (res == null || !res.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = res.getData();
        String userId = loginUser.getId();
        dto.setUserId(userId);
        // 2. 조회AppResource여부저장에서
        MarketDto marketDto = new MarketDto();
        marketDto.setMarketId(dto.getMarketId());
        marketDto.setAppId(dto.getAppId());
        AppMarketResource appResource = appMarketResourceDao.getAppInfoByAppId(marketDto);
        if (appResource == null) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EMPTY, "사용 정보를 찾을 수 없습니다");
        }
        // 3. 조회위검토여부열기시작
        tenantId = marketDto.getTenantId();
        AppApplicationTenant auditConfig = appApplicationTenantDao.getByTenantId(tenantId);
        // 결과가검토미완료열기시작, 직선연결반환yes
        if (auditConfig == null || AUDIT_ENABLE_OFF.equals(auditConfig.getAuditEnable())) {
            return AppResponse.success(1);
        }
        // 4. 가져오기 새버전의robotId
        List<String> appIdList = Collections.singletonList(dto.getAppId());
        List<ResVerDto> resVerDtoList = appMarketVersionDao.getResVerJoin(dto.getMarketId(), appIdList);
        if (resVerDtoList.isEmpty()) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EMPTY, "사용의robotId찾을 수 없습니다");
        }
        String robotId = resVerDtoList.get(0).getRobotId();
        // 5. 조회여부있음통신경과의위검토
        AppApplication approvedApplication = this.getOne(new LambdaQueryWrapper<AppApplication>()
                .eq(AppApplication::getRobotId, robotId)
                .eq(AppApplication::getApplicationType, "release")
                .eq(AppApplication::getStatus, AUDIT_STATUS_APPROVED)
                .eq(AppApplication::getDeleted, 0)
                .eq(AppApplication::getCloudDeleted, 0)
                .last("LIMIT 1"));
        // 결과가있음통신경과의위검토, 예사용완료위
        if (approvedApplication == null) {
            return AppResponse.success(1);
        }
        // 6. 조회해당위검토의비밀단계식별자, 근거비밀단계식별자조회해당사용자여부있음사용권한
        String securityLevel = approvedApplication.getSecurityLevel();
        if (StringUtils.isBlank(securityLevel)) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EMPTY, "사용있음비밀단계식별자");
        }
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User nowUser = response.getData();
        String nowUserId = nowUser.getId();
        // 근거비밀단계식별자조회사용자사용권한
        boolean hasPermission = checkUserPermissionForSecurityLevel(
                nowUserId, securityLevel, approvedApplication.getAllowedDept(), robotId);
        return AppResponse.success(hasPermission ? 1 : 0);
    }
}