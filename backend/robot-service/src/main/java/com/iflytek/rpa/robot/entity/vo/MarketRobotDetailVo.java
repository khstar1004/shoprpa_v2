package com.iflytek.rpa.robot.entity.vo;

import java.util.List;
import lombok.Data;

@Data
public class MarketRobotDetailVo {

    MyRobotDetailVo myRobotDetailVo;

    // 기존버전정보
    String sourceName;
    List<VersionInfo> versionInfoList;
}