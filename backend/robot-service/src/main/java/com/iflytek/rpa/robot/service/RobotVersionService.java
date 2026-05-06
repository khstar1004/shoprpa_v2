package com.iflytek.rpa.robot.service;

import com.iflytek.rpa.robot.entity.RobotExecute;
import com.iflytek.rpa.robot.entity.RobotVersion;
import com.iflytek.rpa.robot.entity.dto.EnableVersionDto;
import com.iflytek.rpa.robot.entity.dto.RobotVersionDto;
import com.iflytek.rpa.robot.entity.dto.VersionListDto;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;

/**
 * 단말봇버전테이블(RobotVersion)테이블서비스연결
 *
 * @author makejava
 * @since 2024-09-29 15:27:41
 */
public interface RobotVersionService {

    AppResponse<?> publishRobot(RobotVersionDto robotVersionDto) throws Exception;

    AppResponse<?> checkSameName(RobotVersionDto robotVersionDto) throws NoLoginException;

    AppResponse<?> getLastRobotVersionInfo(RobotVersion robotVersion) throws NoLoginException;

    AppResponse<?> versionList(VersionListDto queryDto) throws NoLoginException;

    AppResponse<?> enableVersion(EnableVersionDto queryDto) throws Exception;

    AppResponse<?> recoverVersion(EnableVersionDto queryDto) throws Exception;

    AppResponse<?> list4Design(String robotId) throws NoLoginException;

    void updateAppAndRobot(RobotExecute robotExecute, Integer nextVersion) throws NoLoginException;
}