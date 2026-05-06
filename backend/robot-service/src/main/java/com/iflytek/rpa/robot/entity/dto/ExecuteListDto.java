package com.iflytek.rpa.robot.entity.dto;

import lombok.Data;

@Data
public class ExecuteListDto {
    String name; // 봇이름

    String sortType = "desc"; // asc desc

    Long pageNo; // 데이터

    Long pageSize; // 페이지크기
}