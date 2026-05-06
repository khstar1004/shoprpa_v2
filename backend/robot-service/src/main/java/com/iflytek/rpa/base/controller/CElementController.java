package com.iflytek.rpa.base.controller;

import static com.iflytek.rpa.robot.constants.RobotConstant.EDIT_PAGE;

import com.iflytek.rpa.base.entity.CElement;
import com.iflytek.rpa.base.entity.dto.FrontElementCreateDto;
import com.iflytek.rpa.base.entity.dto.FrontElementDto;
import com.iflytek.rpa.base.entity.dto.ServerBaseDto;
import com.iflytek.rpa.base.service.CElementService;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import javax.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 요소또는이미지정보
 *
 * @author mjren
 * @since 2024-10-14 17:21:34
 */
@RestController
@RequestMapping("element")
public class CElementController {
    /**
     * 서비스객체
     */
    @Resource
    private CElementService cElementService;

    //    /**
    //     * 가져오기원이름목록
    //     * @param robotId
    //     * @param mode
    //     * @return
    //     * @throws Exception
    //     */
    //    @PostMapping("/all")
    //    public AppResponse<?> getElementNameList(@RequestParam("robotId") String robotId, @RequestParam(required =
    // false, name = "mode", defaultValue = EDIT_PAGE) String mode) throws Exception {
    //        BaseDto baseDto = new BaseDto();
    //        baseDto.setRobotId(robotId);
    //        baseDto.setMode(mode);
    //        return cElementService.getElementNameList(baseDto);
    //    }

    /**
     * 요소, 이미지-조회
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/detail")
    public AppResponse<?> getElementDetail(
            @RequestParam("robotId") String robotId,
            @RequestParam("elementId") String elementId,
            @RequestParam(required = false, name = "mode", defaultValue = EDIT_PAGE) String mode,
            @RequestParam(required = false, name = "robotVersion") Integer robotVersion)
            throws Exception {
        ServerBaseDto serverBaseDto = new ServerBaseDto();
        serverBaseDto.setRobotId(robotId);
        serverBaseDto.setElementId(elementId);
        serverBaseDto.setRobotVersion(robotVersion);
        serverBaseDto.setMode(mode);
        return cElementService.getElementDetail(serverBaseDto);
    }

    /**
     * 요소, 이미지-분그룹
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/move")
    public AppResponse<?> moveElementOrImage(
            @RequestParam("robotId") String robotId,
            @RequestParam("elementId") String elementId,
            @RequestParam("groupId") String groupId)
            throws Exception {
        ServerBaseDto serverBaseDto = new ServerBaseDto();
        serverBaseDto.setRobotId(robotId);
        serverBaseDto.setRobotVersion(0);
        serverBaseDto.setElementId(elementId);
        serverBaseDto.setGroupId(groupId);
        return cElementService.moveElementOrImage(serverBaseDto);
    }

    /**
     *  요소, 이미지-삭제
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/delete")
    public AppResponse<?> deleteElementOrImage(
            @RequestParam("robotId") String robotId, @RequestParam("elementId") String elementId) throws Exception {
        ServerBaseDto serverBaseDto = new ServerBaseDto();
        serverBaseDto.setRobotId(robotId);
        serverBaseDto.setRobotVersion(0);
        serverBaseDto.setElementId(elementId);
        return cElementService.deleteElementOrImage(serverBaseDto);
    }

    /**
     * 완료이미지이름
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/image/create-name")
    public AppResponse<?> createImageName(@RequestParam("robotId") String robotId) throws Exception {
        ServerBaseDto serverBaseDto = new ServerBaseDto();
        serverBaseDto.setRobotId(robotId);
        serverBaseDto.setRobotVersion(0);
        return cElementService.createImageName(serverBaseDto);
    }

    /**
     * 요소, 이미지-생성
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/create")
    public AppResponse<?> createElement(@RequestBody FrontElementCreateDto frontElementCreateDto) throws Exception {
        if (null == frontElementCreateDto.getType()
                || null == frontElementCreateDto.getRobotId()
                || null == frontElementCreateDto.getGroupName()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        ServerBaseDto serverBaseDto = new ServerBaseDto();
        transFrontParamToServerParamForElement(frontElementCreateDto, serverBaseDto);
        return cElementService.createElement(serverBaseDto);
    }

    public void transFrontParamToServerParamForElement(
            FrontElementCreateDto frontElementCreateDto, ServerBaseDto serverBaseDto) {
        serverBaseDto.setElementType(frontElementCreateDto.getType());
        serverBaseDto.setRobotId(frontElementCreateDto.getRobotId());
        serverBaseDto.setRobotVersion(0);
        serverBaseDto.setGroupName(frontElementCreateDto.getGroupName());
        CElement cElement = new CElement();
        FrontElementDto frontElementDto = frontElementCreateDto.getElement();
        BeanUtils.copyProperties(frontElementDto, cElement);
        cElement.setElementId(frontElementDto.getId());
        cElement.setElementName(frontElementDto.getName());
        cElement.setCommonSubType(frontElementDto.getCommonSubType());
        cElement.setRobotId(serverBaseDto.getRobotId());
        cElement.setRobotVersion(0);
        serverBaseDto.setElement(cElement);
    }

    /**
     * 요소, 이미지-업데이트정보
     * @param frontElementCreateDto
     * @return
     * @throws Exception
     */
    @PostMapping("/update")
    public AppResponse<?> updateElement(@RequestBody FrontElementCreateDto frontElementCreateDto) throws Exception {
        if (null == frontElementCreateDto.getRobotId() || null == frontElementCreateDto.getElement()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        ServerBaseDto serverBaseDto = new ServerBaseDto();
        transFrontParamToServerParamForElement(frontElementCreateDto, serverBaseDto);
        return cElementService.updateElement(serverBaseDto);
    }

    /**
     * 요소-생성본
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/copy")
    public AppResponse<?> copyElement(
            @RequestParam("robotId") String robotId, @RequestParam("elementId") String elementId) throws Exception {
        // 에서현재분그룹아래생성본
        ServerBaseDto serverBaseDto = new ServerBaseDto();
        serverBaseDto.setRobotId(robotId);
        serverBaseDto.setRobotVersion(0);
        serverBaseDto.setElementId(elementId);
        return cElementService.copyElement(serverBaseDto);
    }

    /**
     * 요소, 이미지-조회모든분그룹이름그룹내부원정보
     * @param robotId
     * @param elementType
     * @return
     * @throws Exception
     */
    @PostMapping("/all")
    public AppResponse<?> getAllGroupInfo(
            @RequestParam(name = "robotId") String robotId,
            @RequestParam("elementType") String elementType,
            @RequestParam(required = false, name = "mode", defaultValue = EDIT_PAGE) String mode)
            throws Exception {
        ServerBaseDto serverBaseDto = new ServerBaseDto();
        serverBaseDto.setRobotId(robotId);
        serverBaseDto.setElementType(elementType);
        serverBaseDto.setMode(mode);
        return cElementService.getAllGroupInfo(serverBaseDto);
    }
}