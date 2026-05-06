package com.iflytek.rpa.base.controller;

import com.iflytek.rpa.base.entity.dto.BaseDto;
import com.iflytek.rpa.base.entity.dto.CRequireDeleteDto;
import com.iflytek.rpa.base.entity.dto.CRequireDto;
import com.iflytek.rpa.base.service.CRequireService;
import com.iflytek.rpa.utils.response.AppResponse;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * python관리관리(CRequire)테이블제어
 *
 * @author mjren
 * @since 2024-10-14 17:21:34
 */
@RestController
@RequestMapping("/require")
public class CRequireController {
    /**
     * 서비스객체
     */
    @Resource
    private CRequireService cRequireService;

    @PostMapping("/list")
    public AppResponse<?> getRequireInfoList(@RequestBody @Valid BaseDto baseDto) throws Exception {
        return cRequireService.getRequireInfoList(baseDto);
    }

    /**
     * 추가python패키지관리관리
     *
     * @param crequireDto 정보
     * @return 결과
     */
    @PostMapping("/add")
    public AppResponse<?> addProject(@RequestBody @Valid CRequireDto crequireDto) throws Exception {
        return cRequireService.addRequire(crequireDto);
    }

    /**
     * 삭제python패키지관리관리
     *
     * @param cRequireDeleteDto 정보
     * @return 결과
     */
    @PostMapping("/delete")
    public AppResponse<?> deleteProject(@RequestBody @Valid CRequireDeleteDto cRequireDeleteDto) throws Exception {
        return cRequireService.deleteProject(cRequireDeleteDto);
    }

    /**
     * 업데이트python패키지관리관리
     *
     * @param crequireDto 정보
     * @return 결과
     */
    @PostMapping("/update")
    public AppResponse<?> updateRequire(@RequestBody @Valid CRequireDto crequireDto) throws Exception {
        return cRequireService.updateRequire(crequireDto);
    }
}