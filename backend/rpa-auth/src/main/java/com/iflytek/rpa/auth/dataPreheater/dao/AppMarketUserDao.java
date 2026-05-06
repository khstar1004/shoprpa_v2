package com.iflytek.rpa.auth.dataPreheater.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.auth.dataPreheater.entity.AppMarketUser;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 팀마켓-사람원테이블, n:n의닫기시스템(AppMarketUser)테이블데이터베이스방문
 *
 * @author makejava
 * @since 2024-01-19 14:41:35
 */
@Mapper
public interface AppMarketUserDao extends BaseMapper<AppMarketUser> {

    Integer addDefaultUser(@Param("entity") AppMarketUser appMarketUser);

    Integer addUser(@Param("entity") AppMarketUser appMarketUser);

    @Select("select creator_id " + "from app_market_user "
            + "where deleted = 0 and market_id = #{marketId} and tenant_id = #{tenantId}")
    List<String> getAllUserId(@Param("tenantId") String tenantId, @Param("marketId") String marketId);

    /**
     * 시스템계획행데이터
     *
     * @param appMarketUser 조회파일
     * @return 행데이터
     */
    long count(AppMarketUser appMarketUser);

    void insertBatch(List<AppMarketUser> insertBatchList);
}