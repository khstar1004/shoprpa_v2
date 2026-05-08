package com.iflytek.rpa.market.service.impl;

import static com.iflytek.rpa.robot.constants.RobotConstant.*;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Sets;
import com.iflytek.rpa.base.dao.*;
import com.iflytek.rpa.base.entity.CParam;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.common.feign.entity.dto.GetDeployedUserListDto;
import com.iflytek.rpa.common.feign.entity.dto.PageDto;
import com.iflytek.rpa.component.dao.ComponentRobotUseDao;
import com.iflytek.rpa.component.entity.ComponentRobotUse;
import com.iflytek.rpa.market.dao.*;
import com.iflytek.rpa.market.entity.*;
import com.iflytek.rpa.market.entity.dto.*;
import com.iflytek.rpa.market.entity.vo.*;
import com.iflytek.rpa.market.service.AppApplicationService;
import com.iflytek.rpa.market.service.AppMarketResourceService;
import com.iflytek.rpa.quota.service.QuotaCheckService;
import com.iflytek.rpa.robot.dao.RobotDesignDao;
import com.iflytek.rpa.robot.dao.RobotExecuteDao;
import com.iflytek.rpa.robot.dao.RobotVersionDao;
import com.iflytek.rpa.robot.entity.RobotDesign;
import com.iflytek.rpa.robot.entity.RobotExecute;
import com.iflytek.rpa.robot.entity.RobotVersion;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.PrePage;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import com.iflytek.rpa.utils.response.QuotaCodeEnum;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * 팀마켓-테이블(AppMarketResource)테이블서비스유형
 *
 * @author mjren
 * @since 2024-10-21 14:36:30
 */
@Service("appMarketResourceService")
@RequiredArgsConstructor
public class AppMarketResourceServiceImpl extends ServiceImpl<AppMarketResourceDao, AppMarketResource>
        implements AppMarketResourceService {
    private final StringRedisTemplate stringRedisTemplate;
    private final String filePathPrefix = "/api/resource/file/download?fileId=";

    @Resource
    private AppMarketResourceDao appMarketResourceDao;

    @Autowired
    private AppApplicationDao appApplicationDao;

    @Autowired
    private AppMarketVersionDao appMarketVersionDao;

    @Autowired
    private AppMarketUserDao appMarketUserDao;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private RobotDesignDao robotDesignDao;

    @Autowired
    private RobotExecuteDao robotExecuteDao;

    @Autowired
    private RobotVersionDao robotVersionDao;

    @Autowired
    private CProcessDao processDao;

    @Autowired
    private CGroupDao groupDao;

    @Autowired
    private CElementDao elementDao;

    @Autowired
    private CGlobalVarDao globalVarDao;

    @Autowired
    private CRequireDao requireDao;

    @Autowired
    private CParamDao paramDao;

    @Autowired
    private CModuleDao cModuleDao;

    @Autowired
    private CSmartComponentDao cSmartComponentDao;

    @Autowired
    private ComponentRobotUseDao componentUseDao;

    @Autowired
    private AppApplicationTenantDao appApplicationTenantDao;

    @Autowired
    private AppApplicationService appApplicationService;

    @Autowired
    private QuotaCheckService quotaCheckService;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> shareRobot(ShareRobotDto marketResourceDto) throws NoLoginException {
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

        // 결과가열기시작완료위검토공가능
        if (isAuditFunctionEnabled(tenantId)) {
            return handleShareRobotWithAudit(marketResourceDto, userId, tenantId);
        }

        // 미완료열기시작검토공가능, 사용있음의공유
        return executeShareRobotLogic(marketResourceDto, userId, tenantId);
    }

    /**
     * 관리열기시작검토공가능시의공유
     */
    private AppResponse<?> handleShareRobotWithAudit(ShareRobotDto marketResourceDto, String userId, String tenantId) {
        String robotId = marketResourceDto.getRobotId();

        // 조회여부완료있음대기검토의신청
        AppApplication existingPendingApplication = appApplicationDao.selectOne(new LambdaQueryWrapper<AppApplication>()
                .eq(AppApplication::getRobotId, robotId)
                .eq(AppApplication::getCreatorId, userId)
                .eq(AppApplication::getApplicationType, "release")
                .eq(AppApplication::getStatus, "pending")
                .eq(AppApplication::getDeleted, 0));

        if (existingPendingApplication != null) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "해당봇완료있음대기검토의위신청, 요청대기검토결과");
        }

        // 조회여부완료있음검토통신경과의신청
        AppApplication approvedApplication = appApplicationDao.selectOne(new LambdaQueryWrapper<AppApplication>()
                .eq(AppApplication::getRobotId, robotId)
                .eq(AppApplication::getCreatorId, userId)
                .eq(AppApplication::getApplicationType, "release")
                .eq(AppApplication::getStatus, "approved")
                .eq(AppApplication::getDeleted, 0)
                .orderByDesc(AppApplication::getCreateTime)
                .last("LIMIT 1"));

        // 가져오기봇의사용버전
        RobotVersion robotVersion =
                robotVersionDao.getOriEnableVersion(marketResourceDto.getRobotId(), userId, tenantId);
        if (null == robotVersion || null == robotVersion.getVersion()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "봇없음버전정보");
        }

        // 조회여부예일위신청
        if (approvedApplication == null) {
            // 일위신청, 반환안내정보
            return AppResponse.success("관리관리원열기시작완료위전검토, 검토통신경과후방법가능공유앱 마켓, 확인하세요여부발송신청.");
        }
        Integer applicationVersion = approvedApplication.getRobotVersion();
        Integer toShareVersion = marketResourceDto.getVersion();
        // 아니요예일위신청, 결과가선택완료업데이트발송버전통신경과선택
        if (approvedApplication.getDefaultPass() != null && approvedApplication.getDefaultPass() == 1) {
            // 선택완료통신경과선택, 직선연결실행공유
            return executeShareRobotLogic(marketResourceDto, userId, tenantId);
        } else if (null != applicationVersion && applicationVersion.equals(toShareVersion)) {
            // 전송되지 않았습니다버전의전아래공유까지마켓, 직선연결실행공유, 필요하지 않습니다발송검토
            return executeShareRobotLogic(marketResourceDto, userId, tenantId);
        } else {
            // 미완료선택통신경과선택, 필요다시 발송검토신청
            return AppResponse.success("관리관리원열기시작완료위전검토, 검토통신경과후방법가능공유앱 마켓, 확인하세요여부발송신청.");
        }
    }

    /**
     * 실행공유(검토통신경과후호출)
     */
    @Override
    public AppResponse<?> executeShareRobotLogic(ShareRobotDto marketResourceDto, String userId, String tenantId) {
        List<String> marketIdList = marketResourceDto.getMarketIdList();
        if (CollectionUtils.isEmpty(marketIdList)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "적음마켓id");
        }
        marketResourceDto.setCreatorId(userId);
        marketResourceDto.setUpdaterId(userId);
        marketResourceDto.setTenantId(tenantId);
        String robotName = marketResourceDto.getAppName();
        if (null == robotName) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "적음봇이름");
        }

        // 가져오기봇의사용버전
        RobotVersion robotVersion =
                robotVersionDao.getOriEnableVersion(marketResourceDto.getRobotId(), userId, tenantId);
        if (null == robotVersion || null == robotVersion.getVersion()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "봇없음버전정보");
        }
        marketResourceDto.setVersion(robotVersion.getVersion());

        // 가져오기미완료열기마켓완료저장에서의appId
        List<AppMarketResource> appExestInfoList =
                appMarketResourceDao.getAppInfoByRobotId(marketResourceDto.getRobotId(), userId);
        Map<String, String> exestAppMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(appExestInfoList)) {
            exestAppMap = appExestInfoList.stream()
                    .collect(Collectors.toMap(
                            AppMarketResource::getMarketId, AppMarketResource::getAppId, (existingValue, newValue) -> {
                                // 관리재복사,  사용새값
                                return newValue;
                            }));
        }
        List<AppMarketResource> appInsertInfoList = new ArrayList<>();
        List<AppMarketResource> appUpdateInfoList = new ArrayList<>();
        for (String marketId : marketIdList) {
            AppMarketResource appMarketResource = new AppMarketResource();
            appMarketResource.setMarketId(marketId);
            if (!CollectionUtils.isEmpty(exestAppMap) && exestAppMap.containsKey(marketId)) {
                appMarketResource.setAppId(exestAppMap.get(marketId));
                appUpdateInfoList.add(appMarketResource);
            } else {
                // 제품appId
                appMarketResource.setAppId(idWorker.nextId() + "");
                appInsertInfoList.add(appMarketResource);
            }
        }
        if (!CollectionUtils.isEmpty(appInsertInfoList)) {
            // 일공유까지마켓, 삽입
            marketResourceDto.setAppInsertInfoList(appInsertInfoList);
            appMarketResourceDao.addAppResource(marketResourceDto);
            appMarketVersionDao.addAppVersionBatch(marketResourceDto);
        }
        if (!CollectionUtils.isEmpty(appUpdateInfoList)) {
            // 공유경과의마켓, 업데이트
            marketResourceDto.setAppUpdateInfoList(appUpdateInfoList);
            appMarketResourceDao.updateAppResource(marketResourceDto);
            // 1, 완료위, 위
            // 있음업데이트, 아니요출력삽입의;공유경과의마켓, 클릭공유시일지정예완료위상태, 있음버전의추가;버전추가, 발송버전시완료까지마켓완료
            appMarketVersionDao.updateAppVersionBatch(marketResourceDto);
        }
        robotDesignDao.updateTransformStatus(userId, marketResourceDto.getRobotId(), null, SHARED);
        return AppResponse.success(true);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> obtainRobot(MarketResourceDto marketResourceDto) throws NoLoginException {
        List<String> obtainDirectory = marketResourceDto.getObtainDirection();
        String robotName = marketResourceDto.getAppName();
        Integer appVersion = marketResourceDto.getVersion();
        String marketId = marketResourceDto.getMarketId();

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
        marketResourceDto.setCreatorId(userId);
        marketResourceDto.setUpdaterId(userId);
        marketResourceDto.setTenantId(tenantId);
        if (CollectionUtils.isEmpty(obtainDirectory)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "가져오기 대상을 선택하세요");
        }

        // 해당버전봇여부저장에서
        RobotVersion robotVersion = robotVersionDao.getVersionInfo(marketResourceDto);
        if (null == robotVersion) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "해당버전봇을 찾을 수 없습니다");
        }
        // 가져오기까지계획기기
        if (obtainDirectory.contains("design")) {
            // 검증계획기기매칭금액
            if (!quotaCheckService.checkDesignerQuota()) {
                AcceptResultVo resultVo = new AcceptResultVo(QuotaCodeEnum.E_OVER_LIMIT);
                return AppResponse.success(resultVo);
            }

            // 삽입봇테이블, 
            RobotDesign robotDesign = new RobotDesign();
            robotDesign.setName(robotName);
            robotDesign.setCreatorId(userId);
            robotDesign.setUpdaterId(userId);
            robotDesign.setTenantId(tenantId);
            // 재이름검증
            Long count = robotDesignDao.countRobotByName(robotDesign);
            if (null != count && count > 0) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "계획기기저장된 이름봇, 요청수정이름");
            }
            String newRobotId = idWorker.nextId() + "";
            robotDesign.setRobotId(newRobotId);
            robotDesign.setAppId(marketResourceDto.getAppId());
            robotDesign.setAppVersion(appVersion);
            robotDesign.setMarketId(marketId);
            robotDesign.setResourceStatus(OBTAINED);
            robotDesign.setDataSource(MARKET);
            // 조회코드권한
            AppMarketVersion appMarketVersion = appMarketVersionDao.getLatestAppVersionInfo(marketResourceDto);
            if (null == appMarketVersion) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM, "사용 버전을 찾을 수 없습니다");
            }
            // 가져오기 결과가예, 있음권한
            String authorId = robotVersion.getCreatorId();
            Integer editFlag = appMarketVersion.getEditFlag();
            if (null == editFlag || editFlag == 1 || userId.equals(authorId)) {
                robotDesign.setTransformStatus(EDITING);
            } else {
                robotDesign.setTransformStatus(LOCKED);
            }
            //            robotDesign.setEditEnable(editFlag);
            robotDesignDao.obtainRobotToDesign(robotDesign);
            // 복사프로세스대기데이터
            createDateForInit(robotDesign, robotVersion);

            increaseDownloadNum(marketResourceDto);
        }
        if (obtainDirectory.contains("execute")) {
            // 가져오기까지실행기기

            // 조회예가져오기
            Integer selfObtained = checkSelfObtain(marketResourceDto);
            if (selfObtained > 0) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE_NOT_SUPPORT, "본완료저장에서해당봇");
            }

            // 여부재복사가져오기
            Integer countObtained = robotExecuteDao.countObtainedExecute(marketResourceDto);
            if (countObtained > 0) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE_NOT_SUPPORT, "실행기기중해당봇현재버전완료저장에서");
            }
            RobotExecute robotExecute = new RobotExecute();
            BeanUtil.copyProperties(marketResourceDto, robotExecute);
            robotExecute.setName(robotName);
            robotExecute.setUpdateTime(new Date());
            robotExecute.setAppVersion(appVersion);
            robotExecute.setResourceStatus(OBTAINED);
            robotExecute.setDataSource(MARKET);
            // 조회해당사용여부가져오기경과, 
            Integer obtainCount = robotExecuteDao.getObtainCount(marketResourceDto);
            if (obtainCount > 0) {
                // 업데이트
                robotExecuteDao.updateObtainedRobot(robotExecute);
            } else {
                String newRobotId = idWorker.nextId() + "";
                // 삽입
                robotExecute.setRobotId(newRobotId);
                robotExecuteDao.insertObtainedRobot(robotExecute);

                // 컴포넌트의사용필요삽입일아래
                addCompUseList(newRobotId, appVersion, robotVersion, tenantId, userId);
            }
            increaseDownloadNum(marketResourceDto);
        }

        return AppResponse.success(true);
    }

    private Integer checkSelfObtain(MarketResourceDto marketResourceDto) {
        MarketDto marketDto = new MarketDto();
        BeanUtils.copyProperties(marketResourceDto, marketDto);
        AppMarketResource appResource = appMarketResourceDao.getAppInfoByAppId(marketDto);
        if (appResource == null) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "마켓 봇 정보를 찾을 수 없습니다");
        }
        String robotId = appResource.getRobotId();
        String creatorId = marketResourceDto.getCreatorId();
        String tenantId = marketResourceDto.getTenantId();
        if (!StringUtils.isEmpty(robotId) && !StringUtils.isEmpty(creatorId) && !StringUtils.isEmpty(tenantId)) {
            RobotExecute robotExecute = robotExecuteDao.getRobotExecute(robotId, creatorId, tenantId);
            if (null != robotExecute) {
                return 1;
            }
        }
        return 0;
    }

    public void addCompUseList(
            String newRobotId,
            Integer newRobotVersion,
            RobotVersion authorRobotVersion,
            String tenantId,
            String userId) {
        String authorRobotId = authorRobotVersion.getRobotId();
        Integer authorVersion = authorRobotVersion.getVersion();
        List<ComponentRobotUse> compUseListAuth =
                componentUseDao.getByRobotIdAndVersion(authorRobotId, authorVersion, tenantId);
        if (CollectionUtils.isEmpty(compUseListAuth)) {
            return;
        }

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

    private void increaseDownloadNum(MarketResourceDto marketResourceDto) {
        AppMarketResource appResource =
                appMarketResourceDao.getAppResource(marketResourceDto.getAppId(), marketResourceDto.getMarketId());
        if (appResource == null) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "마켓 봇 정보를 찾을 수 없습니다");
        }
        Long downloadNum = appResource.getDownloadNum();
        appResource.setDownloadNum(downloadNum == null ? 1L : downloadNum + 1L);
        appMarketResourceDao.updateById(appResource);
    }

    /**
     * 가져오기까지의봇의데이터
     *
     * @param obtainedRobotDesign
     * @param authorRobotVersion
     */
    public void createDateForInit(RobotDesign obtainedRobotDesign, RobotVersion authorRobotVersion) {
        // 프로세스
        processDao.createProcessForObtainedVersion(obtainedRobotDesign, authorRobotVersion);
        // 그룹
        groupDao.createGroupForObtainedVersion(obtainedRobotDesign, authorRobotVersion);
        // 요소
        elementDao.createElementForObtainedVersion(obtainedRobotDesign, authorRobotVersion);
        // 전역 변수
        globalVarDao.createGlobalVarForObtainedVersion(obtainedRobotDesign, authorRobotVersion);
        // python
        requireDao.createRequireForObtainedVersion(obtainedRobotDesign, authorRobotVersion);
        // 가능컴포넌트
        cSmartComponentDao.createSmartComponentForObtainedVersion(obtainedRobotDesign, authorRobotVersion);
        // python모듈코드
        cModuleDao.createModuleForObtainedVersion(obtainedRobotDesign, authorRobotVersion);
        // 구성 매개변수
        createParamForCurrentVersion(obtainedRobotDesign, authorRobotVersion);
        // 컴포넌트사용
        addCompUseList(
                obtainedRobotDesign.getRobotId(),
                0,
                authorRobotVersion,
                obtainedRobotDesign.getTenantId(),
                obtainedRobotDesign.getCreatorId());
    }

    public void createParamForCurrentVersion(RobotDesign obtainedRobotDesign, RobotVersion authorRobotVersion) {
        // 조회사용자지정버전의모든매개변수
        List<CParam> cParamList =
                paramDao.getAllParams(null, authorRobotVersion.getRobotId(), authorRobotVersion.getVersion());
        if (CollectionUtils.isEmpty(cParamList)) {
            return;
        }
        for (CParam cParam : cParamList) {
            cParam.setId(idWorker.nextId() + "");
            cParam.setRobotId(obtainedRobotDesign.getRobotId());
            // 업데이트버전
            cParam.setRobotVersion(0);
            cParam.setCreatorId(obtainedRobotDesign.getCreatorId());
            cParam.setCreateTime(new Date());
            cParam.setUpdaterId(obtainedRobotDesign.getUpdaterId());
            cParam.setUpdateTime(new Date());
        }
        paramDao.createParamForCurrentVersion(cParamList);
    }

    @Override
    public AppResponse<?> deployRobot(MarketDto marketDto) {
        List<String> userIdList = marketDto.getUserIdList();
        if (CollectionUtils.isEmpty(userIdList)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "적음사용자id");
        }
        String appId = marketDto.getAppId();
        String marketId = marketDto.getMarketId();
        if (StringUtils.isBlank(marketId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "마켓매개변수 실패");
        }
        if (StringUtils.isBlank(appId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        // 가져오기 여부에서본팀내부
        Set<String> marketUserSet = appMarketUserDao.getMarketUserListForDeploy(marketId, userIdList);

        for (String userId : userIdList) {
            if (null == userId) {
                continue;
            }
            if (!marketUserSet.contains(userId)) {
                return AppResponse.error(ErrorCodeEnum.E_SQL, "사용자가 해당 팀에 없습니다. 초대가 필요합니다");
            }
        }
        // 조회모듈버전: 새버전
        //        RobotVersion robotVersion = robotVersionDao.getLatestRobotVersion(appId);
        //        if(null == robotVersion){
        //            return AppResponse.error(ErrorCodeEnum.E_PARAM,"앱 마켓봇을 찾을 수 없습니다");
        //        }
        //        Integer appVersion = robotVersion.getVersion();

        // 가져오기마켓중의대버전
        MarketResourceDto marketResourceDto = new MarketResourceDto();
        marketResourceDto.setAppId(appId);
        marketResourceDto.setMarketId(marketId);
        AppMarketVersion maxVersionInMarket = appMarketVersionDao.getLatestAppVersionInfo(marketResourceDto);
        if (null == maxVersionInMarket || null == maxVersionInMarket.getAppVersion()) {
            return AppResponse.error(ErrorCodeEnum.E_SQL, "마켓에서 사용 중인 버전을 가져오지 못했습니다");
        }
        Integer appVersion = maxVersionInMarket.getAppVersion();
        List<RobotExecute> robotExecuteList = new ArrayList<>();
        for (String userId : userIdList) {
            RobotExecute robotExecute = new RobotExecute();
            robotExecute.setRobotId(idWorker.nextId() + "");
            robotExecute.setName(marketDto.getAppName());
            robotExecute.setCreatorId(userId);
            robotExecute.setUpdaterId(userId);
            robotExecute.setTenantId(tenantId);
            robotExecute.setAppId(appId);
            robotExecute.setAppVersion(appVersion);
            robotExecute.setMarketId(marketId);
            robotExecute.setResourceStatus(OBTAINED);
            robotExecute.setDataSource(MARKET);
            robotExecuteList.add(robotExecute);
        }
        robotExecuteDao.addRobotByDeploy(robotExecuteList);

        return AppResponse.success(true);
    }

    @Override
    public AppResponse<?> updateRobotByPush(MarketDto marketDto) {
        List<String> userIdList = marketDto.getUserIdList();
        userIdList.removeIf(Objects::isNull);
        marketDto.setUserIdList(userIdList);
        String appId = marketDto.getAppId();
        Integer appVersion = marketDto.getAppVersion();
        if (CollectionUtils.isEmpty(userIdList) || StringUtils.isBlank(appId) || appVersion == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM);
        }
        // 조회버전여부저장에서
        MarketResourceDto marketResourceDto = new MarketResourceDto();
        marketResourceDto.setAppId(appId);
        marketResourceDto.setMarketId(marketDto.getMarketId());
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        marketResourceDto.setTenantId(tenantId);
        marketResourceDto.setVersion(appVersion);
        RobotVersion robotVersion = robotVersionDao.getVersionInfo(marketResourceDto);
        if (null == robotVersion) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "해당버전봇을 찾을 수 없습니다");
        }
        // 조회여부있음사용자있음가져오기경과사용
        Set<String> obtainedUserList = robotExecuteDao.getUserListByAppId(appId);
        for (String userId : userIdList) {
            if (!obtainedUserList.contains(userId)) {
                return AppResponse.error(ErrorCodeEnum.E_SQL, "사용자있음가져오기경과사용");
            }
        }
        // 조회버전의사용이름문자
        String appName = appMarketResourceDao.getAppNameByAppId(appId);
        marketDto.setAppName(appName);
        robotExecuteDao.updateRobotByPush(marketDto);
        return AppResponse.success(true);
    }

    @Override
    public AppResponse<?> getDeployedUserList(MarketDto marketDto) throws NoLoginException {
        String appId = marketDto.getAppId();
        String marketId = marketDto.getMarketId();
        if (StringUtils.isBlank(appId) || StringUtils.isBlank(marketId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM);
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
        marketDto.setCreatorId(userId);
        marketDto.setTenantId(tenantId);
        // 조회기존robotid
        AppMarketResource appMarketResource = appMarketResourceDao.getAppInfoByAppId(marketDto);
        if (null == appMarketResource) {
            return AppResponse.error(ErrorCodeEnum.E_SQL, "사용 정보를 가져오지 못했습니다");
        }
        String robotId = appMarketResource.getRobotId();
        if (StringUtils.isBlank(robotId)) {
            return AppResponse.error(ErrorCodeEnum.E_SQL, "기존 봇 정보를 가져오지 못했습니다");
        }
        // 조회생성자정보
        RobotExecute robotExecute = robotExecuteDao.getAuthInfo(appMarketResource);
        if (null == robotExecute) {
            return AppResponse.error(ErrorCodeEnum.E_SQL, "생성자 정보를 가져오지 못했습니다");
        }
        AppResponse<String> realNameResp = rpaAuthFeign.getNameById(appMarketResource.getCreatorId());
        if (realNameResp == null || realNameResp.getData() == null) {
            throw new ServiceException("사용자명가져오기실패");
        }
        String authorName = realNameResp.getData();
        if (StringUtils.isBlank(authorName)) {
            return AppResponse.error(ErrorCodeEnum.E_SQL, "가져올 수 없는 생성자이름");
        }
        robotExecute.setName(authorName);
        robotExecute.setIsCreator(true);
        String userName = marketDto.getRealName();
        if (StringUtils.isNotBlank(userName)) {
            // 조회
            marketDto.setCreatorId(userId);
            if (authorName.toLowerCase().contains(userName.toLowerCase())) {
                // 조회, 패키지생성자
                return getResultWitchHaveAuthor(marketDto, robotExecute);
            }
            // 조회, 아니요패키지생성자
            PrePage<RobotExecute> pages = new PrePage<>(0L);
            if (null == marketDto.getPageNo() || null == marketDto.getPageSize()) {
                return AppResponse.success(pages);
            }
            // 통신경과Feign호출rpa-auth서비스가져오기완료모듈사용자목록
            GetDeployedUserListDto queryDto = new GetDeployedUserListDto();
            queryDto.setAppId(marketDto.getAppId());
            queryDto.setMarketId(marketDto.getMarketId());
            queryDto.setTenantId(marketDto.getTenantId());
            queryDto.setRealName(marketDto.getRealName());
            queryDto.setPageNo(marketDto.getPageNo());
            queryDto.setPageSize(marketDto.getPageSize());
            AppResponse<PageDto<com.iflytek.rpa.common.feign.entity.RobotExecute>> deployedUserResponse =
                    rpaAuthFeign.getDeployedUserListWithoutTenantId(queryDto);
            if (deployedUserResponse == null || !deployedUserResponse.ok()) {
                PrePage<RobotExecute> pageConfig = new PrePage<>(marketDto.getPageNo(), marketDto.getPageSize(), true);
                return AppResponse.success(pageConfig);
            }
            PageDto<com.iflytek.rpa.common.feign.entity.RobotExecute> pageDto = deployedUserResponse.getData();
            // 변환로PrePage<RobotExecute>
            pages = convertToPrePage(pageDto);
            return AppResponse.success(pages);
        }
        // 조회전체
        return getResultWitchHaveAuthor(marketDto, robotExecute);
    }

    private AppResponse<?> getResultWitchHaveAuthor(MarketDto marketDto, RobotExecute robotExecute) {
        PrePage<RobotExecute> pages = new PrePage<>(1L);
        //        if (null == marketDto.getPageNo() || null == marketDto.getPageSize()) {
        //            List<RobotExecute> oneResult = new ArrayList<>();
        //            oneResult.add(robotExecute);
        //            pages.setRecords(oneResult);
        //            pages.setTotal(1);
        ////            return AppResponse.success(pages);
        //        }
        if (null == marketDto.getPageNo() || null == marketDto.getPageSize()) {
            marketDto.setPageNo(1);
            marketDto.setPageSize(10);
        }
        // 통신경과Feign호출rpa-auth서비스가져오기완료모듈사용자목록
        GetDeployedUserListDto queryDto = new GetDeployedUserListDto();
        queryDto.setAppId(marketDto.getAppId());
        queryDto.setMarketId(marketDto.getMarketId());
        queryDto.setTenantId(marketDto.getTenantId());
        queryDto.setRealName(marketDto.getRealName());
        queryDto.setPageNo(marketDto.getPageNo());
        queryDto.setPageSize(marketDto.getPageSize());
        AppResponse<PageDto<com.iflytek.rpa.common.feign.entity.RobotExecute>> deployedUserResponse =
                rpaAuthFeign.getDeployedUserListWithoutTenantId(queryDto);
        if (deployedUserResponse == null || !deployedUserResponse.ok()) {
            PrePage<RobotExecute> pageConfig = new PrePage<>(marketDto.getPageNo(), marketDto.getPageSize(), true);
            return AppResponse.success(pageConfig);
        }
        PageDto<com.iflytek.rpa.common.feign.entity.RobotExecute> pageDto = deployedUserResponse.getData();
        // 변환로PrePage<RobotExecute>
        pages = convertToPrePage(pageDto);
        List<RobotExecute> robotExecuteList = pages.getRecords();
        robotExecuteList = new ArrayList<>(robotExecuteList);
        if (CollectionUtils.isEmpty(robotExecuteList)) {
            robotExecuteList.add(robotExecute);
        } else {
            for (RobotExecute execute : robotExecuteList) {
                execute.setCreateTime(execute.getUpdateTime());
            }
            robotExecuteList.add(0, robotExecute);
        }
        pages.setRecords(robotExecuteList);
        pages.setTotal(pages.getTotal() + 1);
        return AppResponse.success(pages);
    }

    @Override
    public AppResponse<?> getVersionListForApp(MarketDto marketDto) throws NoLoginException {
        // 버전, 변경 로그, 발송버전시간
        String appId = marketDto.getAppId();
        String marketId = marketDto.getMarketId();
        if (StringUtils.isBlank(appId) || StringUtils.isBlank(marketId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM);
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
        marketDto.setCreatorId(userId);
        marketDto.setTenantId(tenantId);
        // 가져오기마켓중의대버전
        MarketResourceDto marketResourceDto = new MarketResourceDto();
        marketResourceDto.setAppId(appId);
        marketResourceDto.setMarketId(marketId);
        AppMarketVersion maxVersionInMarket = appMarketVersionDao.getLatestAppVersionInfo(marketResourceDto);
        if (null == maxVersionInMarket || null == maxVersionInMarket.getAppVersion()) {
            return AppResponse.error(ErrorCodeEnum.E_SQL, "마켓에서 사용 중인 버전을 가져오지 못했습니다");
        }
        marketDto.setAppVersion(maxVersionInMarket.getAppVersion());
        // 출력마켓발송버전아니요업데이트까지마켓, 으로조회소대기마켓대버전의기록
        List<RobotVersion> robotVersionList = robotVersionDao.getVersionListForApp(marketDto);
        return AppResponse.success(robotVersionList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> deleteApp(String appId, String marketId) throws Exception {
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
        MarketDto marketDto = new MarketDto();
        marketDto.setTenantId(tenantId);
        marketDto.setAppId(appId);
        marketDto.setMarketId(marketId);
        AppMarketResource appResource = appMarketResourceDao.getAppInfoByAppId(marketDto);
        if (appResource == null) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EMPTY, "삭제할 봇을 찾을 수 없습니다");
        }
        // app_resource 삭제
        Integer i = appMarketResourceDao.deleteApp(appId, marketId, tenantId);
        Integer j = appMarketVersionDao.deleteAppVersion(appId, marketId);
        // 업데이트robotDesign테이블및robotExecute테이블
        RobotDesign robotDesign = robotDesignDao.getRobotRegardlessLogicDel(appResource.getRobotId(), userId, tenantId);
        if (robotDesign != null) {
            String transformStatus = robotDesign.getTransformStatus();
            transformStatus = "shared".equals(transformStatus) ? "published" : transformStatus;
            robotDesign.setUpdateTime(new Date());
            robotDesign.setTransformStatus(transformStatus);
            Integer z = robotDesignDao.updateById(robotDesign);
            if (isUpdated(i) && isUpdated(j) && isUpdated(z)) {
                return AppResponse.success("삭제봇성공");
            } else {
                throw new ServiceException(ErrorCodeEnum.E_SQL_EXCEPTION.getCode(), "삭제봇실패");
            }
        } else {
            if (isUpdated(i) && isUpdated(j)) {
                return AppResponse.success("삭제봇성공");
            } else {
                throw new ServiceException(ErrorCodeEnum.E_SQL_EXCEPTION.getCode(), "삭제봇실패");
            }
        }
    }

    private boolean isUpdated(Integer count) {
        return count != null && count > 0;
    }

    @Override
    public AppResponse<?> getALlAppList(AllAppListDto allAppListDto) throws NoLoginException {
        if (allAppListDto == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }

        Long pageNo = allAppListDto.getPageNo();
        Long pageSize = allAppListDto.getPageSize();
        String appName = allAppListDto.getAppName();
        String marketId = allAppListDto.getMarketId();
        String category = allAppListDto.getCategory();
        if (pageNo == null || pageSize == null || pageNo <= 0 || pageSize <= 0) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK, "페이지 번호와 크기가 올바르지 않습니다");
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

        IPage<AppMarketResource> page = new Page<>(pageNo, pageSize);
        // 마켓선택
        if (StringUtils.isBlank(marketId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        // 정렬방식
        HashSet<String> set = Sets.newHashSet("createTime", "downloadNum", "checkNum");
        String sortKey = allAppListDto.getSortKey();
        sortKey = StringUtils.isBlank(sortKey) ? "createTime" : sortKey; // 로createTime 순서
        if (StringUtils.isNotBlank(sortKey) && !set.contains(sortKey)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK, "지원하지 않는 정렬 기준입니다");
        }
        // 분
        Page<AppMarketResource> rePage = appMarketResourceDao.pageAllAppList(
                page, marketId, allAppListDto.getCreatorId(), appName, category, sortKey);

        if (CollectionUtils.isEmpty(rePage.getRecords())) return AppResponse.success(rePage);

        // 까지결과
        IPage<AppInfoVo> ansPage = getAppListAnsPage(rePage, userId, tenantId, pageNo, pageSize, marketId);

        return AppResponse.success(ansPage);
    }

    @Override
    public AppResponse<?> appUpdateCheck(AppUpdateCheckDto queryDto) throws NoLoginException {

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

        String marketId = queryDto.getMarketId();
        String appIdListStr = queryDto.getAppIdListStr();

        if (StringUtils.isBlank(appIdListStr)) return AppResponse.error(ErrorCodeEnum.E_PARAM);

        // appIdList
        List<String> appIdList = Arrays.stream(appIdListStr.split(",")).collect(Collectors.toList());

        // 가져오기
        List<RobotExecute> robotExecuteList =
                robotExecuteDao.getExecuteByAppIdList(userId, tenantId, marketId, appIdList);
        List<RobotDesign> robotDesignList = robotDesignDao.getDesignByAppIdList(userId, tenantId, marketId, appIdList);

        List<AppUpdateCheckVo> appUpdateCheckVos = new ArrayList<>();

        for (String appId : appIdList) {
            AppUpdateCheckVo appUpdateCheckVo = new AppUpdateCheckVo();
            appUpdateCheckVo.setAppId(appId);
            appUpdateCheckVo.setUpdateStatus(0);

            List<RobotExecute> robotExecuteListTmp = robotExecuteList.stream()
                    .filter(robotExecute -> robotExecute.getAppId().equals(appId))
                    .collect(Collectors.toList());

            // 여부안내업데이트
            // 있음중일개가져오기경과
            if (!CollectionUtils.isEmpty(robotExecuteListTmp)) {
                RobotExecute robotExecute = robotExecuteListTmp.get(0);
                if (robotExecute.getResourceStatus().equals("toUpdate")) appUpdateCheckVo.setUpdateStatus(1);
                else appUpdateCheckVo.setUpdateStatus(0);
            }

            appUpdateCheckVos.add(appUpdateCheckVo);
        }

        return AppResponse.success(appUpdateCheckVos);
    }

    @Override
    public AppResponse<?> appDetail(String appId, String marketId) throws Exception {

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

        if (StringUtils.isBlank(appId) || StringUtils.isBlank(marketId))
            return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK);

        AppDetailVo appDetailVo = new AppDetailVo();

        AppMarketResource appResource = appMarketResourceDao.getAppResource(appId, marketId);
        AppMarketVersion latestAppVersion = appMarketVersionDao.getLatestAppVersion(appId, marketId);

        if (appResource == null || latestAppVersion == null) return AppResponse.error(ErrorCodeEnum.E_SQL_EMPTY);

        // 조회데이터목록추가일
        Long checkNum = appResource.getCheckNum();
        appResource.setCheckNum(checkNum == null ? 1L : checkNum + 1L);
        appMarketResourceDao.updateById(appResource);

        setAppDetailVo(appDetailVo, appResource, latestAppVersion, userId, tenantId);

        return AppResponse.success(appDetailVo);
    }

    private void setAppDetailVo(
            AppDetailVo appDetailVo,
            AppMarketResource appResource,
            AppMarketVersion latestAppVersion,
            String userId,
            String tenantId) {

        String appId = appResource.getAppId();
        String marketId = appResource.getMarketId();

        String robotId = appResource.getRobotId();
        Integer appVersionNum = latestAppVersion.getAppVersion();

        RobotVersion robotVersion = robotVersionDao.getOnlineVersionRegardlessDel(robotId);
        List<RobotVersion> robotVersionList = robotVersionDao.getAllVersionWithoutUser(robotId);
        if (robotVersion == null) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "봇 버전 정보를 찾을 수 없습니다");
        }
        String fileName = robotVersionDao.getFileName(robotVersion.getAppendixId());

        AppResponse<String> realNameResp = rpaAuthFeign.getNameById(robotVersion.getCreatorId());
        if (realNameResp == null || realNameResp.getData() == null) {
            throw new ServiceException("사용자명가져오기실패");
        }
        String creatorName = realNameResp.getData();
        RobotExecute robotExecute = robotExecuteDao.getExecuteByAppId(userId, tenantId, marketId, appId);
        RobotDesign robotDesign = robotDesignDao.getDesignByAppId(userId, tenantId, marketId, appId);

        appDetailVo.setIconUrl(robotVersion.getIcon());
        appDetailVo.setAppName(appResource.getAppName());
        appDetailVo.setIntroduction(robotVersion.getIntroduction());
        appDetailVo.setVideoPath(
                StringUtils.isBlank(robotVersion.getVideoId()) ? "" : filePathPrefix + robotVersion.getVideoId());

        // 본정보
        appDetailVo.setDownloadNum(appResource.getDownloadNum());
        appDetailVo.setCheckNum(appResource.getCheckNum());
        appDetailVo.setCreatorName(creatorName);
        appDetailVo.setCategory(latestAppVersion.getCategory());
        appDetailVo.setFileName(StringUtils.isBlank(fileName) ? "" : fileName);
        appDetailVo.setFilePath(
                StringUtils.isBlank(robotVersion.getAppendixId()) ? "" : filePathPrefix + robotVersion.getAppendixId());
        appDetailVo.setUseDescription(robotVersion.getUseDescription());

        // 버전정보 
        List<AppDetailVersionInfo> appDetailVersionInfoList = new ArrayList<>();

        if (CollectionUtils.isEmpty(robotVersionList)) {
            robotVersionList = Collections.singletonList(robotVersion);
        }

        for (RobotVersion rVersion : robotVersionList) {

            AppDetailVersionInfo appDetailVersionInfo = new AppDetailVersionInfo();

            appDetailVersionInfo.setUpdateLog(rVersion.getUpdateLog());
            appDetailVersionInfo.setVersionNum(rVersion.getVersion());
            appDetailVersionInfo.setCreateTime(rVersion.getCreateTime());
            appDetailVersionInfo.setOnline(rVersion.getOnline());
            appDetailVersionInfoList.add(appDetailVersionInfo);
        }

        appDetailVo.setVersionInfoList(appDetailVersionInfoList);
    }

    private IPage<AppInfoVo> getAppListAnsPage(
            IPage<AppMarketResource> rePage,
            String userId,
            String tenantId,
            Long pageNo,
            Long pageSize,
            String marketId) {

        IPage<AppInfoVo> ansPage = new Page<>(pageNo, pageSize);
        List<AppInfoVo> ansRecords = new ArrayList<>();

        List<AppMarketResource> appResourceList = rePage.getRecords();
        List<String> appIdList =
                appResourceList.stream().map(AppMarketResource::getAppId).collect(Collectors.toList());

        // 가져오기모든의appId대의version및의robotId
        List<ResVerDto> resVerDtoList = appMarketVersionDao.getResVerJoin(marketId, appIdList);

        // 근거 버전  및  robotId 조회  위검토기록 닫기정보
        appApplicationService.packageApplicationInfo(appResourceList, resVerDtoList, userId);

        // 가져오기에서마켓중의역할
        Integer allowOperate = 0; // 아니요허용
        String userType = appMarketUserDao.getMarketUserType(marketId, userId, tenantId);
        allowOperate = (userType != null && userType.equals("acquirer")) ? 0 : 1;

        // 가져오기robotExecuteList
        List<RobotExecute> robotExecuteList =
                robotExecuteDao.getExecuteByAppIdList(userId, tenantId, marketId, appIdList);
        List<RobotDesign> robotDesignList = robotDesignDao.getDesignByAppIdList(userId, tenantId, marketId, appIdList);

        for (AppMarketResource record : appResourceList) {
            String appId = record.getAppId();
            AppInfoVo appInfoVo = new AppInfoVo();

            List<ResVerDto> resVerDtos = resVerDtoList.stream()
                    .filter(resVerDto -> resVerDto.getAppId().equals(appId))
                    .collect(Collectors.toList());

            String intro = "";
            String iconUrl = "";
            if (!CollectionUtils.isEmpty(resVerDtos)) {
                ResVerDto resVerDto = resVerDtos.get(0);
                intro = resVerDto.getIntroduction();
                iconUrl = resVerDto.getIconUrl();
            }

            // 가져오기및업데이트상태
            setObtainUpdateStatus(appId, appInfoVo, robotExecuteList, robotDesignList);
            appInfoVo.setAppName(record.getAppName());
            appInfoVo.setCheckNum(record.getCheckNum());
            appInfoVo.setDownloadNum(record.getDownloadNum());
            appInfoVo.setAppIntro(intro);
            appInfoVo.setAllowOperate(allowOperate);
            appInfoVo.setAppId(appId);
            appInfoVo.setMarketId(marketId);
            appInfoVo.setIconUrl(iconUrl);

            appInfoVo.setSecurityLevel(record.getSecurity_level());
            appInfoVo.setExpiryDate(record.getExpiry_date());
            appInfoVo.setExpiryDateStr(record.getExpiry_date_str());

            ansRecords.add(appInfoVo);
        }

        ansPage.setRecords(ansRecords);
        ansPage.setSize(rePage.getSize());
        ansPage.setTotal(rePage.getTotal());

        return ansPage;
    }

    private void setObtainUpdateStatus(
            String appId, AppInfoVo appInfoVo, List<RobotExecute> robotExecuteList, List<RobotDesign> robotDesignList) {

        // 필터링
        List<RobotDesign> robotDesignListTmp = robotDesignList.stream()
                .filter(robotDesign -> robotDesign.getAppId().equals(appId))
                .collect(Collectors.toList());

        List<RobotExecute> robotExecuteListTmp = robotExecuteList.stream()
                .filter(robotExecute -> robotExecute.getAppId().equals(appId))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(robotExecuteListTmp) && CollectionUtils.isEmpty(robotDesignListTmp)) {
            // 있음가져오기경과
            appInfoVo.setObtainStatus(0); // 가져오기
            appInfoVo.setUpdateStatus(0); // 아니요안내업데이트
            return;
        } else { // 설명있음가져오기 기록
            appInfoVo.setObtainStatus(1); // 다시 가져오기상태
        }

        if (CollectionUtils.isEmpty(robotExecuteListTmp)) appInfoVo.setUpdateStatus(0);
        else {
            RobotExecute robotExecute = robotExecuteListTmp.get(0);
            if (robotExecute.getResourceStatus().equals("toUpdate")
                    && StringUtils.isNotBlank(robotExecute.getResourceStatus())) appInfoVo.setUpdateStatus(1);
            else appInfoVo.setUpdateStatus(0);
        }
    }

    /**
     * 를PageDto<RobotExecute>변환로PrePage<RobotExecute>
     */
    private PrePage<RobotExecute> convertToPrePage(PageDto<com.iflytek.rpa.common.feign.entity.RobotExecute> pageDto) {
        PrePage<RobotExecute> prePage = new PrePage<>(pageDto.getCurrentPageNo(), pageDto.getPageSize(), true);
        prePage.setTotal(pageDto.getTotalCount());

        List<RobotExecute> robotExecuteList = new ArrayList<>();
        if (pageDto.getResult() != null) {
            for (com.iflytek.rpa.common.feign.entity.RobotExecute feignRobotExecute : pageDto.getResult()) {
                RobotExecute robotExecute = new RobotExecute();
                robotExecute.setId(feignRobotExecute.getId());
                robotExecute.setCreatorId(feignRobotExecute.getCreatorId());
                robotExecute.setName(feignRobotExecute.getName());
                robotExecute.setUpdateTime(feignRobotExecute.getUpdateTime());
                robotExecute.setAppVersion(feignRobotExecute.getAppVersion());
                robotExecuteList.add(robotExecute);
            }
        }
        prePage.setRecords(robotExecuteList);
        return prePage;
    }
}
