package com.iflytek.rpa.base.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.iflytek.rpa.base.entity.dto.CAtomMetaNewListDto;
import com.iflytek.rpa.base.entity.vo.CAtomMetaNewVo;
import com.iflytek.rpa.base.service.CAtomMetaNewService;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 새기존가능연결
 */
@RestController
@RequestMapping("/atom-new")
public class CAtomMetaNewController {

    @Autowired
    private CAtomMetaNewService cAtomMetaNewService;

    /**
     * 가져오기기존가능
     */
    @PostMapping("/tree")
    public AppResponse<String> getAtomTree() throws JsonProcessingException {
        return cAtomMetaNewService.getAtomTree();
    }

    /**
     * 업데이트기존가능
     */
    @PostMapping("/update-tree")
    public AppResponse<String> updateAtomTree(@RequestBody String treeContent) {
        return cAtomMetaNewService.updateAtomTree(treeContent);
    }

    /**
     * 근거key목록가져오기기존가능
     */
    @PostMapping("/list")
    public AppResponse<List<CAtomMetaNewVo>> getListByKeys(@Valid @RequestBody CAtomMetaNewListDto dto) {
        return cAtomMetaNewService.getListByKeys(dto.getKeys());
    }

    /**
     * 가져오기전체량기존가능
     */
    @PostMapping("/all")
    public List<CAtomMetaNewVo> getAll() {
        return cAtomMetaNewService.getAll();
    }

    /**
     * 전체량업데이트기존가능
     * 결과가있음이면행업데이트, 있음이면행삽입, 결과가발송새의데이터베이스찾을 수 없습니다이면삭제해당기록
     */
    @PostMapping("/all-update")
    public AppResponse<String> allUpdate(@RequestBody List<CAtomMetaNewVo> atomMetaNewVoList) {
        return cAtomMetaNewService.allUpdate(atomMetaNewVoList);
    }
}