package com.iflytek.rpa.market.service.impl;

import static com.iflytek.rpa.robot.constants.RobotConstant.OBTAINED;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.common.feign.entity.dto.GetMarketTenantUserListDto;
import com.iflytek.rpa.common.feign.entity.dto.GetMarketUserByPhoneDto;
import com.iflytek.rpa.common.feign.entity.dto.GetMarketUserByPhoneForOwnerDto;
import com.iflytek.rpa.common.feign.entity.dto.GetMarketUserListByPublicDto;
import com.iflytek.rpa.common.feign.entity.dto.GetMarketUserListDto;
import com.iflytek.rpa.common.feign.entity.dto.GetUserUnDeployedDto;
import com.iflytek.rpa.common.feign.entity.dto.PageDto;
import com.iflytek.rpa.market.dao.AppMarketDao;
import com.iflytek.rpa.market.dao.AppMarketUserDao;
import com.iflytek.rpa.market.entity.AppMarket;
import com.iflytek.rpa.market.entity.AppMarketUser;
import com.iflytek.rpa.market.entity.MarketDto;
import com.iflytek.rpa.market.entity.TenantUser;
import com.iflytek.rpa.market.service.AppMarketUserService;
import com.iflytek.rpa.notify.entity.dto.CreateNotifyDto;
import com.iflytek.rpa.notify.service.NotifySendService;
import com.iflytek.rpa.robot.dao.RobotExecuteDao;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * 팀마켓-사람원테이블, n:n의닫기시스템(AppMarketUser)테이블서비스유형
 *
 * @author makejava
 * @since 2024-01-19 14:41:35
 */
@Service("appMarketUserService")
public class AppMarketUserServiceImpl extends ServiceImpl<AppMarketUserDao, AppMarketUser>
        implements AppMarketUserService {
    @Resource
    private AppMarketUserDao appMarketUserDao;

    @Autowired
    private AppMarketDao appMarketDao;

    @Autowired
    private NotifySendService notifySendService;

    @Autowired
    private RobotExecuteDao robotExecuteDao;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    public AppResponse<?> getUserUnDeployed(MarketDto marketDto) throws NoLoginException {
        // 가져오기 모듈의계정
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();

        if (StringUtils.isNotBlank(marketDto.getPhone())
                && !marketDto.getPhone().matches("[0-9]{0,10}")) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "입력하세요합치기법휴대폰 번호");
        }
        if (null == marketDto.getMarketId() || null == marketDto.getAppId()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "적음사용정보");
        }

        // 통신경과Feign호출rpa-auth서비스가져오기미완료모듈사용자목록
        GetUserUnDeployedDto queryDto = new GetUserUnDeployedDto();
        queryDto.setMarketId(marketDto.getMarketId());
        queryDto.setAppId(marketDto.getAppId());
        queryDto.setTenantId(tenantId);
        queryDto.setPhone(marketDto.getPhone());

        AppResponse<List<com.iflytek.rpa.common.feign.entity.MarketDto>> response =
                rpaAuthFeign.getUserUnDeployed(queryDto);

        List<MarketDto> userList = new ArrayList<>();
        if (response == null || !response.ok()) {
            return AppResponse.success(userList);
        }
        List<com.iflytek.rpa.common.feign.entity.MarketDto> feignUserList = response.getData();

        // 변환로본MarketDto
        if (feignUserList != null) {
            for (com.iflytek.rpa.common.feign.entity.MarketDto feignDto : feignUserList) {
                MarketDto localDto = new MarketDto();
                localDto.setPhone(feignDto.getPhone());
                localDto.setRealName(feignDto.getRealName());
                localDto.setCreatorId(feignDto.getCreatorId());
                userList.add(localDto);
            }
        }
        return AppResponse.success(userList);
    }

    @Override
    public AppResponse getUserList(MarketDto marketDto) throws NoLoginException {
        String marketId = marketDto.getMarketId();
        IPage<MarketDto> userListPage = new Page<>();
        if (null == marketId || null == marketDto.getPageNo() || null == marketDto.getPageSize()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        IPage<MarketDto> pageConfig = new Page<>(marketDto.getPageNo(), marketDto.getPageSize(), true);
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
        String nowUserid = loginUser.getId();

        AppMarket appMarket = appMarketDao.getMarketInfo(marketId);
        if (!appMarket.getMarketType().equals("public")) {
            // 통신경과Feign호출rpa-auth서비스가져오기마켓사용자목록
            GetMarketUserListDto queryDto = new GetMarketUserListDto();
            queryDto.setMarketId(marketDto.getMarketId());
            queryDto.setTenantId(tenantId);
            queryDto.setUserName(marketDto.getUserName());
            queryDto.setRealName(marketDto.getRealName());
            queryDto.setSortBy(marketDto.getSortBy());
            queryDto.setSortType(marketDto.getSortType());
            queryDto.setPageNo(marketDto.getPageNo());
            queryDto.setPageSize(marketDto.getPageSize());

            AppResponse<PageDto<com.iflytek.rpa.common.feign.entity.MarketDto>> feignResponse =
                    rpaAuthFeign.getMarketUserList(queryDto);
            if (feignResponse == null || !feignResponse.ok()) {
                userListPage = new Page<>(marketDto.getPageNo(), marketDto.getPageSize(), true);
            } else {
                PageDto<com.iflytek.rpa.common.feign.entity.MarketDto> pageDto = feignResponse.getData();
                // 변환로IPage
                userListPage = new Page<>(pageDto.getCurrentPageNo(), pageDto.getPageSize(), pageDto.getTotalCount());
                List<MarketDto> records = new ArrayList<>();
                if (pageDto.getResult() != null) {
                    for (com.iflytek.rpa.common.feign.entity.MarketDto feignDto : pageDto.getResult()) {
                        MarketDto localDto = new MarketDto();
                        localDto.setId(feignDto.getId());
                        localDto.setUserType(feignDto.getUserType());
                        localDto.setCreatorId(feignDto.getCreatorId());
                        localDto.setCreateTime(feignDto.getCreateTime());
                        localDto.setUserName(feignDto.getUserName());
                        localDto.setRealName(feignDto.getRealName());
                        localDto.setEmail(feignDto.getEmail());
                        localDto.setPhone(feignDto.getPhone());
                        records.add(localDto);
                    }
                }
                userListPage.setRecords(records);
            }
        } else {
            // 통신경과Feign호출rpa-auth서비스가져오기 공유마켓사용자목록
            GetMarketUserListByPublicDto queryDto = new GetMarketUserListByPublicDto();
            queryDto.setMarketId(marketDto.getMarketId());
            queryDto.setTenantId(tenantId);
            queryDto.setNowUserid(nowUserid);
            queryDto.setUserName(marketDto.getUserName());
            queryDto.setRealName(marketDto.getRealName());
            queryDto.setSortBy(marketDto.getSortBy());
            queryDto.setSortType(marketDto.getSortType());
            queryDto.setPageNo(marketDto.getPageNo());
            queryDto.setPageSize(marketDto.getPageSize());

            AppResponse<PageDto<com.iflytek.rpa.common.feign.entity.MarketDto>> feignResponse =
                    rpaAuthFeign.getMarketUserListByPublic(queryDto);
            if (feignResponse == null || !feignResponse.ok()) {
                userListPage = new Page<>(marketDto.getPageNo(), marketDto.getPageSize(), true);
            } else {
                PageDto<com.iflytek.rpa.common.feign.entity.MarketDto> pageDto = feignResponse.getData();
                // 변환로IPage
                userListPage = new Page<>(pageDto.getCurrentPageNo(), pageDto.getPageSize(), pageDto.getTotalCount());
                List<MarketDto> records = new ArrayList<>();
                if (pageDto.getResult() != null) {
                    for (com.iflytek.rpa.common.feign.entity.MarketDto feignDto : pageDto.getResult()) {
                        MarketDto localDto = new MarketDto();
                        localDto.setId(feignDto.getId());
                        localDto.setUserType(feignDto.getUserType());
                        localDto.setCreatorId(feignDto.getCreatorId());
                        localDto.setCreateTime(feignDto.getCreateTime());
                        localDto.setUserName(feignDto.getUserName());
                        localDto.setRealName(feignDto.getRealName());
                        localDto.setEmail(feignDto.getEmail());
                        localDto.setPhone(feignDto.getPhone());
                        records.add(localDto);
                    }
                }
                userListPage.setRecords(records);
            }
        }
        if (CollectionUtils.isEmpty(userListPage.getRecords())) {
            return AppResponse.success(userListPage);
        }
        // 결과가예공유마켓 아니요조회생성자
        if (!appMarket.getMarketType().equals("public")) {
            // 가져오기마켓생성자
            String creatorId = appMarketDao.getCreator(marketId);
            if (null != creatorId) {
                for (MarketDto userInfo : userListPage.getRecords()) {
                    if (creatorId.equals(userInfo.getCreatorId())) {
                        userListPage.getRecords().remove(userInfo);
                        userInfo.setIsCreator(true);
                        userListPage.getRecords().add(0, userInfo);
                        break;
                    }
                }
            }
        }
        return AppResponse.success(userListPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse deleteUser(MarketDto marketDto) throws NoLoginException {
        String marketId = marketDto.getMarketId();
        if (null == marketId || null == marketDto.getCreatorId()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        String ownerId = appMarketUserDao.getOwnerByRole(marketDto.getMarketId());
        if (marketDto.getCreatorId().equals(ownerId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "할 수 없음출력생성자");
        }

        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String nowUserId = loginUser.getId();
        if (marketDto.getCreatorId().equals(nowUserId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "할 수 없음출력");
        }
        String id = appMarketUserDao.getIdByMarketIdAndCreatorId(marketId, marketDto.getCreatorId());
        if (StringUtils.isNotBlank(id)) {
            Integer i = appMarketUserDao.deleteById(id);
            if (i > 0) {
                // 에서마켓중가져오기경과사용, 를대기업데이트사용의상태로완료가져오기, 본사람, 마켓id
                robotExecuteDao.updateResourceStatusByMarketId(OBTAINED, marketDto.getCreatorId(), marketId);
                return AppResponse.success(true);
            }
        }
        return AppResponse.success(false);
    }

    @Override
    public AppResponse roleSet(MarketDto marketDto) throws NoLoginException {
        String marketId = marketDto.getMarketId();
        if (null == marketId || null == marketDto.getCreatorId() || null == marketDto.getUserType()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        if (userId.equals(marketDto.getCreatorId())) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE_NOT_SUPPORT, "불가변경수정의역할");
        }
        // 결과가아니요에서해당마켓, 이면권한이 없습니다
        if (!isExistsInMarket(userId, marketId)) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE_NOT_SUPPORT);
        }
        String ownerId = appMarketUserDao.getOwnerByRole(marketId);
        if (marketDto.getUserType().equals("owner") && !marketDto.getCreatorId().equals(ownerId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK, "마켓가능있음일개있음");
        }
        if (!marketDto.getUserType().equals("owner") && marketDto.getCreatorId().equals(ownerId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK, "생성자역할할 수 없음변경수정");
        }
        Integer result = appMarketUserDao.roleSet(marketDto);
        if (result > 0) {
            return AppResponse.success(true);
        }
        return AppResponse.success(false);
    }

    private Boolean isExistsInMarket(String userId, String marketId) {
        AppMarketUser appMarketUser = new AppMarketUser();
        appMarketUser.setMarketId(marketId);
        appMarketUser.setCreatorId(userId);
        appMarketUser.setDeleted(0);
        long count = appMarketUserDao.count(appMarketUser);
        return count > 0;
    }

    @Override
    public AppResponse getUserByPhone(GetMarketUserByPhoneDto marketDto) {
        String marketId = marketDto.getMarketId();
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();

        // 통신경과Feign호출rpa-auth서비스근거휴대폰 번호조회마켓사용자
        marketDto.setTenantId(tenantId);

        AppResponse<List<com.iflytek.rpa.common.feign.entity.MarketDto>> feignResponse =
                rpaAuthFeign.getMarketUserByPhone(marketDto);

        List<MarketDto> userList = new ArrayList<>();
        if (feignResponse == null || !feignResponse.ok()) {
            return AppResponse.success(userList);
        }

        List<com.iflytek.rpa.common.feign.entity.MarketDto> feignUserList = feignResponse.getData();

        // 변환로본MarketDto
        if (feignUserList != null) {
            for (com.iflytek.rpa.common.feign.entity.MarketDto feignDto : feignUserList) {
                MarketDto localDto = new MarketDto();
                localDto.setPhone(feignDto.getPhone());
                localDto.setRealName(feignDto.getRealName());
                localDto.setCreatorId(feignDto.getCreatorId());
                userList.add(localDto);
            }
        }

        if (CollectionUtils.isEmpty(userList)) return AppResponse.success(new ArrayList<MarketDto>());

        // 필터링완료에서마켓중의사용자
        List<MarketDto> userListAfterFilter = filterUserAlreadyInMarket(userList, marketId, tenantId);

        return AppResponse.success(userListAfterFilter);
    }

    @Override
    public AppResponse getUserByPhoneForOwner(MarketDto marketDto) throws NoLoginException {
        String marketId = marketDto.getMarketId();
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

        // 통신경과Feign호출rpa-auth서비스근거휴대폰 번호조회마켓중의사용자(정렬제거)
        GetMarketUserByPhoneForOwnerDto queryDto = new GetMarketUserByPhoneForOwnerDto();
        queryDto.setMarketId(marketId);
        queryDto.setTenantId(tenantId);
        queryDto.setPhone(marketDto.getPhone());
        queryDto.setUserId(userId);

        AppResponse<List<com.iflytek.rpa.common.feign.entity.MarketDto>> feignResponse =
                rpaAuthFeign.getMarketUserByPhoneForOwner(queryDto);

        List<MarketDto> userList = new ArrayList<>();
        if (feignResponse == null || !feignResponse.ok()) {
            return AppResponse.success(userList);
        }

        List<com.iflytek.rpa.common.feign.entity.MarketDto> feignUserList = feignResponse.getData();

        // 변환로본MarketDto
        if (feignUserList != null) {
            for (com.iflytek.rpa.common.feign.entity.MarketDto feignDto : feignUserList) {
                MarketDto localDto = new MarketDto();
                localDto.setPhone(feignDto.getPhone());
                localDto.setRealName(feignDto.getRealName());
                localDto.setCreatorId(feignDto.getCreatorId());
                userList.add(localDto);
            }
        }

        return AppResponse.success(userList);
    }

    private List<MarketDto> filterUser4Leave(
            List<MarketDto> userList, String marketId, String tenantId, String userId) {
        List<String> marketUserIdList = appMarketUserDao.getAllUserId(tenantId, marketId);

        // 에서마켓내부의userList
        List<MarketDto> userListInMarket = userList.stream()
                .filter(marketDto -> marketUserIdList.contains(marketDto.getCreatorId()))
                .collect(Collectors.toList());

        // 정렬제거
        List<MarketDto> userListFinal = userListInMarket.stream()
                .filter(marketDto1 -> !(marketDto1.getCreatorId().equals(userId)))
                .collect(Collectors.toList());

        return userListFinal;
    }

    private List<MarketDto> filterUserAlreadyInMarket(List<MarketDto> userList, String marketId, String tenantId) {

        List<String> userIdList = userList.stream().map(MarketDto::getCreatorId).collect(Collectors.toList());

        List<String> marketUserIdInList = appMarketUserDao.getMarketUserInList(marketId, userIdList, tenantId);

        List<MarketDto> userListAfterFilter = userList.stream()
                .filter(marketDto -> !marketUserIdInList.contains(marketDto.getCreatorId()))
                .collect(Collectors.toList());

        return userListAfterFilter;
    }

    @Override
    public AppResponse inviteUser(MarketDto marketDto) throws NoLoginException {
        String marketId = marketDto.getMarketId();
        List<AppMarketUser> userInfoList = marketDto.getUserInfoList();
        if (CollectionUtils.isEmpty(userInfoList)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        // 초대의사람여부에서본테넌트내부
        // 통신경과Feign호출rpa-auth서비스조회테넌트사용자목록
        List<String> userIdList = userInfoList.stream()
                .map(AppMarketUser::getCreatorId)
                .filter(creatorId -> creatorId != null)
                .collect(Collectors.toList());

        GetMarketTenantUserListDto queryDto = new GetMarketTenantUserListDto();
        queryDto.setTenantId(tenantId);
        queryDto.setUserIdList(userIdList);

        AppResponse<List<com.iflytek.rpa.common.feign.entity.TenantUser>> feignResponse =
                rpaAuthFeign.getMarketTenantUserList(queryDto);

        List<TenantUser> tenantUserList = new ArrayList<>();
        if (feignResponse != null && feignResponse.ok() && feignResponse.getData() != null) {
            for (com.iflytek.rpa.common.feign.entity.TenantUser feignTenantUser : feignResponse.getData()) {
                TenantUser localTenantUser = new TenantUser();
                localTenantUser.setTenantId(feignTenantUser.getTenantId());
                localTenantUser.setUserId(feignTenantUser.getUserId());
                tenantUserList.add(localTenantUser);
            }
        }

        // 근거userId분그룹
        Map<String, String> userMap =
                tenantUserList.stream().collect(Collectors.toMap(TenantUser::getUserId, TenantUser::getTenantId));

        for (AppMarketUser userInfo : userInfoList) {
            if (null == userInfo) {
                continue;
            }
            if (null == userMap.get(userInfo.getCreatorId())) {
                return AppResponse.error(ErrorCodeEnum.E_SQL, "초대완료찾을 수 없습니다해당테넌트의사용자");
            }
        }

        // 제품사람메시지, 를marketId,role삽입메시지테이블
        CreateNotifyDto createNotifyDto = new CreateNotifyDto();
        createNotifyDto.setMarketUserList(userInfoList);
        createNotifyDto.setTenantId(tenantId);
        createNotifyDto.setMessageType("teamMarketInvite");
        createNotifyDto.setMarketId(marketId);
        notifySendService.createNotify(createNotifyDto);
        return AppResponse.success(true);
    }
}