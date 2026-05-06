package com.iflytek.rpa.auth.sp.uap.dao;

import com.iflytek.rpa.auth.core.entity.UserEntitlement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 사용자권한데이터방문연결
 *
 * @author system
 */
@Mapper
public interface UserEntitlementDao {

    /**
     * 근거사용자ID및테넌트ID조회사용자권한
     *
     * @param userId   사용자ID
     * @param tenantId 테넌트ID
     * @return 사용자권한정보
     */
    UserEntitlement queryByUserIdAndTenantId(@Param("userId") String userId, @Param("tenantId") String tenantId);
}