package com.iflytek.rpa.feedback.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.feedback.dao.FeedbackReportDao;
import com.iflytek.rpa.feedback.entity.FeedbackReport;
import com.iflytek.rpa.feedback.entity.dto.FeedbackSubmitDto;
import com.iflytek.rpa.feedback.entity.dto.FeedbackSubmitResponse;
import com.iflytek.rpa.feedback.service.FeedbackService;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 반대서비스유형
 *
 * @author system
 * @since 2024-12-15
 */
@Slf4j
@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackReportDao, FeedbackReport> implements FeedbackService {

    @Autowired
    private IdWorker idWorker;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> submitFeedback(FeedbackSubmitDto dto) throws NoLoginException {
        try {
            // 1. 매개변수검증
            validateDto(dto);

            // 2. 관리이미지ID목록
            String imageIdsStr = "";
            if (dto.getImageIds() != null && !dto.getImageIds().isEmpty()) {
                imageIdsStr = String.join(",", dto.getImageIds());
            }

            // 3. 완료일번호
            String reportNo = generateReportNo();

            // 4. 저장반대기록
            FeedbackReport feedbackReport = new FeedbackReport();
            feedbackReport.setId(idWorker.nextId());
            feedbackReport.setReportNo(reportNo);
            feedbackReport.setUsername(dto.getUsername());
            feedbackReport.setCategories(dto.getCategories());
            feedbackReport.setDescription(dto.getDescription());
            feedbackReport.setImageIds(imageIdsStr);
            feedbackReport.setCreateTime(new Date());
            feedbackReport.setDeleted(0);
            feedbackReport.setProcessed(0); // 미완료관리

            this.save(feedbackReport);

            // 5. 반환결과
            FeedbackSubmitResponse response = new FeedbackSubmitResponse();
            response.setReportNo(reportNo);

            return AppResponse.success(response);
        } catch (ServiceException e) {
            log.error("제출반대실패: {}", e.getMessage(), e);
            return AppResponse.error(ErrorCodeEnum.E_PARAM, e.getMessage());
        } catch (Exception e) {
            log.error("제출반대예외: {}", e.getMessage(), e);
            return AppResponse.error(ErrorCodeEnum.E_COMMON, "제출반대실패, 요청 후재시도");
        }
    }

    /**
     * 매개변수검증
     * 비고: 검증완료통신경과@Valid비고해제에서Controller완료
     * 서비스닫기의검증(예JSON형식인증)
     */
    private void validateDto(FeedbackSubmitDto dto) {
        // 인증categories여부로있음의JSON형식
        try {
            JSON.parseObject(dto.getCategories());
        } catch (Exception e) {
            throw new ServiceException("제목 분류 형식이 올바르지 않습니다. JSON 형식이어야 합니다");
        }
    }

    /**
     * 완료일번호
     * 형식: FB + 시간(yyyyMMddHHmmss) + 법ID의후6위치
     */
    private String generateReportNo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = sdf.format(new Date());
        long snowflakeId = idWorker.nextId();
        String lastSixDigits = String.valueOf(snowflakeId)
                .substring(Math.max(0, String.valueOf(snowflakeId).length() - 6));
        return "FB" + timestamp + lastSixDigits;
    }
}