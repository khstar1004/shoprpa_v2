package com.iflytek.rpa.market.service;

import com.iflytek.rpa.market.entity.AppMarket;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;

/**
 * 팀마켓-팀테이블(AppMarket)테이블서비스연결
 *
 * @author makejava
 * @since 2024-01-19 14:41:34
 */
public interface AppMarketService {

    AppResponse getAppType();

    AppResponse getListForPublish() throws NoLoginException;

    AppResponse getMarketList() throws NoLoginException;

    AppResponse<Integer> marketNumCheck() throws NoLoginException;

    AppResponse addMarket(AppMarket appMarket) throws NoLoginException;

    AppResponse getMarketInfo(String marketId) throws NoLoginException;

    AppResponse editTeamMarket(AppMarket appMarket) throws NoLoginException;

    AppResponse leaveTeamMarket(AppMarket appMarket) throws NoLoginException;

    AppResponse dissolveTeamMarket(AppMarket appMarket);
}