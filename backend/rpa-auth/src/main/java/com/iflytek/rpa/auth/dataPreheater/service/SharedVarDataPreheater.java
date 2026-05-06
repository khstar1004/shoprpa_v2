package com.iflytek.rpa.auth.dataPreheater.service;

import com.iflytek.rpa.auth.dataPreheater.dao.SharedVarKeyTenantDao;
import com.iflytek.rpa.auth.dataPreheater.entity.InitDataEvent;
import com.iflytek.rpa.auth.dataPreheater.entity.SharedVarKeyTenant;
import com.iflytek.rpa.auth.utils.IdWorker;
import com.iflytek.rpa.auth.utils.StringUtils;
import java.security.SecureRandom;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SharedVarDataPreheater {

    @Autowired
    private IdWorker idWorker;

    @Resource
    private SharedVarKeyTenantDao sharedVarKeyTenantDao;

    @EventListener(classes = {InitDataEvent.class})
    public void insertShareVarTenantKey(Object event) {
        if (event instanceof InitDataEvent) {
            InitDataEvent initDataEvent = (InitDataEvent) event;
            String tenantId = initDataEvent.getTenantId();
            if (!StringUtils.isEmpty(tenantId)) {
                SharedVarKeyTenant sharedVarKeyTenant = sharedVarKeyTenantDao.selectByTenantId(tenantId);
                if (sharedVarKeyTenant == null) {
                    SharedVarKeyTenant keyTenant = new SharedVarKeyTenant();
                    keyTenant.setId(idWorker.nextId());
                    keyTenant.setTenantId(tenantId);
                    keyTenant.setKey(generateRandomKey(32));
                    keyTenant.setDeleted(0);
                    sharedVarKeyTenantDao.insert(keyTenant);
                }
            }
        }
    }

    /**
     * 완료지정길이정도의기기키
     *
     * @param length 키길이정도
     * @return 기기키
     */
    private String generateRandomKey(int length) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < length; i++) {
            key.append(chars.charAt(random.nextInt(chars.length())));
        }
        return key.toString();
    }
}