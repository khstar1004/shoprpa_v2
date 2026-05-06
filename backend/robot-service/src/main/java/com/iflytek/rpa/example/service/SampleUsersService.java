package com.iflytek.rpa.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.iflytek.rpa.utils.response.AppResponse;

/**
 * 사용자에서시스템중비고입력의데이터(SampleUsers)테이블서비스연결
 *
 * @author makejava
 * @since 2024-12-19
 */
public interface SampleUsersService {
    AppResponse<Boolean> insertUserSample(String userId, String tenantId);

    void sendOpenApi(String robotId, Integer version, String userId, String tenantId) throws JsonProcessingException;
}