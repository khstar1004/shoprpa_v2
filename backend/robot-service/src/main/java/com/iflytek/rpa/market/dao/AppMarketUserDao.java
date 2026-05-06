package com.iflytek.rpa.market.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iflytek.rpa.market.entity.AppMarket;
import com.iflytek.rpa.market.entity.AppMarketUser;
import com.iflytek.rpa.market.entity.MarketDto;
import com.iflytek.rpa.market.entity.TenantUser;
import java.util.List;
import java.util.Set;
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

    IPage<MarketDto> getUserList(
            IPage<MarketDto> pageConfig,
            @Param("entity") MarketDto marketDto,
            @Param("databaseName") String databaseName);

    Integer roleSet(@Param("entity") MarketDto marketDto);

    String getOwnerByRole(@Param("marketId") String marketId);

    String getUserTypeForCheck(@Param("userId") String userId, @Param("marketId") String marketId);

    List<MarketDto> getUserUnDeployed(@Param("entity") MarketDto marketDto, @Param("databaseName") String databaseName);

    List<MarketDto> getUserByPhone(@Param("entity") MarketDto marketDto, @Param("databaseName") String databaseName);

    List<MarketDto> getUserByPhoneForOwner(
            @Param("entity") MarketDto marketDto,
            @Param("marketId") String marketId,
            @Param("userId") String userId,
            @Param("databaseName") String databaseName);

    @Select("select creator_id " + "from app_market_user "
            + "where deleted = 0 and market_id = #{marketId} and tenant_id = #{tenantId}")
    List<String> getAllUserId(@Param("tenantId") String tenantId, @Param("marketId") String marketId);

    List<TenantUser> getTenantUser(
            @Param("tenantId") String tenantId,
            @Param("entities") List<AppMarketUser> userInfoList,
            @Param("databaseName") String databaseName);

    Integer leaveTeamMarket(@Param("entity") AppMarket appMarket);

    Integer updateToOwner(@Param("marketId") String marketId, @Param("newOwnerId") String newOwnerId);

    Integer deleteAllUser(@Param("marketId") String marketId);

    Set<String> getMarketUserListForDeploy(
            @Param("marketId") String marketId, @Param("userIdList") List<String> userIdList);

    /**
     * 시스템계획행데이터
     *
     * @param appMarketUser 조회파일
     * @return 행데이터
     */
    long count(AppMarketUser appMarketUser);

    @Select(
            "select user_type from app_market_user where deleted=0 and market_id=#{marketId} and creator_id=#{creatorId}")
    String getUserType(@Param("marketId") String marketId, @Param("creatorId") String creatorId);

    @Select("select * from app_market_user where deleted=0 and market_id=#{marketId} and creator_id=#{creatorId}")
    AppMarketUser getMarketUser(@Param("marketId") String marketId, @Param("creatorId") String creatorId);

    @Select("select user_type " + "from app_market_user "
            + "where deleted=0 and market_id=#{marketId} and creator_id=#{userId} and tenant_id = #{tenantId}")
    String getMarketUserType(
            @Param("marketId") String marketId, @Param("userId") String userId, @Param("tenantId") String tenantId);

    List<String> getMarketUserInList(
            @Param("marketId") String marketId,
            @Param("userIdList") List<String> userIdList,
            @Param("tenantId") String tenantId);

    void insertBatch(List<AppMarketUser> insertBatchList);

    @Select(
            "select id from app_market_user where deleted=0 and market_id=#{marketId} and creator_id=#{creatorId} and user_type != 'owner' and deleted = 0")
    String getIdByMarketIdAndCreatorId(@Param("marketId") String marketId, @Param("creatorId") String creatorId);

    Integer deleteById(@Param("id") String id);

    IPage<MarketDto> getUserListByPublic(
            IPage<MarketDto> pageConfig,
            @Param("entity") MarketDto marketDto,
            @Param("nowUserid") String nowUserid,
            @Param("databaseName") String databaseName);

    /**
     * 조회사용자에서현재테넌트아래완료추가입력의마켓수(삭제되지 않음의)
     *
     * @param tenantId 테넌트ID
     * @param userId   사용자ID
     * @return 마켓수
     */
    Integer getMarketJoinCount(@Param("tenantId") String tenantId, @Param("userId") String userId);
}