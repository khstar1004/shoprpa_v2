package com.iflytek.rpa.triggerTask.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.task.entity.enums.SourceTypeEnum;
import com.iflytek.rpa.triggerTask.dao.TaskMailMapper;
import com.iflytek.rpa.triggerTask.entity.TaskMail;
import com.iflytek.rpa.triggerTask.service.ITaskMailService;
import com.iflytek.rpa.triggerTask.service.TriggerTaskService;
import com.iflytek.rpa.utils.HttpUtils;
import com.iflytek.rpa.utils.MonitorUtils;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * <p>
 * 예약 작업실행 결과 서비스유형
 * </p>
 *
 * @author keler
 * @since 2021-10-08
 */
@Service
public class TaskMailServiceImpl extends ServiceImpl<TaskMailMapper, TaskMail> implements ITaskMailService {

    @Autowired
    private TriggerTaskService triggerTaskService;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    public IPage<TaskMail> getTaskMailPage(Long pageNo, Long pageSize, String userId) throws NoLoginException {

        IPage<TaskMail> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<TaskMail> wrapper = new LambdaQueryWrapper<>();
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User userDetail = response.getData();
        String sourceType = HttpUtils.getSourceType();

        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();

        if (SourceTypeEnum.WEB.getCode().equals(sourceType) && userId != null) {
            userDetail.setId(userId);
        }
        wrapper.eq(TaskMail::getUserId, userDetail.getId());
        wrapper.eq(TaskMail::getTenantId, tenantId);
        wrapper.orderByDesc(TaskMail::getId);
        page = this.page(page, wrapper);
        //        if(CollectionUtils.isEmpty(page.getRecords())){
        //            return page;
        //        }
        //        for(TaskMail mail:page.getRecords()){
        //            mail.setEnableSSL(mail.getSslFlag());
        //        }
        return page;
    }

    @Override
    public void saveMail(TaskMail mail) throws NoLoginException {
        TaskMail existMail = baseMapper.selectOne(new LambdaQueryWrapper<TaskMail>()
                .eq(TaskMail::getResourceId, mail.getResourceId())
                .orderByDesc(TaskMail::getId)
                .last("limit 1"));
        if (existMail != null) {
            mail.setId(existMail.getId());
        }
        preDealMailInfo(mail);
        checkMailInfo(mail);
        saveOrUpdate(mail);
    }

    private void checkMailInfo(TaskMail mail) {
        LambdaQueryWrapper<TaskMail> wrapper = new LambdaQueryWrapper<TaskMail>()
                .eq(TaskMail::getEmailAccount, mail.getEmailAccount())
                .eq(TaskMail::getUserId, mail.getUserId())
                .eq(TaskMail::getTenantId, mail.getTenantId());
        if (mail.getId() != null) {
            // ,조회재정렬제거
            wrapper.ne(TaskMail::getId, mail.getId());
        }
        Integer count = baseMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ServiceException("메일함계정완료저장에서");
        }
    }

    private void preDealMailInfo(TaskMail mail) throws NoLoginException {
        if ("qqEmail".equals(mail.getEmailService())) {
            mail.setEmailServiceAddress("imap.qq.com");
            mail.setPort("993");
        } else if ("163Email".equals(mail.getEmailService())) {
            mail.setEmailServiceAddress("imap.163.com");
            mail.setPort("993");
        } else if ("126Email".equals(mail.getEmailService())) {
            mail.setEmailServiceAddress("imap.126.com");
            mail.setPort("993");
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

        if (mail.getUserId() == null) {
            mail.setUserId(userId);
        }
        mail.setTenantId(tenantId);
        mail.setPort(mail.getPort().replaceAll(" ", ""));
        mail.setEmailServiceAddress(mail.getEmailServiceAddress().replaceAll(" ", ""));
        mail.setEmailService(mail.getEmailService().replaceAll(" ", ""));
        mail.setEmailAccount(mail.getEmailAccount().replaceAll(" ", ""));
        mail.setEmailProtocol(mail.getEmailProtocol().replaceAll(" ", ""));
        mail.setAuthorizationCode(mail.getAuthorizationCode().replaceAll(" ", ""));
    }

    @Override
    public String connectMail(TaskMail mail) {
        if ("qqEmail".equals(mail.getEmailService())) {
            mail.setEmailServiceAddress("imap.qq.com");
            mail.setPort("993");
        } else if ("163Email".equals(mail.getEmailService())) {
            mail.setEmailServiceAddress("imap.163.com");
            mail.setPort("993");
        } else if ("126Email".equals(mail.getEmailService())) {
            mail.setEmailServiceAddress("imap.126.com");
            mail.setPort("993");
        } else if ("iflytekEmail".equals(mail.getEmailService())) {
            mail.setEmailServiceAddress("mail.iflytek.com");
            mail.setPort("993");
        }
        mail.setPort(mail.getPort().replaceAll(" ", ""));
        mail.setEmailServiceAddress(mail.getEmailServiceAddress().replaceAll(" ", ""));
        mail.setEmailService(mail.getEmailService().replaceAll(" ", ""));
        mail.setEmailAccount(mail.getEmailAccount().replaceAll(" ", ""));
        mail.setEmailProtocol(mail.getEmailProtocol().replaceAll(" ", ""));
        mail.setAuthorizationCode(mail.getAuthorizationCode().replaceAll(" ", ""));
        return MonitorUtils.connectMail(
                mail.getEmailProtocol().toLowerCase().trim(),
                Integer.valueOf(mail.getPort()),
                mail.getEmailServiceAddress().trim(),
                mail.getEnableSSL(),
                mail.getEmailAccount().trim(),
                mail.getAuthorizationCode().trim());
    }

    @Override
    public boolean deleteMail(String resourceId) {
        List<String> usingTasksNames = triggerTaskService.getUsingTasksByMail(resourceId); // todo 아니요일지정가능, 데이터결과계획아니요
        if (!CollectionUtils.isEmpty(usingTasksNames)) {
            return false;
        }
        baseMapper.delete(new LambdaQueryWrapper<TaskMail>().eq(TaskMail::getResourceId, resourceId));
        return true;
    }

    @Override
    public List<TaskMail> getTaskMailsByResourceIds(List<String> mailIds) {
        return baseMapper.selectList(new LambdaQueryWrapper<TaskMail>().in(TaskMail::getResourceId, mailIds));
    }
}