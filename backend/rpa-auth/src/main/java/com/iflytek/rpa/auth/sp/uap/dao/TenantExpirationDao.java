package com.iflytek.rpa.auth.sp.uap.dao;

import com.iflytek.rpa.auth.sp.uap.entity.TenantExpiration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 테넌트까지정보데이터방문연결
 *
 * @author system
 */
@Mapper
public interface TenantExpirationDao {

    /**
     * 근거테넌트ID조회테넌트까지정보
     *
     * @param tenantId 테넌트ID
     * @return 테넌트까지정보
     */
    TenantExpiration queryByTenantId(@Param("tenantId") String tenantId);
}