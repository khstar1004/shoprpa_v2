package com.iflytek.rpa.robot.entity.vo;

import java.util.List;
import lombok.Data;

@Data
public class DelDesignRobotVo {
    // 봇삭제분유형: 1:  계획기기 , 2: 계획기기 실행기기, 3: 계획기기 실행 예약 작업사용
    Integer situation;

    // 봇사용닫기시스템테이블
    List<TaskReferInfo> taskReferInfoList;

    // 봇Id
    String robotId;
}