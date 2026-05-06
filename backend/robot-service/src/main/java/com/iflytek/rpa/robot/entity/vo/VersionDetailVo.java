package com.iflytek.rpa.robot.entity.vo;

import java.util.Date;
import lombok.Data;

@Data
public class VersionDetailVo {
    Integer versionNum;
    Date updateTime;
    String updateLog;
    String online; // enable 시작 ; disable 사용할 수 없습니다
    String robotId;
}