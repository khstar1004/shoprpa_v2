package com.iflytek.rpa.component.service;

import com.iflytek.rpa.component.entity.dto.CreateVersionDto;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;

/**
 * 컴포넌트버전테이블(ComponentVersion)테이블서비스연결
 *
 * @author makejava
 * @since 2024-12-19
 */
public interface ComponentVersionService {

    /**
     * 생성컴포넌트버전
     */
    AppResponse<Boolean> createComponentVersion(CreateVersionDto createVersionDto) throws NoLoginException;

    /**
     * 가져오기컴포넌트아래일개버전(새버전+1, 결과가있음버전이면반환1)
     */
    AppResponse<Integer> getNextVersionNumber(String componentId) throws NoLoginException;
}