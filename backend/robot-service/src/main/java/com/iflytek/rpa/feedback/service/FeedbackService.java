package com.iflytek.rpa.feedback.service;

import com.iflytek.rpa.feedback.entity.dto.FeedbackSubmitDto;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;

/**
 * 반대서비스연결
 *
 * @author system
 * @since 2024-12-15
 */
public interface FeedbackService {

    /**
     * 제출반대
     *
     * @param dto 반대제출DTO
     * @return 제출결과
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    AppResponse<?> submitFeedback(FeedbackSubmitDto dto) throws NoLoginException;
}