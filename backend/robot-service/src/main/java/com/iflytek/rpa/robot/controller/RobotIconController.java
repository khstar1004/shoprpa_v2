package com.iflytek.rpa.robot.controller;

import com.iflytek.rpa.robot.entity.dto.RobotIconDto;
import com.iflytek.rpa.robot.service.RobotIconService;
import com.iflytek.rpa.utils.response.AppResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 봇아이콘정보
 */
@RestController
@RequestMapping("/robot-icon")
public class RobotIconController {
    @Autowired
    RobotIconService robotIconService;

    @PostMapping("/info")
    public AppResponse<?> getIconAndNameInfo(@RequestBody RobotIconDto dto) throws Exception {
        return robotIconService.RobotIconModeHandler(dto);
    }
}