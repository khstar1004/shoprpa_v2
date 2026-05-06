package com.iflytek.rpa.auth.dataPreheater.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.auth.dataPreheater.entity.AppMarketClassification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 앱 마켓분유형테이블(AppMarketClassification)데이터베이스방문
 *
 * @author auto-generated
 */
@Mapper
public interface AppMarketClassificationDao extends BaseMapper<AppMarketClassification> {

    Integer insertDefaultClassification(@Param("tenantId") String tenantId);

    /**
     * 근거테넌트ID조회분유형데이터행데이터
     *
     * @param tenantId 테넌트ID
     * @return 데이터행데이터
     */
    Integer countByTenantId(@Param("tenantId") String tenantId);
}