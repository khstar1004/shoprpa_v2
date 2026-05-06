package com.iflytek.rpa.base.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.base.entity.SysTenantConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 테넌트매칭테이블 Mapper 연결
 */
@Mapper
public interface SysTenantConfigDao extends BaseMapper<SysTenantConfig> {

    /**
     * 근거테넌트ID조회테넌트매칭
     */
    @Select("SELECT * FROM sys_tenant_config WHERE tenant_id = #{tenantId} AND deleted = 0 LIMIT 1")
    SysTenantConfig selectByTenantId(@Param("tenantId") String tenantId);
}