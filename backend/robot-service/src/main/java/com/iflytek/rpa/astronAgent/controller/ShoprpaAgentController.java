package com.iflytek.rpa.astronAgent.controller;

import com.iflytek.rpa.astronAgent.entity.dto.CopyRobotDto;
import com.iflytek.rpa.astronAgent.entity.dto.CopyRobotResponseDto;
import com.iflytek.rpa.astronAgent.entity.dto.GetUserIdDto;
import com.iflytek.rpa.astronAgent.entity.dto.GetUserIdResponseDto;
import com.iflytek.rpa.astronAgent.service.ShoprpaAgentService;
import com.iflytek.rpa.utils.response.AppResponse;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * ShoprpaAgent제어기기
 */
@RestController
@RequestMapping("/astron-agent")
public class ShoprpaAgentController {

    @Resource
    private ShoprpaAgentService astronAgentService;

    /**
     * 복사봇까지목록사용자의개사람테넌트아래
     * @param copyRobotDto 복사봇요청 매개변수
     * @return 복사후의봇id
     */
    @PostMapping("/copy-robot")
    public AppResponse<CopyRobotResponseDto> copyRobot(@Valid @RequestBody CopyRobotDto copyRobotDto) {
        return astronAgentService.copyRobot(copyRobotDto);
    }

    /**
     * 통신경과휴대폰 번호가져오기사용자ID
     * @param getUserIdDto 가져오기사용자ID요청 매개변수
     * @return 사용자ID
     */
    @PostMapping("/get-user-id")
    public AppResponse<GetUserIdResponseDto> getUserIdByPhone(@Valid @RequestBody GetUserIdDto getUserIdDto) {
        return astronAgentService.getUserIdByPhone(getUserIdDto);
    }
}