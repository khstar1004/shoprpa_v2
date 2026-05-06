package com.iflytek.rpa.base.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.iflytek.rpa.base.entity.dto.CParamDto;
import com.iflytek.rpa.base.entity.dto.CParamListDto;
import com.iflytek.rpa.base.entity.dto.ParamDto;
import com.iflytek.rpa.base.entity.dto.QueryParamDto;
import com.iflytek.rpa.base.service.CParamService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.List;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 매개변수관리관리
 */
@RestController
@RequestMapping("/param")
public class CParamController {
    @Resource
    private CParamService cParamService;

    /**
     * 조회프로세스매개변수
     * @param
     * @return
     */
    @PostMapping("/all")
    public AppResponse<List<ParamDto>> getAllParams(@RequestBody @Valid QueryParamDto queryParamDto)
            throws JsonProcessingException, NoLoginException {
        // 봇robotId여부로null
        if (StringUtils.isBlank(queryParamDto.getRobotId())) {
            throw new ServiceException((ErrorCodeEnum.E_SQL.getCode()), "로봇 ID는 비워 둘 수 없습니다");
        }
        return cParamService.getAllParams(queryParamDto);
    }

    /**
     * 추가프로세스매개변수
     * @param ParamDto
     * @return
     */
    @PostMapping("/add")
    public AppResponse<String> addParam(@RequestBody @Valid CParamDto ParamDto) throws NoLoginException {

        return cParamService.addParam(ParamDto);
    }

    /**
     * 삭제프로세스매개변수
     * @param id
     * @return
     */
    @PostMapping("/delete")
    public AppResponse<Boolean> deleteParam(@RequestParam(value = "id") String id) throws NoLoginException {
        // id여부비어 있습니다
        if (StringUtils.isBlank(id)) {
            throw new ServiceException((ErrorCodeEnum.E_SQL.getCode()), "매개변수 ID는 비워 둘 수 없습니다");
        }
        return cParamService.deleteParam(id);
    }

    /**
     * 수정프로세스매개변수
     * @param paramDto
     * @return
     */
    @PostMapping("/update")
    public AppResponse<Boolean> updateParam(@Valid @RequestBody CParamDto paramDto) throws NoLoginException {
        return cParamService.updateParam(paramDto);
    }

    /**
     * 저장사용자지정매개변수
     * @param paramListDto
     * @return
     * @throws NoLoginException
     * @throws JsonProcessingException
     */
    @PostMapping("/saveUserParam")
    public AppResponse<Boolean> saveUserParam(@RequestBody CParamListDto paramListDto)
            throws NoLoginException, JsonProcessingException {

        return cParamService.saveUserParam(paramListDto);
    }
}
