package com.iflytek.rpa.market.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.market.entity.AppMarketInvite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 팀마켓-초대연결테이블
 */
@Mapper
public interface AppMarketInviteDao extends BaseMapper<AppMarketInvite> {
    /**
     * 근거초대key조회초대연결(삭제되지 않음의)
     *
     * @param inviteKey 초대key
     * @return 초대연결
     */
    AppMarketInvite selectByInviteKey(@Param("inviteKey") String inviteKey);

    AppMarketInvite selectByMarketIdAndInviterId(String marketId, String userId);

    int cancelById(Long id);
}