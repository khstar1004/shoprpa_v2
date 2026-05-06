package com.iflytek.rpa.component.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iflytek.rpa.base.entity.CProcess;
import com.iflytek.rpa.component.entity.dto.CheckNameDto;
import com.iflytek.rpa.component.entity.dto.ComponentListDto;
import com.iflytek.rpa.component.entity.dto.EditPageCompInfoDto;
import com.iflytek.rpa.component.entity.dto.GetComponentUseDto;
import com.iflytek.rpa.component.entity.vo.*;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;

/**
 * 컴포넌트테이블(Component)테이블서비스연결
 *
 * @author makejava
 * @since 2024-12-19
 */
public interface ComponentService {

    /**
     * 생성컴포넌트
     */
    AppResponse<CProcess> createComponent(String componentName) throws NoLoginException;

    /**
     * 삭제컴포넌트
     */
    AppResponse<Boolean> deleteComponent(String componentId) throws NoLoginException;

    /**
     * 이름 변경컴포넌트
     */
    AppResponse<Boolean> renameComponent(String componentId, String newName) throws NoLoginException;

    /**
     * 조회컴포넌트이름여부재복사
     */
    AppResponse<Boolean> checkNameDuplicate(CheckNameDto checkNameDto) throws NoLoginException;

    /**
     * 새생성컴포넌트이름
     */
    AppResponse<String> createComponentName() throws NoLoginException;

    /**
     * 가져오기컴포넌트
     * @param componentId 컴포넌트ID
     * @return 컴포넌트정보
     * @throws NoLoginException
     */
    AppResponse<ComponentInfoVo> getComponentInfo(String componentId) throws NoLoginException;

    AppResponse<Boolean> copyComponent(String componentId, String name) throws Exception;

    /**
     * 생성본컴포넌트이름
     * @param componentId 기존컴포넌트ID
     * @return 새의컴포넌트이름
     * @throws Exception
     */
    AppResponse<String> copyCreateName(String componentId) throws Exception;

    /**
     * 분조회컴포넌트목록
     * @param componentListDto 조회파일
     * @return 분컴포넌트목록
     * @throws Exception
     */
    AppResponse<IPage<ComponentVo>> getComponentPageList(ComponentListDto componentListDto) throws Exception;

    /**
     * 가져오기 컴포넌트목록
     * @param queryDto 조회파일
     * @return 컴포넌트목록
     * @throws Exception
     */
    AppResponse<List<EditingPageCompVo>> getEditingPageCompList(GetComponentUseDto queryDto) throws Exception;

    /**
     * 가져오기 컴포넌트
     * @param queryDto 조회파일
     * @return 컴포넌트
     * @throws Exception
     */
    AppResponse<EditingPageCompInfoVo> getEditingPageCompInfo(EditPageCompInfoDto queryDto) throws Exception;

    /**
     * 가져오기컴포넌트관리관리목록
     * @param queryDto 조회파일
     * @return 컴포넌트관리관리목록
     * @throws Exception
     */
    AppResponse<List<CompManageVo>> getCompManageList(GetComponentUseDto queryDto) throws Exception;
}