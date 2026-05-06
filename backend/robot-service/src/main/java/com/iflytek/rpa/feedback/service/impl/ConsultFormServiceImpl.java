package com.iflytek.rpa.feedback.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.feedback.dao.ConsultFormDao;
import com.iflytek.rpa.feedback.entity.ConsultForm;
import com.iflytek.rpa.feedback.entity.dto.ConsultFormSubmitDto;
import com.iflytek.rpa.feedback.entity.enums.FormStatus;
import com.iflytek.rpa.feedback.entity.enums.FormType;
import com.iflytek.rpa.feedback.service.ConsultFormService;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문의테이블단일서비스유형
 *
 * @author system
 * @since 2024-12-15
 */
@Slf4j
@Service
public class ConsultFormServiceImpl extends ServiceImpl<ConsultFormDao, ConsultForm> implements ConsultFormService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> submitConsultForm(ConsultFormSubmitDto dto) {
        try {
            // 1. 매개변수검증
            //            validateDto(dto);

            // 2. 저장문의테이블단일기록
            ConsultForm consultForm = new ConsultForm();
            consultForm.setFormType(dto.getFormType());
            consultForm.setCompanyName(dto.getCompanyName());
            consultForm.setContactName(dto.getContactName());
            consultForm.setMobile(dto.getMobile());
            consultForm.setEmail(dto.getEmail());
            consultForm.setTeamSize(dto.getTeamSize());
            consultForm.setStatus(FormStatus.PENDING.getCode()); // 대기관리
            consultForm.setCreatedAt(new Date());
            consultForm.setUpdatedAt(new Date());

            this.save(consultForm);

            // 3. 반환결과
            return AppResponse.success("문의테이블단일제출성공");
        } catch (ServiceException e) {
            log.error("제출문의테이블단일실패: {}", e.getMessage(), e);
            return AppResponse.error(ErrorCodeEnum.E_PARAM, e.getMessage());
        } catch (Exception e) {
            log.error("제출문의테이블단일예외: {}", e.getMessage(), e);
            return AppResponse.error(ErrorCodeEnum.E_COMMON, "제출문의테이블단일실패, 요청 후재시도");
        }
    }

    /**
     * 매개변수검증
     * 비고: 검증완료통신경과@Valid비고해제에서Controller완료
     * 서비스닫기의검증
     */
    private void validateDto(ConsultFormSubmitDto dto) {
        // 인증테이블단일유형여부있음
        FormType formType = FormType.getByCode(dto.getFormType());
        if (formType == null) {
            throw new ServiceException("테이블단일유형없음, 지요소버전(1)및버전(2)");
        }
    }
}