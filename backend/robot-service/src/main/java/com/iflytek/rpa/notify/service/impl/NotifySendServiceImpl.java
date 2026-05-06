package com.iflytek.rpa.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.common.feign.entity.dto.GetUserDto;
import com.iflytek.rpa.common.feign.entity.dto.UserExtendDto;
import com.iflytek.rpa.market.dao.AppMarketDao;
import com.iflytek.rpa.market.dao.AppMarketUserDao;
import com.iflytek.rpa.market.entity.AppMarketUser;
import com.iflytek.rpa.market.entity.vo.AcceptResultVo;
import com.iflytek.rpa.notify.entity.NotifySend;
import com.iflytek.rpa.notify.entity.dto.ApplicationNotifyDto;
import com.iflytek.rpa.notify.entity.dto.CreateNotifyDto;
import com.iflytek.rpa.notify.entity.dto.NotifyListDto;
import com.iflytek.rpa.notify.entity.vo.NotifyVo;
import com.iflytek.rpa.notify.mapper.NotifySendMapper;
import com.iflytek.rpa.notify.service.NotifySendService;
import com.iflytek.rpa.quota.service.QuotaCheckService;
import com.iflytek.rpa.robot.dao.RobotExecuteDao;
import com.iflytek.rpa.robot.entity.RobotExecute;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import com.iflytek.rpa.utils.response.QuotaCodeEnum;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class NotifySendServiceImpl extends ServiceImpl<NotifySendMapper, NotifySend> implements NotifySendService {

    @Resource
    private AppMarketUserDao appMarketUserDao;

    @Resource
    private AppMarketDao appMarketDao;

    @Resource
    private NotifySendMapper notifySendMapper;

    @Resource
    private RobotExecuteDao robotExecuteDao;

    @Autowired
    private QuotaCheckService quotaCheckService;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    public AppResponse<?> createNotify(CreateNotifyDto createNotifyDto) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String inviteUserId = loginUser.getId();

        List<AppMarketUser> marketUserList = createNotifyDto.getMarketUserList();

        List<NotifySend> notifySendList = new ArrayList<>();
        for (AppMarketUser marketUser : marketUserList) {

            if (marketUser == null) continue;

            NotifySend notifySend = new NotifySend();

            // 메시지
            String messageInfo = "";
            if (StringUtils.equals(createNotifyDto.getMessageType(), "teamMarketInvite")) {
                // 사람메시지
                messageInfo = buildMessageInfo4Invite(inviteUserId, createNotifyDto.getMarketId());
            } else if (StringUtils.equals(createNotifyDto.getMessageType(), "teamMarketUserAdd")) {
                messageInfo = buildMessageInfo4UserAdd(createNotifyDto.getMarketId());
                createNotifyDto.setMessageType("teamMarketUpdate");
            } else if (StringUtils.equals(createNotifyDto.getMessageType(), "teamMarketUserRemove")) {
                messageInfo = buildMessageInfo4UserRemove(createNotifyDto.getMarketId());
                createNotifyDto.setMessageType("teamMarketUpdate");
            } else {
                // 사용업데이트의메시지
                messageInfo = buildMessageInfo4AppUpdate(createNotifyDto.getMarketId(), createNotifyDto.getAppId());
            }

            // appName
            String appName = baseMapper.getAppName(createNotifyDto.getMarketId(), createNotifyDto.getAppId());

            // 데이터
            notifySend.setTenantId(createNotifyDto.getTenantId());
            notifySend.setUserId(marketUser.getCreatorId());
            notifySend.setMessageType(createNotifyDto.getMessageType());
            notifySend.setMarketId(createNotifyDto.getMarketId());
            notifySend.setUserType(marketUser.getUserType());
            notifySend.setMessageInfo(messageInfo);
            notifySend.setOperateResult(1); // 로메시지미완료
            if (createNotifyDto.getOperateResult() != null) {
                notifySend.setOperateResult(createNotifyDto.getOperateResult());
            }
            notifySend.setAppName(appName);

            notifySendList.add(notifySend);
        }

        if (notifySendList.size() == 0) return AppResponse.success("있음메시지필요알림");

        boolean b = saveBatch(notifySendList);

        if (b || marketUserList.size() == 0) {
            return AppResponse.success("제품메시지성공");
        } else {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EXCEPTION);
        }
    }

    private String buildMessageInfo4UserAdd(String marketId) {
        String marketName = appMarketDao.getMarketNameById(marketId);
        return "완료관리관리원추가팀마켓: [" + marketName + "]";
    }

    private String buildMessageInfo4UserRemove(String marketId) {
        String marketName = appMarketDao.getMarketNameById(marketId);
        return "완료관리관리원에서팀마켓: [" + marketName + "]중출력";
    }

    public void createNotify4Application(ApplicationNotifyDto applicationNotifyDto) {
        NotifySend notifySend = new NotifySend();
        notifySend.setTenantId(applicationNotifyDto.getTenantId());
        notifySend.setUserId(applicationNotifyDto.getUserId());
        notifySend.setMarketId(applicationNotifyDto.getMarketId());
        notifySend.setMessageType("teamMarketUpdate");
        notifySend.setUserType("admin");
        notifySend.setMessageInfo(buildMessageInfo4Application(applicationNotifyDto));
        notifySend.setOperateResult(1); // 로메시지미완료
        this.saveOrUpdate(notifySend);
    }

    @Override
    public AppResponse<?> notifyList(NotifyListDto notifyListDto) throws NoLoginException {

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
        Long pageNo = notifyListDto.getPageNo();
        Long pageSize = notifyListDto.getPageSize();

        IPage<NotifySend> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<NotifySend> wrapper = new LambdaQueryWrapper<>();

        Date date = new Date();

        wrapper.eq(NotifySend::getDeleted, 0);
        wrapper.eq(NotifySend::getUserId, userId);
        wrapper.eq(NotifySend::getTenantId, tenantId);
        wrapper.last(" and create_time >= DATE_SUB(NOW(), INTERVAL 6 MONTH) " + "order by create_time desc");

        IPage<NotifySend> rePage = this.page(page, wrapper);

        // 결과연결
        List<NotifySend> records = rePage.getRecords();
        List<NotifyVo> newRecords = new ArrayList<>();
        for (NotifySend record : records) {
            NotifyVo notifyVo = new NotifyVo();
            notifyVo.setId(record.getId());
            notifyVo.setCreateTime(record.getCreateTime());
            notifyVo.setOperateResult(record.getOperateResult());
            notifyVo.setMessageType(record.getMessageType());
            notifyVo.setMessageInfo(record.getMessageInfo());
            notifyVo.setAppName(record.getAppName());
            notifyVo.setMarketId(record.getMarketId());
            newRecords.add(notifyVo);
        }

        IPage<NotifyVo> res = new Page<>(pageNo, pageSize);
        res.setRecords(newRecords);
        res.setTotal(rePage.getTotal());
        res.setPages(rePage.getPages());
        res.setSize(rePage.getSize());
        res.setCurrent(rePage.getCurrent());

        return AppResponse.success(res);
    }

    @Override
    public AppResponse<?> hasNotify() throws NoLoginException {

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

        Integer unreadNum = notifySendMapper.getUnreadNum(userId, tenantId);

        if (unreadNum >= 1) return AppResponse.success("1");
        else return AppResponse.success("0");
    }

    @Override
    public AppResponse<?> setAllNotifyRead() throws NoLoginException {

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

        boolean b = baseMapper.allNotifyRead(userId, tenantId);
        if (b) return AppResponse.success("일완료완료");

        return response;
    }

    @Override
    public AppResponse<?> setSelectedNotifyRead(Long notifyId) throws NoLoginException {

        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        boolean b = baseMapper.setOneRead(notifyId);
        if (b) {
            return AppResponse.success("완료완료");
        } else {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EXCEPTION);
        }
    }

    @Override
    public AppResponse<?> rejectJoinTeam(Long notifyId) throws NoLoginException {

        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        boolean b = baseMapper.setOneReject(notifyId);
        if (b) {
            return AppResponse.success("성공");
        } else {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EXCEPTION);
        }
    }

    @Transactional
    @Override
    public AppResponse<?> acceptJoinTeam(Long notifyId) throws NoLoginException {

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

        NotifySend notifySend = baseMapper.selectById(notifyId);
        if (notifySend == null) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EXCEPTION);
        }
        if (!userId.equals(notifySend.getUserId())) {
            return AppResponse.error("권한방문");
        }

        if (notifySend.getOperateResult().equals(3)
                || notifySend.getOperateResult().equals(4)) {
            return AppResponse.error("완료, 요청 재복사");
        }

        // 여부완료에서해당마켓중
        String marketId = baseMapper.getMarketIdFromAppMarketUser(userId, notifySend.getMarketId());
        if (marketId != null) {
            baseMapper.joinTeam(notifyId);
            return AppResponse.error("완료에서팀중, 필요하지 않습니다재복사추가입력");
        } else {
            // 검증마켓추가입력수매칭금액
            if (!quotaCheckService.checkMarketJoinQuota()) {
                AcceptResultVo resultVo = new AcceptResultVo(QuotaCodeEnum.E_OVER_LIMIT);
                return AppResponse.success(resultVo);
            }

            AppMarketUser appMarketUser = new AppMarketUser();
            appMarketUser.setMarketId(notifySend.getMarketId());
            appMarketUser.setUserType(notifySend.getUserType());
            appMarketUser.setCreatorId(userId);
            appMarketUser.setUpdaterId(userId);

            int insert = appMarketUserDao.insert(appMarketUser);
            boolean b = baseMapper.joinTeam(notifyId);

            if (insert > 0 && b) {
                return AppResponse.success("추가입력팀성공");
            } else {
                return AppResponse.error("추가입력팀실패");
            }
        }
    }

    private String buildMessageInfo4Invite(String userId, String marketId) {
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        GetUserDto getUserDto = new GetUserDto();
        getUserDto.setUserId(userId);
        AppResponse<UserExtendDto> userExtendInfoRes = rpaAuthFeign.getUserExtendInfo(tenantId, getUserDto);
        UserExtendDto data = userExtendInfoRes.getData();
        String userName = data.getUser().getName();
        String marketName = baseMapper.getMarketName(marketId);

        String res = "[" + userName + "]" + "초대추가입력팀마켓: " + "[" + marketName + "]" + "," + "여부추가입력?";
        return res;
    }

    private String buildMessageInfo4AppUpdate(String marketId, String appId) {
        String marketName = baseMapper.getMarketName(marketId);
        String appName = baseMapper.getAppName(marketId, appId);

        String res = "에서팀마켓[" + marketName + "]가져오기의사용//컴포넌트[" + appName + "]있음업데이트, ";
        return res;
    }

    private String buildMessageInfo4Application(ApplicationNotifyDto applicationNotifyDto) {
        String statusStr = "";
        String status = applicationNotifyDto.getStatus();
        if ("approved".equals(status)) {
            statusStr = "";
        } else if ("rejected".equals(status)) {
            statusStr = "돌아가기";
        }
        String applicationTypeStr = "";
        String applicationType = applicationNotifyDto.getApplicationType();
        if ("use".equals(applicationType)) {
            applicationTypeStr = "사용";
        } else if ("release".equals(applicationType)) {
            applicationTypeStr = "위";
        }
        RobotExecute robotExecute = robotExecuteDao.queryByRobotId(
                applicationNotifyDto.getRobotId(),
                applicationNotifyDto.getUserId(),
                applicationNotifyDto.getTenantId());
        String robotStr = robotExecute != null ? robotExecute.getName() : "";

        return String.format("의%s봇%s신청프로세스%s, 요청 앱 마켓조회", robotStr, applicationTypeStr, statusStr);
    }
}