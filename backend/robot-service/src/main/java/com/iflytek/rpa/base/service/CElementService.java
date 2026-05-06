package com.iflytek.rpa.base.service;

import com.iflytek.rpa.base.entity.dto.ServerBaseDto;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;

/**
 * 클라이언트, 원정보(CElement)테이블서비스연결
 *
 * @author mjren
 * @since 2024-10-14 17:21:34
 */
public interface CElementService {

    AppResponse<?> getElementDetail(ServerBaseDto serverBaseDto) throws NoLoginException;

    AppResponse<?> moveElementOrImage(ServerBaseDto serverBaseDto) throws NoLoginException;

    AppResponse<?> deleteElementOrImage(ServerBaseDto serverBaseDto) throws NoLoginException;

    AppResponse<?> createImageName(ServerBaseDto serverBaseDto) throws NoLoginException;

    AppResponse<?> createElement(ServerBaseDto serverBaseDto) throws NoLoginException;

    AppResponse<?> updateElement(ServerBaseDto serverBaseDto) throws NoLoginException;

    AppResponse<?> copyElement(ServerBaseDto serverBaseDto) throws NoLoginException;

    AppResponse<?> getAllGroupInfo(ServerBaseDto serverBaseDto);
}