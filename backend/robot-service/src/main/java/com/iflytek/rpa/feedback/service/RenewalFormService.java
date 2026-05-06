package com.iflytek.rpa.feedback.service;

import com.iflytek.rpa.feedback.entity.dto.RenewalFormSubmitDto;
import com.iflytek.rpa.utils.response.AppResponse;

/**
 * 테이블단일서비스연결
 *
 * @author system
 * @since 2024-12-15
 */
public interface RenewalFormService {

    /**
     * 제출테이블단일
     *
     * @param dto 테이블단일제출DTO
     * @return 제출결과
     */
    AppResponse<?> submitRenewalForm(RenewalFormSubmitDto dto);
}