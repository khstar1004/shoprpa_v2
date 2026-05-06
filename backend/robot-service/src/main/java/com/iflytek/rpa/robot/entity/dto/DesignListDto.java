package com.iflytek.rpa.robot.entity.dto;

import lombok.Data;

@Data
public class DesignListDto {

    String name; // 봇이름

    String sortType = "desc"; // asc desc

    Long pageNo; // 데이터

    Long pageSize; // 페이지크기

    String dataSource = "create"; // create:생성의 ; market:마켓
}