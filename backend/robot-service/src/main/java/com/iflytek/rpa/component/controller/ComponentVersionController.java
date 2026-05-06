package com.iflytek.rpa.component.controller;

import com.iflytek.rpa.component.entity.dto.CreateVersionDto;
import com.iflytek.rpa.component.service.ComponentVersionService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 컴포넌트버전테이블(ComponentVersion)테이블제어
 *
 * @author makejava
 * @since 2024-12-19
 */
@RestController
@RequestMapping("/component-version")
public class ComponentVersionController {

    @Resource
    private ComponentVersionService componentVersionService;

    /**
     * 발송버전
     * @param createVersionDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("create")
    AppResponse<Boolean> createComponentVersion(@RequestBody CreateVersionDto createVersionDto)
            throws NoLoginException {

        return componentVersionService.createComponentVersion(createVersionDto);
    }

    /**
     * 가져오기컴포넌트아래일개버전
     * @param componentId 컴포넌트ID
     * @return 아래일개버전
     * @throws NoLoginException
     */
    @GetMapping("next-version")
    AppResponse<Integer> getNextVersionNumber(@RequestParam("componentId") String componentId) throws NoLoginException {
        return componentVersionService.getNextVersionNumber(componentId);
    }
}