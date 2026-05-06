package com.iflytek.rpa.robot.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

/**
 * 공유파일분조회DTO
 * @author yfchen40
 * @date 2025-07-21
 */
@Data
public class SharedFilePageDto {
    // 파일이름, 생성 시간, 모든, 모듈, 수정 시간, 태그, 분정보
    /**
     * 공유파일이름
     */
    private String fileName;

    /**
     * 열기 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date createTimeStart;

    /**
     * 결과생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date createTimeEnd;

    /**
     * 모든
     */
    private String creator;

    /**
     * 모듈
     */
    private String deptId;

    /**
     * 열기 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date updateTimeStart;

    /**
     * 결과수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date updateTimeEnd;

    /**
     * 태그
     */
    private String tags;

    /**
     * 조회데이터
     */
    private Integer pageNo = 1;

    /**
     * 매크기
     */
    private Integer pageSize = 10;
}