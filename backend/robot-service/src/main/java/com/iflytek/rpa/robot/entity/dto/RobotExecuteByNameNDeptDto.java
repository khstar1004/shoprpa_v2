package com.iflytek.rpa.robot.entity.dto;

import java.util.List;
import lombok.Data;

@Data
public class RobotExecuteByNameNDeptDto {
    String robotName;
    List<String> deptIdPathList; // 모듈id전체경로목록
    String deptIdPath; // 모듈id전체경로
    String tenantId;
}