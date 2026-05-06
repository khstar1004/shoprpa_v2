package com.iflytek.rpa.auth.sp.uap.mapper;

import com.iflytek.rpa.auth.core.entity.Tenant;
import com.iflytek.rpa.auth.sp.uap.constants.UAPConstant;
import com.iflytek.sec.uap.client.core.dto.tenant.UapTenant;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Tenant기기
 * 사용를UAP클라이언트의UapTenant변환로core패키지아래의Tenant
 *
 * @author xqcao2
 */
@Component
public class TenantMapper {

    /**
     * 를UAP클라이언트의UapTenant변환로Tenant
     *
     * @param uapTenant UAP클라이언트의UapTenant
     * @return core패키지아래의Tenant
     */
    public Tenant fromUapTenant(UapTenant uapTenant) {
        if (uapTenant == null) {
            return null;
        }

        Tenant tenant = new Tenant();
        // 사용BeanUtils복사속성
        BeanUtils.copyProperties(uapTenant, tenant);

        if (uapTenant.getTenantCode() == null
                || uapTenant.getTenantCode().startsWith(UAPConstant.PERSONAL_TENANT_CODE)) {
            tenant.setTenantType(UAPConstant.TENANT_TYPE_PERSONAL);
        } else if (uapTenant.getTenantCode().startsWith(UAPConstant.PROFESSIONAL_TENANT_CODE)) {
            tenant.setTenantType(UAPConstant.TENANT_TYPE_PROFESSIONAL);
        } else if (uapTenant.getTenantCode().startsWith(UAPConstant.ENTERPRISE_PURCHASED_TENANT_CODE)) {
            tenant.setTenantType(UAPConstant.TENANT_TYPE_ENTERPRISE_PURCHASED);
        } else if (uapTenant.getTenantCode().startsWith(UAPConstant.ENTERPRISE_SUBSCRIPTION_TENANT_CODE)) {
            tenant.setTenantType(UAPConstant.TENANT_TYPE_ENTERPRISE_SUBSCRIPTION);
        }

        return tenant;
    }
}