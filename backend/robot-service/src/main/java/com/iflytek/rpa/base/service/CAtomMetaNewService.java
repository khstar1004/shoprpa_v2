package com.iflytek.rpa.base.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.iflytek.rpa.base.entity.vo.CAtomMetaNewVo;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;

/**
 * 새기존가능Service
 */
public interface CAtomMetaNewService {

    /**
     * 가져오기기존가능
     */
    AppResponse<String> getAtomTree() throws JsonProcessingException;

    /**
     * 근거key목록가져오기기존가능d
     */
    AppResponse<List<CAtomMetaNewVo>> getListByKeys(List<String> keys);

    /**
     * 가져오기전체량기존가능J
     */
    List<CAtomMetaNewVo> getAll();

    /**
     * 전체량업데이트기존가능
     * 결과가있음이면행업데이트, 있음이면행삽입, 결과가발송새의데이터베이스찾을 수 없습니다이면삭제해당기록
     */
    AppResponse<String> allUpdate(List<CAtomMetaNewVo> atomMetaNewVoList);

    AppResponse<String> updateAtomTree(String treeContent);
}