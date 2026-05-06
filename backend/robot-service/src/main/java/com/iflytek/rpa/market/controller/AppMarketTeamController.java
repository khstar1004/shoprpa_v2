package com.iflytek.rpa.market.controller;

import static com.iflytek.rpa.market.constants.RightConstant.*;

import com.iflytek.rpa.market.annotation.RightCheck;
import com.iflytek.rpa.market.entity.AppMarket;
import com.iflytek.rpa.market.entity.AppMarketDict;
import com.iflytek.rpa.market.entity.AppMarketDo;
import com.iflytek.rpa.market.service.AppMarketService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 팀마켓-팀
 *
 * @author makejava
 * @since 2024-01-19 14:41:28
 */
@RestController
@RequestMapping("market-team")
public class AppMarketTeamController {
    /**
     * 서비스객체
     */
    @Resource
    private AppMarketService appMarketService;

    /**
     * 유형-행목록
     * @return
     */
    @PostMapping("/type")
    public AppResponse<List<AppMarketDict>> getAppType() {

        return appMarketService.getAppType();
    }

    /**
     * 게시마켓-마켓목록, 사용자여부있음마켓
     *
     * @param
     * @return
     */
    @PostMapping("/list")
    public AppResponse<AppMarketDo> getListForPublish() throws NoLoginException {

        return appMarketService.getListForPublish();
    }

    /**
     * 팀마켓-마켓이름목록-왼쪽
     *
     * @param
     * @return
     */
    @PostMapping("/get-list")
    public AppResponse<List<AppMarket>> getList() throws NoLoginException {

        return appMarketService.getMarketList();
    }

    /**
     * 팀마켓수조회
     * @return 0-가득금액
     * @throws NoLoginException
     */
    @GetMapping("/market-num-check")
    public AppResponse<Integer> marketNumCheck() throws NoLoginException {
        return appMarketService.marketNumCheck();
    }

    /**
     * 생성팀마켓
     *
     * @param
     * @return
     */
    @PostMapping("/add")
    public AppResponse<Boolean> addMarket(@RequestBody AppMarket appMarket) throws NoLoginException {

        return appMarketService.addMarket(appMarket);
    }

    /**
     * 가져오기마켓정보
     * @param marketId
     * @return
     */
    @PostMapping("/info")
    AppResponse<AppMarket> getMarketInfo(@RequestParam("marketId") String marketId) throws NoLoginException {
        return appMarketService.getMarketInfo(marketId);
    }

    /**
     * 마켓
     *
     * @param
     * @return
     */
    @PostMapping("/edit")
    @RightCheck(dictCode = market_team_edit, clazz = AppMarket.class)
    public AppResponse<Boolean> editTeamMarket(@RequestBody AppMarket appMarket) throws NoLoginException {

        return appMarketService.editTeamMarket(appMarket);
    }

    /**
     * 열기팀마켓
     *
     * @param
     * @return
     */
    @PostMapping("/leave")
    @RightCheck(dictCode = market_team_leave, clazz = AppMarket.class)
    public AppResponse<Boolean> leaveTeamMarket(@RequestBody AppMarket appMarket) throws NoLoginException {

        return appMarketService.leaveTeamMarket(appMarket);
    }

    /**
     * 해제팀마켓
     *
     * @param
     * @return
     */
    @PostMapping("/dissolve")
    @RightCheck(dictCode = market_team_dissolve, clazz = AppMarket.class)
    public AppResponse<Boolean> dissolveTeamMarket(@RequestBody AppMarket appMarket) throws NoLoginException {

        return appMarketService.dissolveTeamMarket(appMarket);
    }
}