package com.iflytek.rpa.robot.entity.dto;

import lombok.Data;

@Data
public class DeleteDesignDto {

    // 계획기기봇삭제분유형: 1:  계획기기 , 2: 계획기기 실행기기, 3: 계획기기 실행 예약 작업사용
    // 실행기기봇삭제분유형: 1:  실행기기, 3: 실행 예약 작업사용
    Integer situation;

    // 봇id
    String robotId;

    // 다중개사용해당봇의예약 작업id, 사용열기, 있음삼필요.
    String taskIds;
}