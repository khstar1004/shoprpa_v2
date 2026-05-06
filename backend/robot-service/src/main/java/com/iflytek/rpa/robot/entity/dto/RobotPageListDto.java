package com.iflytek.rpa.robot.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

@Data
public class RobotPageListDto {
    Integer pageSize = 10;

    Integer pageNo = 1;
    /**
     * 수정 시간 (봇제품새발송버전의시간)  가능선택 createTime  생성 시간
     */
    String sortBy = "latestReleaseTime";
    /**
     * 순서
     */
    String sortType = "desc";

    /**
     * 봇이름
     */
    String robotName;
    /**
     * 모듈id
     */
    String deptId;

    /**
     * 모듈id전체경로
     */
    String deptIdPath;

    /**
     * 생성사람id
     */
    String creatorId;
    /**
     * 생성사람이름
     */
    String creatorName;
    /**
     * 테넌트id
     */
    String tenantId;

    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    Date createTimeStart;
    /**
     * 생성 시간결과
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    Date createTimeEnd;

    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    Date latestReleaseTimeStart;
    /**
     * 수정 시간결과
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    Date latestReleaseTimeEnd;
}