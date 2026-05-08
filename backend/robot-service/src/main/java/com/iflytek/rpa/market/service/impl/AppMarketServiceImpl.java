package com.iflytek.rpa.market.service.impl;

import static com.iflytek.rpa.robot.constants.RobotConstant.OBTAINED;

import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.TenantExpirationDto;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.market.dao.AppMarketDao;
import com.iflytek.rpa.market.dao.AppMarketDictDao;
import com.iflytek.rpa.market.dao.AppMarketUserDao;
import com.iflytek.rpa.market.entity.AppMarket;
import com.iflytek.rpa.market.entity.AppMarketDo;
import com.iflytek.rpa.market.entity.AppMarketUser;
import com.iflytek.rpa.market.service.AppMarketService;
import com.iflytek.rpa.quota.service.QuotaCheckService;
import com.iflytek.rpa.robot.dao.RobotExecuteDao;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * 팀마켓-팀테이블(AppMarket)테이블서비스유형
 *
 * @author makejava
 * @since 2024-01-19 14:41:34
 */
@Service("appMarketService")
public class AppMarketServiceImpl implements AppMarketService {
    @Resource
    private AppMarketDao appMarketDao;

    @Autowired
    private AppMarketDictDao appMarketDictDao;

    @Autowired
    private AppMarketUserDao appMarketUserDao;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private RobotExecuteDao robotExecuteDao;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private QuotaCheckService quotaCheckService;

    @Value("${market.maxCreateCount:3}")
    private Integer maxCreateCount;

    @Override
    public AppResponse getAppType() {

        return AppResponse.success(appMarketDictDao.getAppType());
    }

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    public AppResponse getListForPublish() throws NoLoginException {
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
        List<AppMarket> joinedMarketList = appMarketDao.getJoinedMarketList(userId);
        List<AppMarket> createdMarketList = appMarketDao.getCreatedMarketList(tenantId, userId);
        AppMarketDo appMarketDo = new AppMarketDo();
        appMarketDo.setJoinedMarketList(joinedMarketList);
        appMarketDo.setCreatedMarketList(createdMarketList);
        appMarketDo.setNoMarket(
                CollectionUtils.isEmpty(joinedMarketList) && CollectionUtils.isEmpty(createdMarketList));
        return AppResponse.success(appMarketDo);
    }

    @Override
    public AppResponse getMarketList() throws NoLoginException {
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

        List<AppMarket> marketList = appMarketDao.getMarketList(tenantId, userId);

        return AppResponse.success(marketList);
    }

    public AppResponse<Integer> marketNumCheck() throws NoLoginException {
        AppResponse<TenantExpirationDto> resp = rpaAuthFeign.getExpiration();
        if (resp == null || !resp.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        TenantExpirationDto data = resp.getData();
        String tenantId = data.getTenantId();
        String tenantType = data.getTenantType();
        // 개사람버전  팀마켓생성데이터제한제어
        if (tenantType.equals("personal")) {
            Integer marketCount = appMarketDao.getMarketCount(tenantId);
            if (marketCount > maxCreateCount) {
                return AppResponse.success(0);
            }
        }
        return AppResponse.success(1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse addMarket(AppMarket appMarket) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        AppResponse<TenantExpirationDto> resp = rpaAuthFeign.getExpiration();
        if (resp == null || !resp.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        TenantExpirationDto data = resp.getData();
        String tenantId = data.getTenantId();

        String marketName = appMarket.getMarketName();
        marketName = marketName.trim();
        appMarket.setMarketName(marketName);
        if (StringUtils.isBlank(marketName)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "마켓이름비워 둘 수 없습니다");
        }

        // 검증마켓추가입력수매칭금액
        if (!quotaCheckService.checkMarketJoinQuota()) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "완료추가입력의마켓수완료위제한, 불가생성변경다중팀마켓");
        }

        appMarket.setCreatorId(userId);
        appMarket.setUpdaterId(userId);
        Integer marketCount = appMarketDao.getMarketNameByName(tenantId, appMarket.getMarketName());
        if (marketCount > 0) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "해당테넌트메모리에서이름마켓, 요청다시 명령이름");
        }
        // 제품marketId
        String marketId = idWorker.nextId() + "";
        appMarket.setMarketId(marketId);
        appMarket.setTenantId(tenantId);
        appMarketDao.addMarket(appMarket);
        // 추가구성원
        AppMarketUser appMarketUser = new AppMarketUser();
        appMarketUser.setMarketId(marketId);
        appMarketUser.setCreatorId(userId);
        appMarketUser.setUpdaterId(userId);
        appMarketUser.setTenantId(tenantId);
        appMarketUserDao.addDefaultUser(appMarketUser);
        return AppResponse.success(true);
    }

    @Override
    public AppResponse getMarketInfo(String marketId) throws NoLoginException {
        if (null == marketId) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        AppMarket appMarket = appMarketDao.getMarketInfo(marketId);
        if (null == appMarket || null == appMarket.getCreatorId()) {
            return AppResponse.error(ErrorCodeEnum.E_SQL);
        }

        AppResponse<String> realNameResp = rpaAuthFeign.getNameById(appMarket.getCreatorId());
        if (realNameResp == null || realNameResp.getData() == null) {
            throw new ServiceException("사용자명가져오기실패");
        }
        String userName = realNameResp.getData();

        appMarket.setUserName(userName);

        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        // 가져오기역할
        String userType = appMarketUserDao.getUserTypeForCheck(userId, marketId);
        appMarket.setUserType(userType);

        // 결과가예공유마켓 아니요조회생성자
        if (appMarket.getMarketType().equals("public")) {
            appMarket.setUserName("시스템생성");
            // 테넌트생성 시간

            // UapTenant uapTenant = tenantDao.getTenantById(databaseName,tenantId);
            Date createTime = new Date();
            appMarket.setCreateTime(createTime);
            appMarket.setMarketDescribe("해당마켓로공유마켓");
        }
        return AppResponse.success(appMarket);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse editTeamMarket(AppMarket appMarket) throws NoLoginException {
        if (null == appMarket) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        String marketId = appMarket.getMarketId();
        if (null == marketId) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
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

        // marketName 아니요비어 있습니다시, 재이름
        if (StringUtils.isNotBlank(appMarket.getMarketName())) {
            boolean b = isMarketNameRepeat(appMarket.getMarketName(), appMarket.getMarketId(), userId, tenantId);
            if (b) return AppResponse.error("팀마켓이름재복사, 요청수정");
        }

        appMarket.setUpdaterId(userId);
        appMarket.setTenantId(tenantId);
        appMarketDao.updateTeamMarket(appMarket);
        return AppResponse.success(true);
    }

    private boolean isMarketNameRepeat(String marketName, String marketId, String userId, String tenantId) {
        List<AppMarket> marketList = appMarketDao.getTenantMarketList(tenantId);
        List<AppMarket> marketListAfterFilter = marketList.stream()
                .filter(appMarket -> (appMarket.getMarketName().equals(marketName)
                        && !appMarket.getMarketId().equals(marketId)))
                .collect(Collectors.toList());

        return !CollectionUtils.isEmpty(marketListAfterFilter);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse leaveTeamMarket(AppMarket appMarket) throws NoLoginException {

        if (null == appMarket || null == appMarket.getMarketId()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        AppResponse<User> resp = rpaAuthFeign.getLoginUser();
        if (resp == null || !resp.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = resp.getData();
        String userId = loginUser.getId();

        String oldOwnerId = userId;
        appMarket.setCreatorId(oldOwnerId);

        // 결과가예모든, 열기의시있음모든권한,  직선연결오류
        String userType = appMarketUserDao.getUserType(appMarket.getMarketId(), userId);
        if (userType.equals("owner") && StringUtils.isBlank(appMarket.getNewOwner())) {
            return AppResponse.error("완료로팀모든, 완료로새로고침페이지");
        }

        if (StringUtils.isNotBlank(appMarket.getNewOwner())) {

            AppResponse<User> userResp = rpaAuthFeign.getUserInfoByPhone(appMarket.getNewOwner());
            if (userResp == null || userResp.getData() == null) {
                throw new ServiceException("가져오기사용자 정보 조회 실패");
            }
            User user = userResp.getData();
            String newOwnerId = Optional.ofNullable(user).map(User::getId).orElse(null);
            if (null == newOwnerId) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "새 팀 구성원을 찾을 수 없습니다");
            }
            // 업데이트팀테이블
            appMarketDao.updateTeamMarketOwner(appMarket.getMarketId(), newOwnerId);
            // 업데이트팀사람원테이블
            appMarketUserDao.updateToOwner(appMarket.getMarketId(), newOwnerId);
        }
        // 열기팀마켓
        appMarketUserDao.leaveTeamMarket(appMarket);
        // 에서마켓중가져오기경과사용, 를대기업데이트사용의상태로완료가져오기
        robotExecuteDao.updateResourceStatusByMarketId(OBTAINED, appMarket.getCreatorId(), appMarket.getMarketId());
        return AppResponse.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse dissolveTeamMarket(AppMarket appMarket) {

        if (null == appMarket || null == appMarket.getMarketId()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        String marketName = appMarketDao.getMarketNameById(appMarket.getMarketId());
        if (StringUtils.isBlank(marketName)) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "팀 마켓을 찾을 수 없습니다");
        }
        if (!marketName.equals(appMarket.getMarketName())) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "팀 마켓 이름이 올바르지 않습니다");
        }
        // 삭제마켓,삭제닫기 의사용, 삭제닫기 의구성원
        appMarketDao.deleteMarket(appMarket.getMarketId());
        appMarketUserDao.deleteAllUser(appMarket.getMarketId());

        //        appMarketResourceDao.deleteResource(appMarket.getMarketId());
        //        appMarketVersionDao.deletVersion(appMarket.getMarketId());

        return AppResponse.success(true);
    }
}