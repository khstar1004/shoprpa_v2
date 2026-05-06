package com.iflytek.rpa.auth.sp.uap.utils;

import com.iflytek.rpa.auth.conf.condition.ConditionalOnSaaSOrUAP;
import com.iflytek.rpa.auth.utils.HttpUtils;
import com.iflytek.sec.uap.client.api.UapUserInfoAPI;
import com.iflytek.sec.uap.client.core.client.ManagementClient;
import com.iflytek.sec.uap.client.core.dto.ResponseDto;
import com.iflytek.sec.uap.client.core.dto.tenant.GetTenantDto;
import com.iflytek.sec.uap.client.core.dto.tenant.TenantDetailDto;
import com.iflytek.sec.uap.client.core.dto.tenant.UapTenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

/**
 * @author mjren
 * @date 2025-04-28 11:31
 * @copyright Copyright (c) 2025 mjren
 */
@ConditionalOnSaaSOrUAP
public class TenantUtils {
    private static final Logger log = LoggerFactory.getLogger(TenantUtils.class);

    public TenantUtils() {}

    /**
     * 가져오기현재로그인의테넌트id
     * @return
     */
    public static String getTenantId() {
        return UapUserInfoAPI.getTenantId(HttpUtils.getRequest());
    }

    /**
     * 가져오기현재로그인의테넌트이름
     * @return
     */
    public static String getTenantName() {
        UapTenant tenantInfo = UapUserInfoAPI.getTenant(HttpUtils.getRequest());
        if (tenantInfo != null) {
            return tenantInfo.getName();
        } else {
            log.error("가져오기테넌트이름실패");
            return null;
        }
    }

    /**
     * 근거테넌트id조회테넌트 정보
     * @param tenantId
     * @return
     */
    public static UapTenant queryTenantInfoById(String tenantId) {
        UapTenant tenant = new UapTenant();
        GetTenantDto getTenantDto = new GetTenantDto();
        getTenantDto.setId(tenantId);
        ManagementClient managementClient = UapManagementClientUtil.getManagementClient(HttpUtils.getRequest());
        ResponseDto<TenantDetailDto> tenantDetailResponse = managementClient.queryTenantDetailInfo(getTenantDto);
        if (tenantDetailResponse.isFlag()) {
            TenantDetailDto tenantDetailDto = tenantDetailResponse.getData();
            BeanUtils.copyProperties(tenantDetailDto, tenant);
            return tenant;
        } else {
            log.error("queryUserPageListByOrg연결호출예외 {}", tenantDetailResponse.getMessage());
            return null;
        }
    }
}