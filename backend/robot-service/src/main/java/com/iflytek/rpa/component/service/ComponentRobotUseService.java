package com.iflytek.rpa.component.service;

import com.iflytek.rpa.component.entity.dto.*;
import com.iflytek.rpa.component.entity.vo.ComponentUseVo;
import com.iflytek.rpa.component.entity.vo.EditCompUseVo;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;

/**
 * 봇컴포넌트사용테이블(ComponentRobotUse)테이블서비스연결
 *
 * @author makejava
 * @since 2024-12-19
 */
public interface ComponentRobotUseService {

    AppResponse<List<ComponentUseVo>> getComponentUse(GetComponentUseDto getComponentUseDto) throws NoLoginException;

    /**
     * 추가컴포넌트사용
     * @param addCompUseDto 추가컴포넌트사용DTO
     * @return 결과
     * @throws NoLoginException
     */
    AppResponse<String> addComponentUse(AddCompUseDto addCompUseDto) throws NoLoginException;

    /**
     * 삭제컴포넌트사용
     * @param delComponentUseDto 삭제컴포넌트사용DTO
     * @return 결과
     * @throws NoLoginException
     */
    AppResponse<String> deleteComponentUse(DelComponentUseDto delComponentUseDto) throws NoLoginException;

    /**
     * 업데이트컴포넌트사용버전
     * @param updateComponentUseDto 업데이트컴포넌트사용DTO
     * @return 결과
     * @throws NoLoginException
     */
    AppResponse<String> updateComponentUse(UpdateComponentUseDto updateComponentUseDto) throws NoLoginException;

    /**
     * 근거컴포넌트ID및버전조회프로세스ID
     * @param componentId
     * @param componentVersion
     * @return 프로세스ID
     * @throws NoLoginException
     */
    AppResponse<String> getProcessId(String componentId, Integer componentVersion) throws NoLoginException;

    AppResponse<EditCompUseVo> getEditCompUse(EditCompUseDto queryDto) throws NoLoginException;
}