package com.iflytek.rpa.auth.sp.uap.service.impl;

import com.iflytek.rpa.auth.conf.condition.ConditionalOnSaaSOrUAP;
import com.iflytek.rpa.auth.core.entity.AppInfoBo;
import com.iflytek.rpa.auth.sp.uap.dao.RoleDao;
import com.iflytek.rpa.auth.sp.uap.dao.TenantDao;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시작조회여부있음역할[지정되지 않았습니다]
 */
@Slf4j
@Component
@ConditionalOnSaaSOrUAP
public class RolePreheater implements CommandLineRunner {

    @Value("${uap.database.name:uap_db}")
    private String databaseName;

    @Resource
    private RoleDao roleDao;

    @Autowired
    private TenantDao tenantDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(String... args) throws Exception {
        log.info("-----------------열기 조회역할[지정되지 않았습니다]-----------------");
        // 조회 t_uap_role
        Integer exist = roleDao.getUnspecifiedRole(databaseName);
        // 조회appId 및appName
        if (exist <= 0) {
            AppInfoBo info = roleDao.selectAppInfo(databaseName);
            String appId = info.getAppId();
            String appName = info.getAppName(); // 아니요까지appId
            if (StringUtils.isBlank(appId)) {
                return;
            }
            // 삽입역할[지정되지 않았습니다]
            roleDao.insertUnspecifiedRole(databaseName, appId, appName);
        }

        List<String> tenantIds = tenantDao.getAllTenantId(databaseName);
        for (String tenantId : tenantIds) {
            // 해당테넌트여부저장에서역할[지정되지 않았습니다]
            Integer j = roleDao.getUnspecifiedRoleWithTenant(databaseName, tenantId);
            if (j <= 0) {
                roleDao.insertUnspecifiedTenantBind(databaseName, tenantId);
            }
        }
        log.info("-----------------결과조회역할[지정되지 않았습니다]-----------------");
    }
}