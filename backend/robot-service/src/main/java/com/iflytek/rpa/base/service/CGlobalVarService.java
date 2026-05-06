package com.iflytek.rpa.base.service;

import com.iflytek.rpa.base.entity.dto.BaseDto;
import com.iflytek.rpa.base.entity.dto.CGlobalDto;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;

/**
 * 클라이언트-전역 변수(CGlobalVar)테이블서비스연결
 *
 * @author mjren
 * @since 2024-10-14 17:21:34
 */
public interface CGlobalVarService {

    AppResponse<?> getGlobalVarInfoList(BaseDto baseDto) throws NoLoginException;

    AppResponse<?> createGlobalVar(CGlobalDto globalDto) throws NoLoginException;

    AppResponse<?> saveGlobalVar(CGlobalDto globalDto);

    AppResponse<?> getGlobalVarNameList(String robotId) throws NoLoginException;

    AppResponse<?> deleteGlobalVar(CGlobalDto globalDto) throws NoLoginException;
}