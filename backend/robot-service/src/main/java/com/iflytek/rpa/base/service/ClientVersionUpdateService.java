package com.iflytek.rpa.base.service;

import com.iflytek.rpa.base.entity.dto.ClientVersionCheckDto;
import com.iflytek.rpa.base.entity.dto.ClientVersionUpdateDto;
import com.iflytek.rpa.base.entity.vo.ClientVersionCheckVo;
import com.iflytek.rpa.base.entity.vo.ClientVersionUpdateVo;
import com.iflytek.rpa.utils.response.AppResponse;

/**
 * 클라이언트버전업데이트서비스연결
 *
 * @author system
 * @since 2025-01-XX
 */
public interface ClientVersionUpdateService {

    /**
     * 추가버전정보
     *
     * @param dto 버전정보DTO
     * @return 결과
     */
    AppResponse<ClientVersionUpdateVo> save(ClientVersionUpdateDto dto) throws Exception;

    /**
     * 업데이트버전정보
     *
     * @param dto 버전정보DTO
     * @return 결과
     */
    AppResponse<ClientVersionUpdateVo> update(ClientVersionUpdateDto dto) throws Exception;

    /**
     * 조회클라이언트버전여부필요업데이트
     *
     * @param dto 버전조회DTO
     * @return 버전조회결과
     */
    AppResponse<ClientVersionCheckVo> checkVersion(ClientVersionCheckDto dto) throws Exception;

    /**
     * 조회클라이언트버전여부필요업데이트(버전, 근거버전)
     *
     * @param version 현재버전
     * @return 새버전의다운로드URL, 결과가완료예새버전이면반환null
     */
    String checkVersionSimple(String os, String arch, String version) throws Exception;
}