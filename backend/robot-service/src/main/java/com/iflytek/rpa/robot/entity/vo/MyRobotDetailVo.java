package com.iflytek.rpa.robot.entity.vo;

import java.util.Date;
import lombok.Data;

@Data
public class MyRobotDetailVo {
    String name;
    Integer version;
    String introduction; // 
    String useDescription; // 사용설명

    String fileName; // 파일이름
    String filePath; // 파일 경로

    String videoName; // 이름
    String videoPath; // 경로

    String creatorName;
    Date createTime;
    /**
     * 모듈
     */
    String deptName;
    /**
     * 수정 시간
     */
    Date updateTime;
    /**
     * 봇Id
     */
    String robotId;
}