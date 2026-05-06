package com.iflytek.rpa.task.entity.dto;

import lombok.Data;

@Data
public class TaskDto {

    private String userId;

    private String name;

    private Long pageNo;

    private Long pageSize;

    // 예update_time
    private String sortBy;

    // desc또는asc
    private String sortType;
}