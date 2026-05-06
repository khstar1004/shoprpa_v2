package com.iflytek.rpa.robot.entity.vo;

import java.util.List;
import lombok.Data;

@Data
public class DelExecuteRobotVo {
    // 봇삭제분유형: 1: 실행기기, 3: 실행기기 예약 작업사용  로완료프론트엔드복사사용
    Integer situation;

    // 봇사용닫기시스템테이블
    List<TaskReferInfo> taskReferInfoList;

    // 봇Id
    String robotId;
}