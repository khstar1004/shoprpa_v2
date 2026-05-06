package com.iflytek.rpa.market.service.impl;

import static com.iflytek.rpa.utils.DeBounceUtils.deBounce;

import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.TenantExpirationDto;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.market.dao.AppMarketDao;
import com.iflytek.rpa.market.dao.AppMarketInviteDao;
import com.iflytek.rpa.market.dao.AppMarketUserDao;
import com.iflytek.rpa.market.entity.AppMarketInvite;
import com.iflytek.rpa.market.entity.AppMarketUser;
import com.iflytek.rpa.market.entity.dto.InviteLinkDto;
import com.iflytek.rpa.market.entity.enums.ExpireTypeEnum;
import com.iflytek.rpa.market.entity.vo.AcceptResultVo;
import com.iflytek.rpa.market.entity.vo.InviteInfoVo;
import com.iflytek.rpa.market.entity.vo.InviteLinkVo;
import com.iflytek.rpa.market.service.AppMarketInviteService;
import com.iflytek.rpa.quota.service.QuotaCheckService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import com.iflytek.rpa.utils.response.QuotaCodeEnum;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팀마켓-SaaS
 */
@Service("appMarketInviteService")
public class AppMarketInviteServiceImpl implements AppMarketInviteService {

    @Autowired
    private AppMarketInviteDao appMarketInviteDao;

    @Autowired
    private AppMarketDao appMarketDao;

    @Autowired
    private AppMarketUserDao appMarketUserDao;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Autowired
    private AppMarketInviteServiceImpl self;

    @Autowired
    private QuotaCheckService quotaCheckService;

    @Value("${market.invite.maxJoinCount:10}")
    private Integer maxJoinCount;

    @Value("${deBounce.prefix}")
    private String doBouncePrefix;

    @Value("${deBounce.window}")
    private Long deBounceWindow;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<InviteLinkVo> generateInviteLink(InviteLinkDto inviteLinkDto) throws NoLoginException {
        // 매개변수검증
        if (inviteLinkDto == null || StringUtils.isBlank(inviteLinkDto.getMarketId())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "마켓ID비워 둘 수 없습니다");
        }
        if (StringUtils.isBlank(inviteLinkDto.getExpireType())) {
            inviteLinkDto.setExpireType(ExpireTypeEnum.TWENTY_FOUR_HOURS.getCode());
        }
        // 가져오기현재로그인사용자 정보
        AppResponse<User> userResponse = rpaAuthFeign.getLoginUser();
        if (userResponse == null || !userResponse.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = userResponse.getData();
        String userId = loginUser.getId();

        AppResponse<TenantExpirationDto> resp = rpaAuthFeign.getExpiration();
        if (resp == null || !resp.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        TenantExpirationDto data = resp.getData();
        String tenantType = data.getTenantType();

        // 조회사용자권한: 있음admin가능행연결공유
        String userType = appMarketUserDao.getUserTypeForCheck(userId, inviteLinkDto.getMarketId());
        if (userType == null) {
            return AppResponse.error(ErrorCodeEnum.E_SQL, "마켓에서 해당 구성원을 찾을 수 없습니다");
        }
        if (!"admin".equals(userType) && !"owner".equals(userType)) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE_POWER_LIMIT, "권한이 없습니다행연결공유");
        }
        // 조회여부완료저장에서초대연결
        AppMarketInvite existingInvite =
                appMarketInviteDao.selectByMarketIdAndInviterId(inviteLinkDto.getMarketId(), userId);
        Date now = new Date();

        if (existingInvite != null) {
            // 경과완료새의초대연결
            if (existingInvite.getExpireTime() != null
                    && existingInvite.getExpireTime().before(now)) {
                AppMarketInvite newInvite = new AppMarketInvite();
                BeanUtils.copyProperties(existingInvite, newInvite);
                existingInvite.setDeleted(1);
                appMarketInviteDao.cancelById(existingInvite.getId());
                String inviteKey = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                newInvite.setInviteKey(inviteKey);
                newInvite.setCurrentJoinCount(existingInvite.getCurrentJoinCount());
                newInvite.setMaxJoinCount(existingInvite.getMaxJoinCount());
                if (tenantType.equals("personal")) {
                    newInvite.setMaxJoinCount(maxJoinCount);
                } else {
                    newInvite.setMaxJoinCount(-1);
                }
                newInvite.setExpireType(inviteLinkDto.getExpireType());
                ExpireTypeEnum expireTypeEnum = ExpireTypeEnum.getByCode(inviteLinkDto.getExpireType());
                Date expireTime = calculateExpireTime(expireTypeEnum);
                newInvite.setExpireTime(expireTime);
                newInvite.setUpdateTime(now);
                newInvite.setCreateTime(now);
                newInvite.setDeleted(0);
                appMarketInviteDao.insert(newInvite);

                InviteLinkVo responseVo = getInviteLinkVo(newInvite);
                return AppResponse.success(responseVo);
            }
            // 결과가완료저장에서, 직선연결반환완료저장에서의연결
            InviteLinkVo responseVo = getInviteLinkVo(existingInvite);
            return AppResponse.success(responseVo);
        }
        // 결과가찾을 수 없습니다, 생성새기록
        AppMarketInvite appMarketInvite = new AppMarketInvite();

        appMarketInvite.setMarketId(inviteLinkDto.getMarketId());
        // 완료기기문자열로inviteKey
        String inviteKey = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        appMarketInvite.setInviteKey(inviteKey);
        // 초대사람ID
        appMarketInvite.setInviterId(userId);
        // 계획실패시간
        ExpireTypeEnum expireTypeEnum = ExpireTypeEnum.getByCode(inviteLinkDto.getExpireType());
        if (expireTypeEnum == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "지원하지 않는 실패시간유형, 지원: 4H, 24H, 7D, 30D");
        }
        Date expireTime = calculateExpireTime(expireTypeEnum);
        appMarketInvite.setExpireTime(expireTime);
        // 실패시간유형
        appMarketInvite.setExpireType(inviteLinkDto.getExpireType());
        // 대추가입력사람데이터
        if (tenantType.equals("personal")) {
            appMarketInvite.setMaxJoinCount(maxJoinCount);
        } else {
            appMarketInvite.setMaxJoinCount(-1);
        }
        appMarketInvite.setCurrentJoinCount(0);
        appMarketInvite.setCreatorId(userId);
        appMarketInvite.setCreateTime(now);
        appMarketInvite.setUpdaterId(userId);
        appMarketInvite.setUpdateTime(now);
        appMarketInvite.setDeleted(0);
        int i = appMarketInviteDao.insert(appMarketInvite);
        if (i > 0) {
            InviteLinkVo responseVo = new InviteLinkVo();
            responseVo.setInviteKey(inviteKey);
            responseVo.setExpireType(inviteLinkDto.getExpireType());
            responseVo.setExpireTime(expireTime);
            responseVo.setOverNumLimit(0);
            return AppResponse.success(responseVo);
        }
        return null;
    }

    private static InviteLinkVo getInviteLinkVo(AppMarketInvite existingInvite) {
        InviteLinkVo responseVo = new InviteLinkVo();
        responseVo.setInviteKey(existingInvite.getInviteKey());
        responseVo.setExpireTime(existingInvite.getExpireTime());
        responseVo.setExpireType(existingInvite.getExpireType());

        if (existingInvite.getMaxJoinCount() > 0) {
            if (existingInvite.getCurrentJoinCount() >= existingInvite.getMaxJoinCount()) {
                responseVo.setOverNumLimit(1);
            } else {
                responseVo.setOverNumLimit(0);
            }
        }
        return responseVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<InviteLinkVo> resetInviteLink(InviteLinkDto inviteLinkDto) throws NoLoginException {
        // 매개변수검증
        if (inviteLinkDto == null || StringUtils.isBlank(inviteLinkDto.getMarketId())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "마켓ID비워 둘 수 없습니다");
        }
        if (StringUtils.isBlank(inviteLinkDto.getExpireType())) {
            inviteLinkDto.setExpireType(ExpireTypeEnum.TWENTY_FOUR_HOURS.getCode());
        }
        // 가져오기현재로그인사용자 정보
        AppResponse<User> userResponse = rpaAuthFeign.getLoginUser();
        if (userResponse == null || !userResponse.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        AppResponse<TenantExpirationDto> resp = rpaAuthFeign.getExpiration();
        if (resp == null || !resp.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        TenantExpirationDto data = resp.getData();
        String tenantType = data.getTenantType();
        User loginUser = userResponse.getData();
        String userId = loginUser.getId();
        // 조회여부저장에서초대연결
        AppMarketInvite existingInvite =
                appMarketInviteDao.selectByMarketIdAndInviterId(inviteLinkDto.getMarketId(), userId);
        if (existingInvite == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "해당마켓완료되지 않은초대연결, 요청완료초대연결");
        }
        AppMarketInvite newInvite = new AppMarketInvite();
        BeanUtils.copyProperties(existingInvite, newInvite);
        existingInvite.setDeleted(1);
        appMarketInviteDao.cancelById(existingInvite.getId());
        String inviteKey = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Date now = new Date();
        newInvite.setInviteKey(inviteKey);
        newInvite.setCurrentJoinCount(existingInvite.getCurrentJoinCount());
        newInvite.setMaxJoinCount(existingInvite.getMaxJoinCount());
        if (tenantType.equals("personal")) {
            newInvite.setMaxJoinCount(maxJoinCount);
        } else {
            newInvite.setMaxJoinCount(-1);
        }
        newInvite.setExpireType(inviteLinkDto.getExpireType());
        ExpireTypeEnum expireTypeEnum = ExpireTypeEnum.getByCode(inviteLinkDto.getExpireType());
        Date expireTime = calculateExpireTime(expireTypeEnum);
        newInvite.setExpireTime(expireTime);
        newInvite.setUpdateTime(now);
        newInvite.setCreateTime(now);
        newInvite.setDeleted(0);
        appMarketInviteDao.insert(newInvite);
        // 생성
        InviteLinkVo responseVo = new InviteLinkVo();
        responseVo.setInviteKey(inviteKey);
        responseVo.setExpireType(inviteLinkDto.getExpireType());
        responseVo.setExpireTime(expireTime);
        responseVo.setOverNumLimit(0);
        return AppResponse.success(responseVo);
    }

    @Override
    public AppResponse<InviteInfoVo> getInviteInfoByInviteKey(String inviteKey) {
        // 매개변수검증
        if (StringUtils.isBlank(inviteKey)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "초대key비워 둘 수 없습니다");
        }
        // 근거inviteKey조회초대정보
        AppMarketInvite invite = appMarketInviteDao.selectByInviteKey(inviteKey);
        if (invite == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "초대 링크를 찾을 수 없습니다");
        }
        if (invite.getDeleted() == 1) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "초대연결완료실패");
        }
        // 조회초대여부경과
        Date now = new Date();
        if (invite.getExpireTime() != null && invite.getExpireTime().before(now)) {
            InviteInfoVo resultVo = new InviteInfoVo();
            resultVo.setResultCode(QuotaCodeEnum.E_EXPIRE.getResultCode());
            return AppResponse.success(resultVo);
        }
        // 조회현재완료추가입력사람데이터여부초과경과제한제어사람데이터
        Integer currentJoinCount = invite.getCurrentJoinCount() == null ? 0 : invite.getCurrentJoinCount();
        if (invite.getMaxJoinCount() > 0) {
            if (currentJoinCount >= invite.getMaxJoinCount()) {
                InviteInfoVo resultVo = new InviteInfoVo();
                resultVo.setResultCode(QuotaCodeEnum.E_OVER_MARKET_USER_NUM_LIMIT.getResultCode());
                return AppResponse.success(resultVo);
            }
        }
        // 가져오기초대사람이름
        String inviterName = "";
        if (StringUtils.isNotBlank(invite.getInviterId())) {
            AppResponse<String> response = rpaAuthFeign.getNameById(invite.getInviterId());
            if (response == null || response.getData() == null) {
                throw new ServiceException("가져오기사용자이름실패");
            }
            inviterName = response.getData();
        }

        // 가져오기팀이름
        String marketName = "";
        if (StringUtils.isNotBlank(invite.getMarketId())) {
            marketName = appMarketDao.getMarketNameById(invite.getMarketId());
            if (marketName == null) {
                marketName = "";
            }
        }
        // 생성
        InviteInfoVo inviteInfoVo = new InviteInfoVo();
        inviteInfoVo.setInviterName(inviterName);
        inviteInfoVo.setMarketName(marketName);

        // 가져오기현재로그인사용자 정보
        AppResponse<User> userResponse = rpaAuthFeign.getLoginUser();
        if (userResponse != null && userResponse.ok()) {
            User loginUser = userResponse.getData();
            if (null != loginUser) {
                String userId = loginUser.getId();
                if (!StringUtils.isEmpty(userId)) {
                    AppMarketUser existingUser = appMarketUserDao.getMarketUser(invite.getMarketId(), userId);
                    if (existingUser != null) {
                        inviteInfoVo.setResultCode(QuotaCodeEnum.S_REPEAT_JOIN.getResultCode());
                    }
                }
            }
        }
        return AppResponse.success(inviteInfoVo);
    }

    @Override
    public AppResponse<AcceptResultVo> acceptInvite(String inviteKey) throws NoLoginException {
        // 매개변수검증
        if (StringUtils.isBlank(inviteKey)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "초대key비워 둘 수 없습니다");
        }
        // 근거inviteKey조회초대정보
        AppMarketInvite invite = appMarketInviteDao.selectByInviteKey(inviteKey);
        if (invite == null) {
            AcceptResultVo resultVo = new AcceptResultVo(QuotaCodeEnum.E_EXPIRE);
            return AppResponse.success(resultVo);
        }
        if (invite.getDeleted() == 1) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "초대연결완료실패");
        }
        // 조회초대여부경과
        Date now = new Date();
        if (invite.getExpireTime() != null && invite.getExpireTime().before(now)) {
            AcceptResultVo resultVo = new AcceptResultVo(QuotaCodeEnum.E_EXPIRE);
            return AppResponse.success(resultVo);
        }
        // 조회현재완료추가입력사람데이터여부초과경과제한제어사람데이터
        Integer currentJoinCount = invite.getCurrentJoinCount() == null ? 0 : invite.getCurrentJoinCount();
        if (invite.getMaxJoinCount() > 0) {
            if (currentJoinCount >= invite.getMaxJoinCount()) {
                AcceptResultVo resultVo = new AcceptResultVo(QuotaCodeEnum.E_OVER_MARKET_USER_NUM_LIMIT);
                return AppResponse.success(resultVo);
            }
        }
        // 가져오기현재로그인사용자 정보
        AppResponse<User> userResponse = rpaAuthFeign.getLoginUser();
        if (userResponse == null || !userResponse.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = userResponse.getData();
        String userId = loginUser.getId();
        String marketId = invite.getMarketId();
        String createCompVerKey = doBouncePrefix + inviteKey + marketId + userId;
        // 관리
        deBounce(createCompVerKey, deBounceWindow);
        // 조회사용자여부완료에서해당마켓중
        AppMarketUser existingUser = appMarketUserDao.getMarketUser(marketId, userId);
        if (existingUser != null) {
            AcceptResultVo resultVo = new AcceptResultVo(QuotaCodeEnum.S_REPEAT_JOIN);
            return AppResponse.success(resultVo);
        }
        // 검증마켓추가입력수매칭금액
        if (!quotaCheckService.checkMarketJoinQuota()) {
            AcceptResultVo resultVo = new AcceptResultVo(QuotaCodeEnum.E_OVER_LIMIT);
            return AppResponse.success(resultVo);
        }
        return self.doAcceptInviteInTransaction(invite, userId, marketId);
    }

    @Transactional(rollbackFor = Exception.class)
    public AppResponse<AcceptResultVo> doAcceptInviteInTransaction(
            AppMarketInvite invite, String userId, String marketId) {
        Date now = new Date();
        // 삽입사용자닫기시스템
        AppMarketUser appMarketUser = buildAppMarketUser(marketId, userId, now);
        appMarketUserDao.insert(appMarketUser);
        // 업데이트초대계획데이터
        invite.setCurrentJoinCount(invite.getCurrentJoinCount() + 1);
        invite.setUpdaterId(userId);
        invite.setUpdateTime(now);
        appMarketInviteDao.updateById(invite);
        return AppResponse.success(new AcceptResultVo(QuotaCodeEnum.S_SUCCESS));
    }

    private AppMarketUser buildAppMarketUser(String marketId, String userId, Date now) {
        AppMarketUser appMarketUser = new AppMarketUser();
        appMarketUser.setMarketId(marketId);
        appMarketUser.setUserType("acquirer");
        AppResponse<String> tenantIdRes = rpaAuthFeign.getTenantId();
        if (tenantIdRes == null || !tenantIdRes.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        String tenantId = tenantIdRes.getData();
        // 목록 테넌트id
        appMarketUser.setTenantId(tenantId);
        appMarketUser.setCreatorId(userId);
        appMarketUser.setCreateTime(now);
        appMarketUser.setUpdaterId(userId);
        appMarketUser.setUpdateTime(now);
        appMarketUser.setDeleted(0);
        return appMarketUser;
    }

    /**
     * 근거실패시간유형계획실패시간
     *
     * @param expireTypeEnum 실패시간유형
     * @return 실패시간
     */
    private Date calculateExpireTime(ExpireTypeEnum expireTypeEnum) {
        if (expireTypeEnum == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        switch (expireTypeEnum) {
            case FOUR_HOURS:
                calendar.add(Calendar.HOUR_OF_DAY, 4);
                break;
            case TWENTY_FOUR_HOURS:
                calendar.add(Calendar.HOUR_OF_DAY, 24);
                break;
            case SEVEN_DAYS:
                calendar.add(Calendar.DAY_OF_MONTH, 7);
                break;
            case THIRTY_DAYS:
                calendar.add(Calendar.DAY_OF_MONTH, 30);
                break;
            default:
                return null;
        }

        return calendar.getTime();
    }
}