package com.iflytek.rpa.market.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iflytek.rpa.common.feign.entity.dto.GetMarketUserByPhoneDto;
import com.iflytek.rpa.market.entity.AppMarketUser;
import com.iflytek.rpa.market.entity.MarketDto;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;

/**
 * 팀마켓-사람원테이블, n:n의닫기시스템(AppMarketUser)테이블서비스연결
 *
 * @author makejava
 * @since 2024-01-19 14:41:35
 */
public interface AppMarketUserService extends IService<AppMarketUser> {

    AppResponse<?> getUserUnDeployed(MarketDto marketDto) throws NoLoginException;

    AppResponse getUserList(MarketDto marketDto) throws NoLoginException;

    AppResponse deleteUser(MarketDto marketDto) throws NoLoginException;

    AppResponse roleSet(MarketDto marketDto) throws NoLoginException;

    AppResponse getUserByPhone(GetMarketUserByPhoneDto marketDto);

    AppResponse getUserByPhoneForOwner(MarketDto marketDto) throws NoLoginException;

    AppResponse inviteUser(MarketDto marketDto) throws NoLoginException;
}