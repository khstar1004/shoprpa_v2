package com.iflytek.rpa.market.controller;

import com.iflytek.rpa.market.entity.MarketDto;
import com.iflytek.rpa.market.entity.dto.AllAppListDto;
import com.iflytek.rpa.market.entity.dto.AppUpdateCheckDto;
import com.iflytek.rpa.market.entity.dto.MarketResourceDto;
import com.iflytek.rpa.market.entity.dto.ShareRobotDto;
import com.iflytek.rpa.market.service.AppMarketResourceService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.List;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 팀마켓-테이블(AppMarketResource)테이블제어
 *
 * @author mjren
 * @since 2024-10-21 14:36:30
 */
@RestController
@RequestMapping("/market-resource")
public class AppMarketResourceController {
    /**
     * 서비스객체
     */
    @Resource
    private AppMarketResourceService appMarketResourceService;

    /**
     * 봇공유까지팀마켓
     * @paramMarketResourceDto
     * @return
     * @throws Exception
     */
    @PostMapping("/share")
    public AppResponse<?> shareRobot(@Valid @RequestBody ShareRobotDto marketResourceDto) throws Exception {
        return appMarketResourceService.shareRobot(marketResourceDto);
    }

    /**
     * 가져오기
     * @paramMarketResourceDto
     * @return
     * @throws Exception
     */
    @PostMapping("/obtain")
    public AppResponse<?> obtainRobot(@RequestBody MarketResourceDto marketResourceDto) throws Exception {
        String marketId = marketResourceDto.getMarketId();
        String robotName = marketResourceDto.getAppName();
        if (StringUtils.isBlank(robotName)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "봇이름비워 둘 수 없습니다");
        }
        //        Integer editFlag = marketResourceDto.getEditFlag();
        List<String> obtainDirectory = marketResourceDto.getObtainDirection();
        if (CollectionUtils.isEmpty(obtainDirectory)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "적음가져오기 ");
        }
        if (null == marketId) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "적음마켓id");
        }
        if (null == marketResourceDto.getAppId()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "적음사용Id");
        }
        return appMarketResourceService.obtainRobot(marketResourceDto);
    }

    /**
     * 완료모듈계정목록조회
     * @param marketDto
     * @return
     * @throws Exception
     */
    @PostMapping("/deployed-user")
    public AppResponse<?> getDeployedUserList(@RequestBody MarketDto marketDto) throws Exception {
        return appMarketResourceService.getDeployedUserList(marketDto);
    }

    /**
     * 모듈(클라이언트팀마켓)
     * MarketDto
     * @return
     * @throws Exception
     */
    @PostMapping("/deploy")
    public AppResponse<?> deployRobot(@RequestBody MarketDto marketDto) throws Exception {
        return appMarketResourceService.deployRobot(marketDto);
    }

    /**
     * 업데이트-관리관리요소업데이트(클라이언트팀마켓)
     * @paramMarketResourceDto
     * @return
     * @throws Exception
     */
    @PostMapping("/update/push")
    public AppResponse<?> updateRobotByPush(@RequestBody MarketDto marketDto) throws Exception {
        return appMarketResourceService.updateRobotByPush(marketDto);
    }

    /**
     * 버전-버전목록조회
     * @paramMarketResourceDto
     * @return
     * @throws Exception
     */
    @PostMapping("/update/version-list")
    public AppResponse<?> getVersionListForApp(@RequestBody MarketDto marketDto) throws Exception {
        return appMarketResourceService.getVersionListForApp(marketDto);
    }

    /**
     * 삭제app
     * @param appId
     * @param marketId
     * @return
     * @throws Exception
     */
    @GetMapping("/delete-app")
    public AppResponse<?> deleteApp(@RequestParam String appId, @RequestParam String marketId) throws Exception {
        return appMarketResourceService.deleteApp(appId, marketId);
    }

    /**
     * 사용목록연결
     * @param allAppListDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/get-all-app-list")
    public AppResponse<?> getALlAppList(@RequestBody AllAppListDto allAppListDto) throws NoLoginException {
        return appMarketResourceService.getALlAppList(allAppListDto);
    }

    /**
     * 사용목록업데이트상태
     * @param queryDto
     * @return
     * @throws Exception
     */
    @PostMapping("/app-update-check")
    public AppResponse<?> appUpdateCheck(@RequestBody AppUpdateCheckDto queryDto) throws Exception {
        return appMarketResourceService.appUpdateCheck(queryDto);
    }

    @GetMapping("/app-detail")
    public AppResponse<?> appDetail(@RequestParam String appId, @RequestParam String marketId) throws Exception {
        return appMarketResourceService.appDetail(appId, marketId);
    }
}