package com.iflytek.rpa.market.controller;

import static com.iflytek.rpa.market.constants.RightConstant.*;

import com.iflytek.rpa.common.feign.entity.dto.GetMarketUserByPhoneDto;
import com.iflytek.rpa.market.annotation.RightCheck;
import com.iflytek.rpa.market.entity.MarketDto;
import com.iflytek.rpa.market.service.AppMarketUserService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 팀마켓-사람원
 *
 * @author makejava
 * @since 2024-01-19 14:41:35
 */
@RestController
@RequestMapping("market-user")
public class AppMarketUserController {
    /**
     * 서비스객체
     */
    @Resource
    private AppMarketUserService appMarketUserService;

    /**
     * 미완료모듈계정목록조회
     * @param marketDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/undeploy-user")
    //    @RightCheck(dictCode = market_user_get_user)
    public AppResponse<?> getUserUnDeployed(@RequestBody MarketDto marketDto) throws NoLoginException {

        return appMarketUserService.getUserUnDeployed(marketDto);
    }

    /**
     * 구성원관리관리-구성원목록
     *
     * @param
     * @return
     */
    @PostMapping("/list")
    public AppResponse<List<MarketDto>> getUserList(@RequestBody MarketDto marketUserDto) throws NoLoginException {
        return appMarketUserService.getUserList(marketUserDto);
    }

    /**
     * 구성원관리관리-출력
     *
     * @param
     * @return
     */
    @PostMapping("/delete")
    @RightCheck(dictCode = market_user_delete)
    public AppResponse<Boolean> deleteUser(@RequestBody MarketDto marketUserDto) throws NoLoginException {

        return appMarketUserService.deleteUser(marketUserDto);
    }

    /**
     * 구성원관리관리-역할
     *
     * @param
     * @return
     */
    @PostMapping("/role")
    @RightCheck(dictCode = market_user_role)
    public AppResponse<Boolean> roleSet(@RequestBody MarketDto marketUserDto) throws NoLoginException {

        return appMarketUserService.roleSet(marketUserDto);
    }

    /**
     * 구성원관리관리-초대-조회요소
     *
     * @param
     * @return
     */
    @PostMapping("/get/user")
    @RightCheck(dictCode = market_user_get_user)
    public AppResponse<List<MarketDto>> getUserByPhone(@RequestBody GetMarketUserByPhoneDto marketDto)
            throws NoLoginException {

        return appMarketUserService.getUserByPhone(marketDto);
    }

    /**
     * 팀관리관리-있음열기팀-조회요소
     *
     * @param
     * @return
     */
    @PostMapping("/leave/user")
    @RightCheck(dictCode = market_user_get_user)
    public AppResponse<List<MarketDto>> getUserByPhoneForOwner(@RequestBody MarketDto marketDto)
            throws NoLoginException {
        return appMarketUserService.getUserByPhoneForOwner(marketDto);
    }

    /**
     * 구성원관리관리-초대
     *
     * @param
     * @return
     */
    @PostMapping("/invite")
    @RightCheck(dictCode = market_user_invite)
    public AppResponse<Boolean> inviteUser(@RequestBody MarketDto marketDto) throws NoLoginException {

        return appMarketUserService.inviteUser(marketDto);
    }
}