package com.iflytek.rpa.component.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iflytek.rpa.base.entity.CProcess;
import com.iflytek.rpa.component.entity.dto.CheckNameDto;
import com.iflytek.rpa.component.entity.dto.ComponentListDto;
import com.iflytek.rpa.component.entity.dto.EditPageCompInfoDto;
import com.iflytek.rpa.component.entity.dto.GetComponentUseDto;
import com.iflytek.rpa.component.entity.vo.*;
import com.iflytek.rpa.component.service.ComponentService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 컴포넌트테이블(Component)테이블제어
 *
 * @author makejava
 * @since 2024-12-19
 */
@RestController
@RequestMapping("/component")
public class ComponentController {

    @Autowired
    private ComponentService componentService;

    /**
     * 새생성컴포넌트-가져오기 컴포넌트이름
     * @return
     * @throws Exception
     */
    @PostMapping("/create-name")
    public AppResponse<String> createComponentName() throws Exception {
        return componentService.createComponentName();
    }

    /**
     * 생성컴포넌트
     */
    @GetMapping("/create")
    public AppResponse<CProcess> createComponent(@RequestParam("componentName") String componentName) throws Exception {
        return componentService.createComponent(componentName);
    }

    /**
     * 삭제컴포넌트
     * 예에서목록 의, is_shown로0, 아니요예의삭제
     */
    @GetMapping("/delete")
    public AppResponse<Boolean> deleteComponent(@RequestParam("componentId") String componentId) throws Exception {
        return componentService.deleteComponent(componentId);
    }

    /**
     * 이름 변경컴포넌트
     */
    @GetMapping("/rename")
    public AppResponse<Boolean> renameComponent(@RequestParam String componentId, @RequestParam String newName)
            throws Exception {
        return componentService.renameComponent(componentId, newName);
    }

    /**
     * 조회컴포넌트이름여부재복사, 
     * 수정이름문자의시 componentId
     * 새생성의시아니요 componentId
     * @param checkNameDto
     * @return
     * @throws Exception
     */
    @PostMapping("/check-name")
    public AppResponse<Boolean> checkNameDuplicate(@RequestBody @Valid CheckNameDto checkNameDto) throws Exception {
        return componentService.checkNameDuplicate(checkNameDto);
    }

    /**
     * 가져오기컴포넌트(목록 의버전)
     * @param componentId 컴포넌트ID
     * @return 컴포넌트정보
     * @throws NoLoginException
     */
    @GetMapping("/info")
    public AppResponse<ComponentInfoVo> getComponentInfo(@RequestParam("componentId") String componentId)
            throws NoLoginException {
        return componentService.getComponentInfo(componentId);
    }

    /**
     * 생성본
     * @param componentId
     * @param name
     * @return
     * @throws NoLoginException
     */
    @GetMapping("/copy")
    public AppResponse<Boolean> copyComponent(
            @RequestParam("componentId") String componentId, @RequestParam("name") String name) throws Exception {
        return componentService.copyComponent(componentId, name);
    }

    /**
     * 생성본컴포넌트이름
     * 근거기존컴포넌트이름완료새의본이름, 추가"-본"후, 결과가재이름이면추가숫자
     * @param componentId 기존컴포넌트ID
     * @return 새의컴포넌트이름
     * @throws Exception
     */
    @GetMapping("/copy/create-name")
    public AppResponse<String> copyCreateName(@RequestParam("componentId") String componentId) throws Exception {
        return componentService.copyCreateName(componentId);
    }

    /**
     * 분조회컴포넌트목록
     * @param componentListDto 조회파일
     * @return 분컴포넌트목록
     * @throws Exception
     */
    @PostMapping("/page-list")
    public AppResponse<IPage<ComponentVo>> getComponentPageList(@RequestBody @Valid ComponentListDto componentListDto)
            throws Exception {
        return componentService.getComponentPageList(componentListDto);
    }

    /**
     * 봇왼쪽컴포넌트목록
     * @param queryDto
     * @return
     * @throws Exception
     */
    @PostMapping("/editing/list")
    public AppResponse<List<EditingPageCompVo>> getEditingPageCompList(@RequestBody GetComponentUseDto queryDto)
            throws Exception {
        return componentService.getEditingPageCompList(queryDto);
    }

    /**
     * 의컴포넌트
     * @param queryDto
     * @return
     */
    @PostMapping("/editing/info")
    public AppResponse<EditingPageCompInfoVo> editingPageCompInfo(@RequestBody EditPageCompInfoDto queryDto)
            throws Exception {
        return componentService.getEditingPageCompInfo(queryDto);
    }

    /**
     * 컴포넌트관리관리목록
     * @param queryDto
     * @return
     * @throws Exception
     */
    @PostMapping("/editing/manage-list")
    public AppResponse<List<CompManageVo>> CompManageList(@RequestBody GetComponentUseDto queryDto) throws Exception {
        return componentService.getCompManageList(queryDto);
    }
}