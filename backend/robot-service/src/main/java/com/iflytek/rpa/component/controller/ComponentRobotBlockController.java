package com.iflytek.rpa.component.controller;

import com.iflytek.rpa.component.entity.dto.AddRobotBlockDto;
import com.iflytek.rpa.component.entity.dto.GetRobotBlockDto;
import com.iflytek.rpa.component.service.ComponentRobotBlockService;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 봇컴포넌트테이블(ComponentRobotBlock)테이블제어
 *
 * @author makejava
 * @since 2024-12-19
 */
@RestController
@RequestMapping("/component-robot-block")
public class ComponentRobotBlockController {

    @Autowired
    private ComponentRobotBlockService componentRobotBlockService;

    /**
     * 추가봇컴포넌트의기록 - 제거
     * @param addRobotBlockDto 추가기록요청 매개변수
     * @return 결과
     * @throws Exception 예외정보
     */
    @PostMapping("/add")
    public AppResponse<Boolean> addRobotBlock(@RequestBody @Valid AddRobotBlockDto addRobotBlockDto) throws Exception {
        return componentRobotBlockService.addRobotBlock(addRobotBlockDto);
    }

    /**
     * 삭제봇컴포넌트의기록 - 설치
     * @param addRobotBlockDto 삭제기록요청 매개변수
     * @return 결과
     * @throws Exception 예외정보
     */
    @PostMapping("/delete")
    public AppResponse<Boolean> deleteRobotBlock(@RequestBody @Valid AddRobotBlockDto addRobotBlockDto)
            throws Exception {
        return componentRobotBlockService.deleteRobotBlock(addRobotBlockDto);
    }

    /**
     * 가져오기봇의컴포넌트ID목록
     * @param queryDto
     * @return 의컴포넌트ID목록
     * @throws Exception 예외정보
     */
    @PostMapping("/blocked-components")
    public AppResponse<List<String>> getBlockedComponentIds(@RequestBody @Valid GetRobotBlockDto queryDto)
            throws Exception {
        return componentRobotBlockService.getBlockedComponentIds(queryDto);
    }
}