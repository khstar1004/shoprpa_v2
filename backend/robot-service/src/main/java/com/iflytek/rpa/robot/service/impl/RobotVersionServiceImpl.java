package com.iflytek.rpa.robot.service.impl;

import static com.iflytek.rpa.robot.constants.RobotConstant.*;
import static com.iflytek.rpa.utils.DeBounceUtils.deBounce;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.base.dao.*;
import com.iflytek.rpa.base.entity.*;
import com.iflytek.rpa.base.service.CElementService;
import com.iflytek.rpa.base.service.CParamService;
import com.iflytek.rpa.base.service.handler.ExecutorModeHandler;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.component.dao.ComponentRobotBlockDao;
import com.iflytek.rpa.component.dao.ComponentRobotUseDao;
import com.iflytek.rpa.component.entity.ComponentRobotBlock;
import com.iflytek.rpa.component.entity.ComponentRobotUse;
import com.iflytek.rpa.example.service.SampleUsersService;
import com.iflytek.rpa.market.dao.AppMarketDao;
import com.iflytek.rpa.market.dao.AppMarketResourceDao;
import com.iflytek.rpa.market.dao.AppMarketVersionDao;
import com.iflytek.rpa.market.entity.AppMarketResource;
import com.iflytek.rpa.market.entity.AppMarketUser;
import com.iflytek.rpa.market.entity.AppMarketVersion;
import com.iflytek.rpa.market.entity.dto.MarketResourceDto;
import com.iflytek.rpa.market.entity.vo.AppMarketUserVo;
import com.iflytek.rpa.market.service.AppApplicationService;
import com.iflytek.rpa.notify.entity.dto.CreateNotifyDto;
import com.iflytek.rpa.notify.service.NotifySendService;
import com.iflytek.rpa.robot.dao.RobotDesignDao;
import com.iflytek.rpa.robot.dao.RobotExecuteDao;
import com.iflytek.rpa.robot.dao.RobotVersionDao;
import com.iflytek.rpa.robot.entity.File;
import com.iflytek.rpa.robot.entity.RobotDesign;
import com.iflytek.rpa.robot.entity.RobotExecute;
import com.iflytek.rpa.robot.entity.RobotVersion;
import com.iflytek.rpa.robot.entity.dto.EnableVersionDto;
import com.iflytek.rpa.robot.entity.dto.RobotVersionDto;
import com.iflytek.rpa.robot.entity.dto.VersionListDto;
import com.iflytek.rpa.robot.entity.vo.VersionDetailVo;
import com.iflytek.rpa.robot.entity.vo.VersionInfo;
import com.iflytek.rpa.robot.entity.vo.VersionListVo;
import com.iflytek.rpa.robot.service.RobotVersionService;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * 단말봇버전테이블(RobotVersion)테이블서비스유형
 *
 * @author makejava
 * @since 2024-09-29 15:27:42
 */
@Service("robotVersionService")
@Slf4j
public class RobotVersionServiceImpl extends ServiceImpl<RobotVersionDao, RobotVersion> implements RobotVersionService {
    @Resource
    NotifySendService notifySendService;

    @Resource
    private RobotVersionDao robotVersionDao;

    @Resource
    private RobotDesignDao robotDesignDao;

    @Autowired
    private RobotExecuteDao robotExecuteDao;

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
    private CModuleDao moduleDao;

    @Autowired
    private CSmartComponentDao smartComponentDao;

    @Autowired
    private AppMarketDao appMarketDao;

    @Autowired
    private SampleUsersService sampleUsersService;

    @Autowired
    private AppMarketResourceDao appMarketResourceDao;

    @Autowired
    private IdWorker idWorker;

    @Resource
    private CElementService cElementService;

    @Autowired
    private AppMarketVersionDao appMarketVersionDao;

    @Autowired
    private AppApplicationService appApplicationService;

    @Resource
    private CParamDao cParamDao;

    @Autowired
    private CParamService paramService;

    @Autowired
    private ComponentRobotUseDao componentRobotUseDao;

    @Autowired
    private ComponentRobotBlockDao componentRobotBlockDao;

    @Autowired
    private RobotVersionService robotVersionService;

    @Autowired
    private ExecutorModeHandler executorModeHandler;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Value("${deBounce.prefix}")
    private String doBouncePrefix;

    @Value("${deBounce.window}")
    private Long deBounceWindow;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public AppResponse<?> publishRobot(RobotVersionDto robotVersionDto) throws Exception {
        // 클릭발송버전의시, 프론트엔드조정연결완료모든데이터저장까지v0완료
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
        robotVersionDto.setCreatorId(userId);
        robotVersionDto.setUpdaterId(userId);
        robotVersionDto.setTenantId(tenantId);
        String name = robotVersionDto.getName();
        // 근거해당필드, 프론트엔드아니요의성공안내
        String haveShared = CREATE;
        Integer enableLastVersion = robotVersionDto.getEnableLastVersion();
        // 조회버전여부정상
        Integer nextVersion = robotVersionDto.getVersion();
        if (null == nextVersion || nextVersion <= 0) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "버전오류");
        }
        Integer latestVersion = robotVersionDao.getLatestVersionNum(robotVersionDto);
        if (null != latestVersion && latestVersion >= nextVersion) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "버전오류");
        }
        RobotExecute robotExecute = new RobotExecute();
        // 조회Robot의유형 입력 web  예 other유형 version로 0 의 content
        BeanUtils.copyProperties(robotVersionDto, robotExecute);
        // packageWebType(robotExecute);
        robotExecute.setDataSource(CREATE);
        AppResponse<String> currentLevelCodeRes = rpaAuthFeign.getCurrentLevelCode();
        if (!currentLevelCodeRes.ok()) throw new ServiceException("rpa-auth 서비스가 준비되지 않았습니다");
        String deptIdPath = currentLevelCodeRes.getData();
        robotExecute.setDeptIdPath(deptIdPath);

        // 닫기
        String createCompVerKey = doBouncePrefix + robotVersionDto.getRobotId() + robotVersionDto.getVersion() + userId;
        deBounce(createCompVerKey, deBounceWindow);

        Integer updateCount = null;
        // 일게시
        if (1 == nextVersion) {
            // 를상태수정로완료게시
            updateCount = robotDesignDao.updateTransformStatus(userId, robotVersionDto.getRobotId(), name, PUBLISHED);
            // 삽입실행기기테이블
            robotExecuteDao.insertRobot(robotExecute);
            // 삽입버전테이블, 사용
            robotVersionDto.setOnline(1);
            robotVersionDao.addRobotVersion(robotVersionDto);
        } else {
            // 일게시
            // 업데이트실행기기테이블
            robotExecuteDao.updateRobot(robotExecute);
            // 업데이트app_market_resource앱 마켓중의이름문자
            Integer appCount = appMarketResourceDao.selectAppInfo(robotExecute);
            // 결과가공유경과또는출력마켓완료, 상태예완료발송버전; 공유경과, 완료위
            if (null != appCount && appCount > 0) {
                // 공유경과마켓, 1)업데이트이름문자, 2)업데이트가져오기 resource_status로toUpdate 3)삽입새버전까지app_market_version
                // 결과가열기시작위검토  이면를입력매개으로JSON의방식저장아래
                AppResponse<String> auditStatus = appApplicationService.getAuditStatus();
                if (auditStatus.ok() && auditStatus.getData().equals("off")) { // 결과가있음, 위검토있음열기시작
                    haveShared = "market";
                    updateAppAndRobot(robotExecute, robotVersionDto.getVersion());
                } else {
                    // 열기시작예완료발송버전
                    updateCount =
                            robotDesignDao.updateTransformStatus(userId, robotVersionDto.getRobotId(), name, PUBLISHED);
                }
            } else {
                updateCount =
                        robotDesignDao.updateTransformStatus(userId, robotVersionDto.getRobotId(), name, PUBLISHED);
            }
            // 삽입버전테이블, 사용할 수 없습니다
            robotVersionDto.setOnline(0);
            // paramDetail필드행삭제
            robotVersionDao.addRobotVersion(robotVersionDto);
        }
        createDataForNewVersion(robotVersionDto);

        // 사용새버전
        if (enableLastVersion != null && enableLastVersion == 1) {
            EnableVersionDto enableVersionDto = new EnableVersionDto();
            enableVersionDto.setRobotId(robotVersionDto.getRobotId());
            enableVersionDto.setVersion(robotVersionDto.getVersion());
            robotVersionService.enableVersion(enableVersionDto);
        }

        // 예외실행, 요청 openApi
        sampleUsersService.sendOpenApi(robotVersionDto.getRobotId(), nextVersion, userId, tenantId);

        return AppResponse.success(haveShared);
    }

    public void createDataForNewVersion(RobotVersionDto robotVersionDto) {
        // 생성새버전의프로세스대기데이터
        processDao.createProcessForCurrentVersion(robotVersionDto);
        // 요소그룹데이터
        groupDao.createGroupForCurrentVersion(robotVersionDto);
        // 원데이터
        elementDao.createElementForCurrentVersion(robotVersionDto);
        // 전역 변수데이터
        globalVarDao.createGlobalVarForCurrentVersion(robotVersionDto);
        // python데이터
        requireDao.createRequireForCurrentVersion(robotVersionDto);
        // python모듈 module데이터
        moduleDao.createModuleForCurrentVersion(robotVersionDto);
        // 가능컴포넌트
        smartComponentDao.createSmartComponentForCurrentVersion(robotVersionDto);
        // 프로세스매개변수
        paramService.createParamForCurrentVersion(null, robotVersionDto, 0);
        // 컴포넌트사용데이터
        createCompRobotUse4NewVer(robotVersionDto);
        // 컴포넌트데이터
        createCompRobotBlock4NewVer(robotVersionDto);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void updateAppAndRobot(RobotExecute robotExecute, Integer nextVersion) throws NoLoginException {
        robotDesignDao.updateTransformStatus(
                robotExecute.getCreatorId(), robotExecute.getRobotId(), robotExecute.getName(), SHARED);

        // 업데이트앱 마켓의 사용이름
        appMarketResourceDao.updateAppName(robotExecute);

        // 가져오기닫기 의있음출력의모든마켓사용정보
        List<AppMarketResource> appInfoList =
                appMarketResourceDao.getAppInfoByRobotId(robotExecute.getRobotId(), robotExecute.getCreatorId());
        if (!CollectionUtils.isEmpty(appInfoList)) {
            // 1, 없음업데이트직선연결발송버전 2, 후발송버전
            // 조회새버전
            AppMarketResource appMarketResourceAnyOne = appInfoList.get(0);
            MarketResourceDto marketResourceDto = new MarketResourceDto();
            marketResourceDto.setMarketId(appMarketResourceAnyOne.getMarketId());
            marketResourceDto.setAppId(appMarketResourceAnyOne.getAppId());
            AppMarketVersion latestAppVersion = appMarketVersionDao.getLatestAppVersionInfo(marketResourceDto);
            // 복사사용새버전의열기 코드, 행분유형대기정보, 추가버전
            latestAppVersion.setAppVersion(nextVersion);
            latestAppVersion.setCreateTime(new Date());
            latestAppVersion.setUpdateTime(new Date());
            // app_market_version중삽입새버전
            appMarketVersionDao.insertAppVersionBatch(latestAppVersion, appInfoList);
            // 가져오기공유경과의마켓및사용id
            List<String> marketIdList =
                    appInfoList.stream().map(AppMarketResource::getMarketId).collect(Collectors.toList());
            List<String> appIdList =
                    appInfoList.stream().map(AppMarketResource::getAppId).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(marketIdList) || CollectionUtils.isEmpty(appIdList)) {
                return;
            }
            // 가져오기 및가져오기 에서의마켓정보 사람원정보
            List<AppMarketUserVo> marketUserVoList =
                    robotExecuteDao.getObtainerIdList(marketIdList, appIdList, robotExecute.getCreatorId());
            // 근거appId분그룹
            Map<String, List<AppMarketUserVo>> marketUserMap =
                    marketUserVoList.stream().collect(Collectors.groupingBy(AppMarketUserVo::getAppId));
            if (CollectionUtils.isEmpty(marketUserVoList)) {
                return;
            }
            // 업데이트가져오기 resource_status로toUpdate
            // 또는가져오기 있음출력마켓, 발송버전, 가져오기 까지업데이트
            robotExecuteDao.updateResourceStatusByAppIdList(TO_UPDATE, appIdList, marketUserVoList);
            for (AppMarketResource appInfo : appInfoList) {
                CreateNotifyDto createNotifyDto = new CreateNotifyDto();
                List<AppMarketUserVo> marketUserVoListForAppId = marketUserMap.get(appInfo.getAppId());
                List<AppMarketUser> marketUserList = new ArrayList<>();
                if (CollectionUtils.isEmpty(marketUserVoListForAppId)) {
                    continue;
                }
                for (AppMarketUserVo marketUserVo : marketUserVoListForAppId) {
                    AppMarketUser appMarketUser = new AppMarketUser();
                    BeanUtils.copyProperties(marketUserVo, appMarketUser);
                    marketUserList.add(appMarketUser);
                }
                createNotifyDto.setMarketUserList(marketUserList);
                createNotifyDto.setTenantId(robotExecute.getTenantId());
                createNotifyDto.setMessageType("teamMarketUpdate");
                createNotifyDto.setMarketId(appInfo.getMarketId());
                createNotifyDto.setAppId(appInfo.getAppId());
                notifySendService.createNotify(createNotifyDto);
            }
        }
    }

    /**
     * 계획기기아니요허용재이름, 실행기기및마켓허용재이름
     *
     * @param robotVersionDto
     * @return
     * @throws NoLoginException
     */
    @Override
    public AppResponse<?> checkSameName(RobotVersionDto robotVersionDto) throws NoLoginException {
        String name = robotVersionDto.getName();
        String robotId = robotVersionDto.getRobotId();
        if (StringUtils.isBlank(name) || StringUtils.isBlank(robotId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM);
        }

        AppResponse<User> resp = rpaAuthFeign.getLoginUser();
        if (resp == null || !resp.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = resp.getData();
        String userId = loginUser.getId();

        robotVersionDto.setCreatorId(userId);
        AppResponse<String> res = rpaAuthFeign.getTenantId();
        if (res == null || res.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = res.getData();
        robotVersionDto.setTenantId(tenantId);
        Integer countDesign = robotDesignDao.countByName(robotVersionDto);
        //        Integer countExecute = robotExecuteDao.countByName(robotVersionDto);
        if (countDesign > 0) {
            return AppResponse.success(true);
        }
        return AppResponse.success(false);
    }

    @Override
    public AppResponse<?> getLastRobotVersionInfo(RobotVersion robotVersionSearch) throws NoLoginException {
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
        String robotId = robotVersionSearch.getRobotId();
        if (StringUtils.isBlank(robotId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "봇id비워 둘 수 없습니다");
        }
        RobotVersionDto robotVersionDto = new RobotVersionDto();
        // 조회계획기기이름문자
        RobotDesign robotDesign = robotDesignDao.getRobotDesignInfo(robotId, userId, tenantId);
        if (null == robotDesign) {
            return AppResponse.error(ErrorCodeEnum.E_SQL, "봇을 찾을 수 없습니다");
        }
        RobotVersion robotVersion = robotVersionDao.getLastRobotVersionInfo(robotId, userId, tenantId);
        if (null != robotVersion) {
            // 까지버전
            Integer version = robotVersion.getVersion();
            if (version == null) {
                return AppResponse.error(ErrorCodeEnum.E_SQL, "없음버전");
            }
            Integer nextVersion = version + 1;
            robotVersion.setVersion(nextVersion);
            BeanUtils.copyProperties(robotVersion, robotVersionDto);
            String videoId = robotVersion.getVideoId();
            String appendixId = robotVersion.getAppendixId();
            // 가져오기이름
            List<String> fileIdList = new ArrayList<>();
            fileIdList.add(videoId);
            fileIdList.add(appendixId);
            List<File> fileInfoList = robotVersionDao.getFileNameInfo(fileIdList);
            Map<String, String> fileInfoMap =
                    fileInfoList.stream().collect(Collectors.toMap(File::getFileId, File::getFileName));
            robotVersionDto.setVideoName(fileInfoMap.get(videoId));
            robotVersionDto.setAppendixName(fileInfoMap.get(appendixId));
            robotVersionDto.setName(robotDesign.getName());
        } else {
            robotVersionDto.setName(robotDesign.getName());
            robotVersionDto.setVersion(1);
        }
        return AppResponse.success(robotVersionDto);
    }

    @Override
    public AppResponse<?> versionList(VersionListDto queryDto) throws NoLoginException {

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
        String robotId = queryDto.getRobotId();
        Integer sortType = queryDto.getSortType() == null ? 1 : queryDto.getSortType();

        String marketId = robotDesignDao.getMarketId(robotId, userId, tenantId);

        IPage<RobotVersion> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<RobotVersion> wrapper = new LambdaQueryWrapper<>();

        // userID tenantId 선택
        wrapper.eq(RobotVersion::getCreatorId, userId);
        wrapper.eq(RobotVersion::getTenantId, tenantId);

        wrapper.eq(RobotVersion::getRobotId, robotId);

        // 수정 시간정렬
        if (sortType.equals(0)) wrapper.orderByAsc(RobotVersion::getVersion);
        else wrapper.orderByDesc(RobotVersion::getVersion);

        String sourceName = "";
        IPage<RobotVersion> rePage = this.page(page, wrapper);

        if (rePage.getRecords().size() == 0) {
            sourceName = StringUtils.isBlank(marketId) ? "본" : "팀마켓";

            IPage<VersionInfo> ansPage = new Page<>(pageNo, pageSize);

            VersionListVo resVo = new VersionListVo();
            resVo.setSourceName(sourceName);
            resVo.setAnsPage(ansPage);

            return AppResponse.success(resVo);
        }

        IPage<VersionInfo> ansPage = getVersionInfoPage(rePage, pageNo, pageSize);

        sourceName = StringUtils.isBlank(marketId) ? "본" : "팀마켓";

        // 결과그룹설치
        VersionListVo resVo = new VersionListVo();
        resVo.setSourceName(sourceName);
        resVo.setAnsPage(ansPage);

        return AppResponse.success(resVo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public AppResponse<?> enableVersion(EnableVersionDto queryDto) throws Exception {

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

        String robotId = queryDto.getRobotId();
        Integer version = queryDto.getVersion();

        if (StringUtils.isBlank(robotId) || version == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK, "요청 매개변수 실패");
        }

        // 빈robotExecute테이블의매개변수매칭,사용사용버전의구성 매개변수
        robotExecuteDao.updateParamToNUll(robotId, userId, tenantId);
        // 를사용버전아래
        robotVersionDao.unEnableAllVersion(robotId, userId, tenantId);

        // 위지정버전
        boolean b = robotVersionDao.enableVersion(robotId, version, userId, tenantId);
        if (b) return AppResponse.success("위새버전성공");
        else throw new ServiceException("지정한 버전을 활성화하지 못했습니다");
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public AppResponse<?> recoverVersion(EnableVersionDto queryDto) throws Exception {

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

        String robotId = queryDto.getRobotId();
        Integer version = queryDto.getVersion();

        if (StringUtils.isBlank(robotId) || version == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK, "요청 매개변수 실패");
        }

        // 복사지정버전, 에서 c_element, c_global_var, c_process, c_require , c_module
        recover(robotId, version, userId, tenantId);

        String massage = "";
        massage = "복사버전" + version + "성공!";

        return AppResponse.success(massage);
    }

    public void recover(String robotId, Integer version, String userId, String tenantId) throws Exception {
        elementRecover(robotId, version, userId);
        globalVarRecover(robotId, version, userId);
        processRecover(robotId, version, userId);
        requireRecover(robotId, version, userId);
        moduleRecover(robotId, version, userId);
        smartComponentRecover(robotId, version, userId);
        // 복사매개변수
        paramRecover(robotId, version, userId);
        // 복사컴포넌트사용데이터
        recoverComponentUse(robotId, version, userId);
        // 복사컴포넌트데이터
        recoverComponentBlock(robotId, version, userId);

        // 계획기기상태변경수정로중
        RobotDesign robotDesign = robotDesignDao.getRobot(robotId, userId, tenantId);
        robotDesign.setTransformStatus("editing");
        robotDesignDao.updateById(robotDesign);
    }

    // element 복사
    public void elementRecover(String robotId, Integer version, String userId) throws Exception {
        elementDao.deleteOldEditVersion(robotId, userId);
        List<CElement> elementList = elementDao.getElement(robotId, version, userId);
        if (CollectionUtils.isEmpty(elementList)) return;

        for (CElement element : elementList) {

            element.setId(null);
            element.setRobotVersion(0);
            element.setCreateTime(new Date());
            element.setUpdateTime(new Date());
        }

        // 후량삽입
        elementDao.insertEleBatch(elementList);
    }

    public void globalVarRecover(String robotId, Integer version, String userId) {
        globalVarDao.deleteOldEditVersion(robotId, userId);
        List<CGlobalVar> globalVarList = globalVarDao.getGlobalVar(robotId, version, userId);
        if (CollectionUtils.isEmpty(globalVarList)) return;

        for (CGlobalVar globalVar : globalVarList) {

            globalVar.setId(null);
            globalVar.setRobotVersion(0);
            globalVar.setCreateTime(new Date());
            globalVar.setUpdateTime(new Date());
        }

        globalVarDao.insertGloBatch(globalVarList);
    }

    public void processRecover(String robotId, Integer version, String userId) throws Exception {
        processDao.deleteOldEditVersion(robotId, userId);

        List<CProcess> processList = processDao.getProcess(robotId, version, userId);
        if (CollectionUtils.isEmpty(processList)) {
            throw new ServiceException("복구할 버전의 프로세스를 찾을 수 없습니다");
        }

        for (CProcess process : processList) {

            process.setId(null);
            process.setRobotVersion(0);
            process.setCreateTime(new Date());
            process.setUpdateTime(new Date());
        }

        processDao.insertProcessBatch(processList);
    }

    public void moduleRecover(String robotId, Integer version, String userId) throws Exception {
        moduleDao.deleteOldEditVersion(robotId, userId);

        List<CModule> moduleList = moduleDao.getAllModuleList(robotId, version, userId);
        if (CollectionUtils.isEmpty(moduleList)) {
            return;
        }

        for (CModule module : moduleList) {

            module.setId(null);
            module.setRobotVersion(0);
            module.setCreateTime(new Date());
            module.setUpdateTime(new Date());
        }

        moduleDao.insertBatch(moduleList);
    }

    public void smartComponentRecover(String robotId, Integer version, String userId) throws Exception {
        smartComponentDao.deleteOldEditVersion(robotId, userId);

        List<CSmartComponent> smartComponentList = smartComponentDao.getAllSmartComponentList(robotId, version, userId);
        if (CollectionUtils.isEmpty(smartComponentList)) {
            return;
        }

        for (CSmartComponent smartComponent : smartComponentList) {
            smartComponent.setRobotVersion(0);
            smartComponent.setCreatorId(userId);
            smartComponent.setUpdaterId(userId);
        }

        smartComponentDao.insertBatch(smartComponentList);
    }

    public void requireRecover(String robotId, Integer version, String userId) {
        requireDao.deleteOldEditVersion(robotId, userId);
        List<CRequire> requireList = requireDao.getRequire(robotId, version, userId);
        if (CollectionUtils.isEmpty(requireList)) return;

        for (CRequire require : requireList) {

            require.setId(null);
            require.setRobotVersion(0);
            require.setCreateTime(new Date());
            require.setUpdateTime(new Date());
        }

        requireDao.insertReqBatch(requireList);
    }

    // 매개변수 복사
    public void paramRecover(String robotId, Integer version, String userId) {
        // 삭제0버전의매개변수
        cParamDao.deleteParamByRobotId(robotId);
        // 조회현재버전매개변수
        List<CParam> cParamList = cParamDao.getAllParams(null, robotId, version);
        for (CParam cParam : cParamList) {
            cParam.setUpdaterId(userId);
            cParam.setId(idWorker.nextId() + "");
        }
        // 량삽입버전매개변수
        cParamList.removeIf(Objects::isNull);
        if (!cParamList.isEmpty()) {
            cParamDao.createParamForCurrentVersion(cParamList);
        }
    }

    @Override
    public AppResponse<?> list4Design(String robotId) throws NoLoginException {

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

        if (StringUtils.isBlank(robotId)) return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK, "매개변수 실패");

        RobotDesign robotDesign = robotDesignDao.getRobot(robotId, userId, tenantId);
        List<RobotVersion> robotVersionList = robotVersionDao.getAllVersion(robotId, userId, tenantId);

        List<VersionDetailVo> ansVersionList = getAnsVersionList(robotVersionList, robotId, robotDesign);

        return AppResponse.success(ansVersionList);
    }

    private List<VersionDetailVo> getAnsVersionList(
            List<RobotVersion> robotVersionList, String robotId, RobotDesign robotDesign) {

        List<VersionDetailVo> ansVoList = new ArrayList<>();

        for (RobotVersion robotVersion : robotVersionList) {
            VersionDetailVo tempVo = new VersionDetailVo();

            String onlineStr = "";
            onlineStr = robotVersion.getOnline().equals(0) ? "disable" : "enable";

            tempVo.setUpdateLog(robotVersion.getUpdateLog());
            tempVo.setRobotId(robotId);
            tempVo.setVersionNum(robotVersion.getVersion());
            tempVo.setOnline(onlineStr);
            tempVo.setUpdateTime(robotVersion.getUpdateTime());

            ansVoList.add(tempVo);
        }

        // 추가입력의버전
        if (robotDesign.getTransformStatus().equals("editing")) {
            VersionDetailVo tempVo = new VersionDetailVo();

            tempVo.setVersionNum(0);
            tempVo.setRobotId(robotId);
            tempVo.setUpdateLog(null);
            tempVo.setOnline("disable");
            tempVo.setUpdateTime(robotDesign.getUpdateTime());

            ansVoList.add(0, tempVo);
        }

        return ansVoList;
    }

    private IPage<VersionInfo> getVersionInfoPage(IPage<RobotVersion> rePage, Long pageNo, Long pageSize) {

        List<RobotVersion> robotVersionList = rePage.getRecords();

        IPage<VersionInfo> ansPage = new Page<>(pageNo, pageSize);
        List<VersionInfo> ansRecords = new ArrayList<>();

        for (int i = 0; i < robotVersionList.size(); i++) {
            VersionInfo versionInfo = new VersionInfo();

            RobotVersion robotVersionTmp = robotVersionList.get(i);

            versionInfo.setVersionNum(robotVersionTmp.getVersion());
            versionInfo.setCreateTime(robotVersionTmp.getCreateTime());
            versionInfo.setOnline(robotVersionTmp.getOnline());

            ansRecords.add(versionInfo);
        }

        ansPage.setSize(rePage.getSize());
        ansPage.setTotal(rePage.getTotal());
        ansPage.setRecords(ansRecords);

        return ansPage;
    }

    /**
     * 로새버전생성컴포넌트사용데이터
     *
     * @param robotVersionDto 봇버전정보
     */
    private void createCompRobotUse4NewVer(RobotVersionDto robotVersionDto) {
        String robotId = robotVersionDto.getRobotId();
        String creatorId = robotVersionDto.getCreatorId();
        Integer newVersion = robotVersionDto.getVersion();
        String tenantId = robotVersionDto.getTenantId();

        // 근거robotId, robotVersion=0및creatorId조회모든의componentRobotUseList기록
        List<ComponentRobotUse> componentRobotUseList =
                componentRobotUseDao.getByRobotIdAndVersion(robotId, 0, tenantId);
        if (CollectionUtils.isEmpty(componentRobotUseList)) return;

        List<ComponentRobotUse> newComponentRobotUseList = new ArrayList<>();

        for (ComponentRobotUse componentRobotUse : componentRobotUseList) {
            // 2. 생성새의기록, robotVersion로새버전
            ComponentRobotUse newComponentRobotUse = new ComponentRobotUse();
            newComponentRobotUse.setRobotId(componentRobotUse.getRobotId());
            newComponentRobotUse.setRobotVersion(newVersion);
            newComponentRobotUse.setComponentId(componentRobotUse.getComponentId());
            newComponentRobotUse.setComponentVersion(componentRobotUse.getComponentVersion());
            newComponentRobotUse.setCreatorId(creatorId);
            newComponentRobotUse.setCreateTime(new Date());
            newComponentRobotUse.setUpdaterId(creatorId);
            newComponentRobotUse.setUpdateTime(new Date());
            newComponentRobotUse.setDeleted(0);
            newComponentRobotUse.setTenantId(tenantId);

            newComponentRobotUseList.add(newComponentRobotUse);
        }

        // 량삽입새기록
        componentRobotUseDao.insertBatch(newComponentRobotUseList);
    }

    /**
     * 로새버전생성컴포넌트데이터
     *
     * @param robotVersionDto 봇버전정보
     */
    private void createCompRobotBlock4NewVer(RobotVersionDto robotVersionDto) {
        String robotId = robotVersionDto.getRobotId();
        String creatorId = robotVersionDto.getCreatorId();
        Integer newVersion = robotVersionDto.getVersion();
        String tenantId = robotVersionDto.getTenantId();

        // 1. 근거robotId, robotVersion=0및creatorId조회모든의componentRobotBlockList기록
        List<ComponentRobotBlock> componentRobotBlockList =
                componentRobotBlockDao.getBlocksByRobotId(robotId, tenantId);

        if (CollectionUtils.isEmpty(componentRobotBlockList)) return;

        List<ComponentRobotBlock> resList = new ArrayList<>();

        for (ComponentRobotBlock componentRobotBlock : componentRobotBlockList) {
            // 관리버전로0의기록
            if (componentRobotBlock.getRobotVersion() != null && componentRobotBlock.getRobotVersion() == 0) {
                // 2. 생성새의기록, robotVersion로새버전
                ComponentRobotBlock newCompRobotBlock = new ComponentRobotBlock();
                newCompRobotBlock.setRobotId(componentRobotBlock.getRobotId());
                newCompRobotBlock.setRobotVersion(newVersion);
                newCompRobotBlock.setComponentId(componentRobotBlock.getComponentId());
                newCompRobotBlock.setCreatorId(creatorId);
                newCompRobotBlock.setCreateTime(new Date());
                newCompRobotBlock.setUpdaterId(creatorId);
                newCompRobotBlock.setUpdateTime(new Date());
                newCompRobotBlock.setDeleted(0);
                newCompRobotBlock.setTenantId(tenantId);

                resList.add(newCompRobotBlock);
            }
        }

        // 량삽입새기록
        componentRobotBlockDao.insertBatch(resList);
    }

    /**
     * 복사컴포넌트사용데이터
     *
     * @param robotId 봇ID
     * @param version 버전
     * @param userId  사용자ID
     */
    public void recoverComponentUse(String robotId, Integer version, String userId) {
        // 삭제전의기록
        componentRobotUseDao.deleteOldEditVersion(robotId, userId);

        // 조회지정버전의컴포넌트사용기록
        List<ComponentRobotUse> componentRobotUseList =
                componentRobotUseDao.getComponentRobotUse(robotId, version, userId);
        if (CollectionUtils.isEmpty(componentRobotUseList)) return;

        // 관리매기록: id로null, version수정로0, 수정 시간
        for (ComponentRobotUse componentRobotUse : componentRobotUseList) {
            componentRobotUse.setId(null);
            componentRobotUse.setRobotVersion(0);
            componentRobotUse.setCreateTime(new Date());
            componentRobotUse.setUpdateTime(new Date());
        }

        // 량삽입새기록
        componentRobotUseDao.insertBatch(componentRobotUseList);
    }

    /**
     * 복사컴포넌트데이터
     *
     * @param robotId 봇ID
     * @param version 버전
     * @param userId  사용자ID
     */
    public void recoverComponentBlock(String robotId, Integer version, String userId) {
        // 삭제전의기록
        componentRobotBlockDao.deleteOldEditVersion(robotId, userId);

        // 조회지정버전의컴포넌트기록
        List<ComponentRobotBlock> componentRobotBlockList =
                componentRobotBlockDao.getComponentRobotBlock(robotId, version, userId);
        if (CollectionUtils.isEmpty(componentRobotBlockList)) return;

        // 관리매기록: id로null, version수정로0, 수정 시간
        for (ComponentRobotBlock componentRobotBlock : componentRobotBlockList) {
            componentRobotBlock.setId(null);
            componentRobotBlock.setRobotVersion(0);
            componentRobotBlock.setCreateTime(new Date());
            componentRobotBlock.setUpdateTime(new Date());
        }

        // 량삽입새기록
        componentRobotBlockDao.insertBatch(componentRobotBlockList);
    }
}
