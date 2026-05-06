package com.iflytek.rpa.base.controller;

import com.iflytek.rpa.base.entity.dto.*;
import com.iflytek.rpa.base.entity.vo.ModuleListVo;
import com.iflytek.rpa.base.entity.vo.OpenModuleVo;
import com.iflytek.rpa.base.entity.vo.ProcessModuleListVo;
import com.iflytek.rpa.base.service.CModuleService;
import com.iflytek.rpa.robot.entity.dto.SaveModuleDto;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import java.sql.SQLException;
import java.util.List;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/module")
public class CModuleController {

    @Resource
    private CModuleService cModuleService;

    /**
     * 프로세스및코드모듈목록
     * @param queryDto
     * @throws NoLoginException
     */
    @PostMapping("/processModuleList")
    public AppResponse<List<ProcessModuleListVo>> processModuleList(@RequestBody ProcessModuleListDto queryDto)
            throws NoLoginException {
        return cModuleService.processModuleList(queryDto);
    }

    /**
     * 코드모듈목록
     * @param queryDto
     * @throws NoLoginException
     */
    @PostMapping("/moduleList")
    public AppResponse<List<ModuleListVo>> moduleList(@RequestBody ProcessModuleListDto queryDto)
            throws NoLoginException {
        return cModuleService.moduleList(queryDto);
    }

    /**
     * 새생성코드모듈
     * @param queryDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("create")
    public AppResponse<OpenModuleVo> create(@RequestBody CreateModuleDto queryDto) throws NoLoginException {
        return cModuleService.create(queryDto);
    }

    /**
     * 새생성코드모듈이름
     * @param robotId
     * @return
     * @throws NoLoginException
     */
    @RequestMapping("newModuleName")
    public AppResponse<String> newModuleName(@RequestParam String robotId) throws NoLoginException {
        return cModuleService.newModuleName(robotId);
    }

    /**
     * 이름 변경코드모듈
     * @param queryDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("rename")
    public AppResponse<Boolean> rename(@RequestBody RenameModuleDto queryDto) throws NoLoginException {
        return cModuleService.rename(queryDto);
    }

    /**
     * 삭제모듈연결
     * @param moduleId
     * @return
     * @throws NoLoginException
     */
    @GetMapping("delete")
    public AppResponse<Boolean> delete(@RequestParam String moduleId) throws NoLoginException, SQLException {
        return cModuleService.delete(moduleId);
    }

    /**
     * 열기모듈파일
     * @param queryDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("open")
    public AppResponse<OpenModuleVo> open(@RequestBody @Valid OpenModuleDto queryDto) throws NoLoginException {
        BaseDto baseDto = new BaseDto();
        BeanUtils.copyProperties(queryDto, baseDto);
        return cModuleService.open(baseDto, queryDto.getModuleId());
    }

    /**
     * 저장지정코드모듈
     * @param queryDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("save")
    public AppResponse<Boolean> save(@RequestBody SaveModuleDto queryDto) throws NoLoginException, SQLException {
        return cModuleService.save(queryDto);
    }
}