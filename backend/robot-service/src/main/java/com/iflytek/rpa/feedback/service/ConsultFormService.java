package com.iflytek.rpa.feedback.service;

import com.iflytek.rpa.feedback.entity.dto.ConsultFormSubmitDto;
import com.iflytek.rpa.utils.response.AppResponse;

/**
 * 문의테이블단일서비스연결
 *
 * @author system
 * @since 2024-12-15
 */
public interface ConsultFormService {

    /**
     * 제출문의테이블단일
     *
     * @param dto 문의테이블단일제출DTO
     * @return 제출결과
     */
    AppResponse<?> submitConsultForm(ConsultFormSubmitDto dto);
}