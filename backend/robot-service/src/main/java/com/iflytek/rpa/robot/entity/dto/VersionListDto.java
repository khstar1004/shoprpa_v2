package com.iflytek.rpa.robot.entity.dto;

import lombok.Data;

@Data
public class VersionListDto {
    String robotId; // 봇id

    Integer sortType = 1; // 근거버전순서정렬 0:asc 1:desc

    Long pageNo; // 데이터

    Long pageSize; // 페이지크기
}