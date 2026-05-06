package com.iflytek.rpa.component.controller;

import static com.iflytek.rpa.robot.constants.RobotConstant.EDIT_PAGE;

import com.iflytek.rpa.component.entity.dto.*;
import com.iflytek.rpa.component.entity.vo.ComponentUseVo;
import com.iflytek.rpa.component.entity.vo.EditCompUseVo;
import com.iflytek.rpa.component.service.ComponentRobotUseService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 봇컴포넌트사용테이블(ComponentRobotUse)테이블제어
 *
 * @author makejava
 * @since 2024-12-19
 */
@RestController
@RequestMapping("/component-robot-use")
public class ComponentRobotUseController {

    @Autowired
    private ComponentRobotUseService componentRobotUseService;

    /**
     * 봇사용의컴포넌트id 및 의version
     * @param getComponentUseDto 조회컴포넌트사용DTO
     * @return 컴포넌트사용목록
     * @throws NoLoginException
     */
    @PostMapping("/component-use")
    public AppResponse<List<ComponentUseVo>> getComponentUse(@RequestBody GetComponentUseDto getComponentUseDto)
            throws NoLoginException {
        return componentRobotUseService.getComponentUse(getComponentUseDto);
    }

    /**
     * 추가컴포넌트사용
     * @param addCompUseDto 추가컴포넌트사용DTO
     * @return 결과
     * @throws NoLoginException
     */
    @PostMapping("/add")
    public AppResponse<String> addComponentUse(@RequestBody AddCompUseDto addCompUseDto) throws NoLoginException {
        return componentRobotUseService.addComponentUse(addCompUseDto);
    }

    /**
     * 삭제컴포넌트사용
     * @param delComponentUseDto 삭제컴포넌트사용DTO
     * @return 결과
     * @throws NoLoginException
     */
    @PostMapping("/delete")
    public AppResponse<String> deleteComponentUse(@RequestBody DelComponentUseDto delComponentUseDto)
            throws NoLoginException {
        return componentRobotUseService.deleteComponentUse(delComponentUseDto);
    }

    /**
     * 업데이트컴포넌트사용버전
     * @param updateComponentUseDto 업데이트컴포넌트사용DTO
     * @return 결과
     * @throws NoLoginException
     */
    @PostMapping("/update")
    public AppResponse<String> updateComponentUse(@RequestBody UpdateComponentUseDto updateComponentUseDto)
            throws NoLoginException {
        return componentRobotUseService.updateComponentUse(updateComponentUseDto);
    }

    /**
     * 근거컴포넌트ID및버전조회프로세스ID
     * @param componentId 컴포넌트ID
     * @param componentVersion 컴포넌트버전
     * @return 프로세스ID
     * @throws NoLoginException
     */
    @GetMapping("/process-id")
    public AppResponse<String> getProcessIdByComponentIdAndVersion(
            @RequestParam("componentId") String componentId, @RequestParam("componentVersion") Integer componentVersion)
            throws NoLoginException {

        if (StringUtils.isBlank(componentId)) {
            throw new ServiceException(ErrorCodeEnum.E_PARAM_LOSE.getCode(), "컴포넌트ID비워 둘 수 없습니다");
        }
        if (componentVersion == null) {
            throw new ServiceException(ErrorCodeEnum.E_PARAM_LOSE.getCode(), "컴포넌트버전비워 둘 수 없습니다");
        }

        return componentRobotUseService.getProcessId(componentId, componentVersion);
    }

    /**
     * 가져오기컴포넌트의닫기정보
     * @param queryDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/edit")
    public AppResponse<EditCompUseVo> getEditCompUse(@RequestBody EditCompUseDto queryDto) throws NoLoginException {
        if (!queryDto.getMode().equals(EDIT_PAGE)) throw new ServiceException("연결에서요청 ");
        return componentRobotUseService.getEditCompUse(queryDto);
    }
}