package com.iflytek.rpa.robot.controller;

import com.iflytek.rpa.robot.entity.RobotVersion;
import com.iflytek.rpa.robot.entity.dto.EnableVersionDto;
import com.iflytek.rpa.robot.entity.dto.RobotVersionDto;
import com.iflytek.rpa.robot.entity.dto.VersionListDto;
import com.iflytek.rpa.robot.service.RobotVersionService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 단말봇버전테이블(RobotVersion)테이블제어
 *
 * @author makejava
 * @since 2024-09-29 15:27:41
 */
@RestController
@RequestMapping("/robot-version")
public class RobotVersionController {

    /**
     * 서비스객체
     */
    @Resource
    private RobotVersionService robotVersionService;

    /**
     * 봇발송버전-재이름검증
     *
     * @param robotVersionDto
     * @return
     * @throws Exception
     */
    @PostMapping("/same-name")
    public AppResponse<?> checkSameName(@RequestBody RobotVersionDto robotVersionDto) throws Exception {
        return robotVersionService.checkSameName(robotVersionDto);
    }

    /**
     * 봇발송버전
     *
     * @param robotVersionDto
     * @return
     * @throws Exception
     */
    @PostMapping("/publish")
    public AppResponse<?> publishRobot(@Valid @RequestBody RobotVersionDto robotVersionDto) throws Exception {

        return robotVersionService.publishRobot(robotVersionDto);
    }

    /**
     * 봇발송버전-위발송버전정보돌아가기
     *
     * @param robotVersion
     * @return
     */
    @PostMapping("/latest-info")
    public AppResponse<?> getRobotVersionInfo(@RequestBody RobotVersion robotVersion) throws NoLoginException {
        return robotVersionService.getLastRobotVersionInfo(robotVersion);
    }

    /**
     * 실행기기조회지정봇모든버전
     *
     * @param queryDto
     * @return
     * @throws NoLoginException
     */
    @GetMapping("/list4Execute")
    public AppResponse<?> list4Execute(VersionListDto queryDto) throws NoLoginException {
        return robotVersionService.versionList(queryDto);
    }

    /**
     * 사용지정버전
     *
     * @param queryDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/enable-version")
    public AppResponse<?> enableVersion(@RequestBody EnableVersionDto queryDto) throws Exception {
        return robotVersionService.enableVersion(queryDto);
    }

    /**
     * 복사지정버전
     *
     * @param queryDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/recover-version")
    public AppResponse<?> recoverVersion(@RequestBody EnableVersionDto queryDto) throws Exception {
        return robotVersionService.recoverVersion(queryDto);
    }

    /**
     * 계획기기버전관리관리목록
     *
     * @param robotId
     * @return
     * @throws NoLoginException
     */
    @GetMapping("/list4Design")
    public AppResponse<?> list4Design(@RequestParam String robotId) throws NoLoginException {
        return robotVersionService.list4Design(robotId);
    }
}