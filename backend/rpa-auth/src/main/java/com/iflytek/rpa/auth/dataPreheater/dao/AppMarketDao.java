package com.iflytek.rpa.auth.dataPreheater.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.auth.dataPreheater.entity.AppMarket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 팀마켓-팀테이블(AppMarket)테이블데이터베이스방문
 *
 * @author makejava
 * @since 2024-01-19 14:41:29
 */
@Mapper
public interface AppMarketDao extends BaseMapper<AppMarket> {
    AppMarket selectPublicMarket(String tenantId);

    Integer addMarketWithType(@Param("entity") AppMarket appMarket);
}