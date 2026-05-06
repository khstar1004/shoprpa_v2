package com.iflytek.rpa.task.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

/**
 * 예약 작업실행기록목록DTO
 * @author jqfang3
 * @since 2025-08-05
 */
@Data
public class ScheduleTaskRecordDto {
    /**
     * 예약 작업이름
     */
    private String taskName;
    /**
     * 열기 날짜
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startDate;
    /**
     * 결과날짜
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endDate;
    /**
     * 작업상태:성공:success, 시작 실패:start_error, 실행실패:exe_error, 가져오기 :cancel, 실행중:executing
     */
    private String status;
    /**
     * 작업유형: : manual, 예약: schedule, 메일: mail, 파일: file, : hotKey
     */
    private String taskType;

    private Integer pageNo;

    private Integer pageSize;

    /**
     * 정렬:시작 시간
     */
    private String sortBy = "startTime";
    /**
     * 순서
     */
    private String sortType = "desc";

    /**
     * 사용자ID
     */
    private String userId;

    /**
     * 테넌트ID
     */
    private String tenantId;
}