package com.iflytek.rpa.astronAgent.service;

import com.iflytek.rpa.astronAgent.entity.dto.CopyRobotDto;
import com.iflytek.rpa.astronAgent.entity.dto.CopyRobotResponseDto;
import com.iflytek.rpa.astronAgent.entity.dto.GetUserIdDto;
import com.iflytek.rpa.astronAgent.entity.dto.GetUserIdResponseDto;
import com.iflytek.rpa.utils.response.AppResponse;

/**
 * ShoprpaAgent서비스연결
 */
public interface ShoprpaAgentService {

    /**
     * 복사봇까지목록사용자의개사람테넌트아래
     * @param copyRobotDto 복사봇요청 매개변수
     * @return 복사후의봇id
     */
    AppResponse<CopyRobotResponseDto> copyRobot(CopyRobotDto copyRobotDto);

    /**
     * 통신경과휴대폰 번호가져오기사용자ID
     * @param getUserIdDto 가져오기사용자ID요청 매개변수
     * @return 사용자ID
     */
    AppResponse<GetUserIdResponseDto> getUserIdByPhone(GetUserIdDto getUserIdDto);
}