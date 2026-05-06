package com.iflytek.rpa.base.controller;

import static com.iflytek.rpa.robot.constants.RobotConstant.EDIT_PAGE;

import com.iflytek.rpa.base.entity.dto.BaseDto;
import com.iflytek.rpa.base.entity.dto.CGlobalDto;
import com.iflytek.rpa.base.service.CGlobalVarService;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 클라이언트-전역 변수(CGlobalVar)테이블제어
 *
 * @author mjren
 * @since 2024-10-14 17:21:34
 */
@RestController
@RequestMapping("/global")
public class CGlobalVarController {
    @Autowired
    private RpaAuthFeign rpaAuthFeign;
    /**
     * 서비스객체
     */
    @Resource
    private CGlobalVarService cGlobalVarService;

    private static final Logger logger = LoggerFactory.getLogger(CGlobalVarController.class);

    @PostMapping("/all")
    public AppResponse<?> getGlobalVarInfoList(
            @RequestParam("robotId") String robotId,
            @RequestParam(required = false, name = "mode", defaultValue = EDIT_PAGE) String mode,
            @RequestParam(required = false, name = "robotVersion") Integer robotVersion)
            throws Exception {
        BaseDto baseDto = new BaseDto();
        baseDto.setRobotId(robotId);
        baseDto.setMode(mode);
        baseDto.setRobotVersion(robotVersion);
        return cGlobalVarService.getGlobalVarInfoList(baseDto);
    }

    /**
     * 생성전역 변수
     *
     * @param globalDto 전역 변수정보
     * @return AppResponse
     */
    @PostMapping("/create")
    public AppResponse<?> createGlobalVar(@RequestBody CGlobalDto globalDto) throws Exception {
        if (globalDto.getRobotId() == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        return cGlobalVarService.createGlobalVar(globalDto);
    }

    /**
     * 저장전역 변수
     *
     * @param globalDto 전역 변수정보
     * @return AppResponse
     */
    @PostMapping("/save")
    public AppResponse<?> saveGlobalVar(@RequestBody CGlobalDto globalDto) throws Exception {
        if (globalDto.getRobotId() == null || globalDto.getGlobalId() == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        AppResponse<User> resp = rpaAuthFeign.getLoginUser();
        if (resp == null || !resp.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = resp.getData();
        String userId = loginUser.getId();

        globalDto.setUpdaterId(String.valueOf(userId));

        return cGlobalVarService.saveGlobalVar(globalDto);
    }

    /**
     * 가져오기전역 변수이름목록
     *
     * @param robotId 봇ID
     * @return AppResponse
     */
    @PostMapping("/name-list")
    public AppResponse<?> getGlobalVarNameList(@RequestParam("robotId") String robotId) throws Exception {
        return cGlobalVarService.getGlobalVarNameList(robotId);
    }

    /**
     * 삭제전역 변수
     *
     * @param globalDto 전역 변수정보
     * @return AppResponse
     */
    @PostMapping("/delete")
    public AppResponse<?> deleteGlobalVar(@RequestBody CGlobalDto globalDto) throws Exception {
        if (globalDto.getRobotId() == null || globalDto.getGlobalId() == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        return cGlobalVarService.deleteGlobalVar(globalDto);
    }
}