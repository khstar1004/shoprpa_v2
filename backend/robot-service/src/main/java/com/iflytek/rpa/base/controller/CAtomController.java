package com.iflytek.rpa.base.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.iflytek.rpa.base.entity.AtomCommon;
import com.iflytek.rpa.base.entity.dto.AtomKeyListDto;
import com.iflytek.rpa.base.entity.dto.AtomListDto;
import com.iflytek.rpa.base.entity.dto.SaveAtomicsDto;
import com.iflytek.rpa.base.service.CAtomMetaService;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 기존가능지정정보
 */
@RestController
@RequestMapping("/atom")
public class CAtomController {

    @Autowired
    private CAtomMetaService cAtomMetaService;

    /**
     * 가져오기기존가능단계닫기시스템및공유정보
     */
    @PostMapping("/tree")
    public AppResponse<?> getAtomTree() {
        return cAtomMetaService.getAtomTree("atomCommon");
    }

    /**
     * 조회지정디렉터리의기존가능지정
     */
    @PostMapping("/getListByParentKey")
    public AppResponse<?> getAtomListByParentKey(@RequestParam(name = "parentKey") String parentKey) {
        return cAtomMetaService.getAtomListByParentKey(parentKey);
    }

    /**
     * 근거key및version목록 량가져오기기존가능지정
     */
    @PostMapping("/getByVersionList")
    public AppResponse<?> getAtomList(@RequestBody AtomListDto atomListDto) throws Exception {
        return cAtomMetaService.getAtomList(atomListDto);
    }

    /**
     * 근거key조회단일개기존가능새의지정
     */
    @PostMapping("/getLatestAtomByKey")
    public AppResponse<?> getLatestAtomByKey(@RequestParam(name = "key") String atomKey) throws Exception {
        return cAtomMetaService.getLatestAtomByKey(atomKey);
    }
    /**
     * 근거 List[key] 량조회기존가능새지정
     */
    @PostMapping("/getLatestAtomsByList")
    public AppResponse<?> getLatestAtomsByList(@RequestBody AtomKeyListDto dto) throws Exception {
        return cAtomMetaService.getLatestAtomsByList(dto);
    }

    /**
     * 추가기존가능공유데이터(types, commonAdvancedParameter, atomicTree, atomicTreeExtend)
     */
    @PostMapping("/add-common")
    public AppResponse<?> addAtomCommonInfo(@Valid @RequestBody AtomCommon atomCommon) throws JsonProcessingException {
        // todo 암호화코드, 제한

        return cAtomMetaService.addAtomCommonInfo(atomCommon);
    }

    /**
     *
     * 업데이트기존가능공유데이터(types, commonAdvancedParameter, atomicTree, atomicTreeExtend)
     *
     * @param atomCommon
     * @return
     */
    @PostMapping("/update-common")
    public AppResponse<?> updateAtomCommonInfo(@Valid @RequestBody AtomCommon atomCommon)
            throws JsonProcessingException {
        // todo 암호화코드, 제한
        return cAtomMetaService.updateAtomCommonInfo(atomCommon);
    }

    /**
     * 삽입또는업데이트기존가능지정정보(atomics), 저장입력DB
     */
    @PostMapping("/save-atomics")
    public AppResponse<?> saveAtomicsInfo(@RequestBody SaveAtomicsDto saveAtomicsDto) throws Exception {

        return cAtomMetaService.saveAtomicsInfo(saveAtomicsDto.getAtomMap(), saveAtomicsDto.getSaveWay());
    }

    /**
     * 기존가능새지정전체량조회연결
     */
    @GetMapping("/getLatestAllAtoms")
    public Map getLatestAllAtoms() throws Exception {
        return cAtomMetaService.getLatestAllAtoms();
    }
}