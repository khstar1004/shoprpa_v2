package com.iflytek.rpa.robot.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class ExecuteDetailVo {
    // 본정보
    String robotName;
    Integer versionNum;
    String fileName;
    String filePath;
    String videoName; // 이름
    String videoPath; // 경로
    String introduction;
    String useDescription;

    // 버전정보
    String sourceName; // 
    List<VersionInfo> versionInfoList; // 버전정보테이블

    // 생성자정보
    String creatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    Date createTime;
}