package com.iflytek.rpa.market.service;

import com.iflytek.rpa.market.entity.MarketDto;
import com.iflytek.rpa.market.entity.dto.AllAppListDto;
import com.iflytek.rpa.market.entity.dto.AppUpdateCheckDto;
import com.iflytek.rpa.market.entity.dto.MarketResourceDto;
import com.iflytek.rpa.market.entity.dto.ShareRobotDto;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;

/**
 * 팀마켓-테이블(AppMarketResource)테이블서비스연결
 *
 * @author mjren
 * @since 2024-10-21 14:36:30
 */
public interface AppMarketResourceService {

    /**
     * 봇공유까지팀마켓
     */
    AppResponse<?> shareRobot(ShareRobotDto marketResourceDto) throws Exception;

    /**
     * 가져오기봇
     */
    AppResponse<?> obtainRobot(MarketResourceDto marketResourceDto) throws Exception;

    /**
     * 완료모듈계정목록조회
     */
    AppResponse<?> getDeployedUserList(MarketDto marketDto) throws Exception;

    /**
     * 모듈
     */
    AppResponse<?> deployRobot(MarketDto marketDto) throws Exception;

    /**
     * 업데이트-관리관리요소업데이트
     */
    AppResponse<?> updateRobotByPush(MarketDto marketDto) throws Exception;

    /**
     * 버전-버전목록조회
     */
    AppResponse<?> getVersionListForApp(MarketDto marketDto) throws Exception;

    /**
     * 삭제app
     */
    AppResponse<?> deleteApp(String appId, String marketId) throws Exception;

    /**
     * 사용목록연결
     */
    AppResponse<?> getALlAppList(AllAppListDto allAppListDto) throws NoLoginException;

    /**
     * 사용목록업데이트상태
     */
    AppResponse<?> appUpdateCheck(AppUpdateCheckDto queryDto) throws Exception;

    /**
     * 사용
     */
    AppResponse<?> appDetail(String appId, String marketId) throws Exception;

    /**
     * 실행공유(검토통신경과후호출)
     */
    AppResponse<?> executeShareRobotLogic(ShareRobotDto marketResourceDto, String userId, String tenantId);
}