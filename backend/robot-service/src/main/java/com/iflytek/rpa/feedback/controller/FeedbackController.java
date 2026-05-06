package com.iflytek.rpa.feedback.controller;

import com.iflytek.rpa.feedback.entity.dto.ConsultFormSubmitDto;
import com.iflytek.rpa.feedback.entity.dto.FeedbackSubmitDto;
import com.iflytek.rpa.feedback.entity.dto.RenewalFormSubmitDto;
import com.iflytek.rpa.feedback.service.ConsultFormService;
import com.iflytek.rpa.feedback.service.FeedbackService;
import com.iflytek.rpa.feedback.service.RenewalFormService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 반대제어기기
 *
 * @author system
 * @since 2024-12-15
 */
@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private ConsultFormService consultFormService;

    @Autowired
    private RenewalFormService renewalFormService;

    /**
     * 제출반대
     * 비고: 프론트엔드필요호출Python서비스(/api/resource/file/upload)업로드이미지가져오기fileId, 
     * 후호출연결fileId목록
     *
     * @param dto 반대제출DTO
     * @return 제출결과
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    @PostMapping("/submit")
    public AppResponse<?> submitFeedback(@RequestBody @Valid FeedbackSubmitDto dto) throws NoLoginException {
        return feedbackService.submitFeedback(dto);
    }

    /**
     * 제출문의테이블단일(버전/버전)
     *
     * @param dto 문의테이블단일제출DTO
     * @return 제출결과
     */
    @PostMapping("/consult/submit")
    public AppResponse<?> submitConsultForm(@RequestBody @Valid ConsultFormSubmitDto dto) {
        return consultFormService.submitConsultForm(dto);
    }

    /**
     * 제출테이블단일(버전/버전)
     *
     * @param dto 테이블단일제출DTO
     * @return 제출결과
     */
    @PostMapping("/renewal/submit")
    public AppResponse<?> submitRenewalForm(@RequestBody @Valid RenewalFormSubmitDto dto) {
        return renewalFormService.submitRenewalForm(dto);
    }
}