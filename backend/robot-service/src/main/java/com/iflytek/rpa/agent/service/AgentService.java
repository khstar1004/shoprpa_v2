package com.iflytek.rpa.agent.service;

import com.iflytek.rpa.agent.entity.dto.AgentDto;
import com.iflytek.rpa.agent.entity.vo.AgentVo;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;

/**
 * Agent Service연결
 */
public interface AgentService {

    /**
     * 저장Agent매칭
     *
     * @param agentDto Agent매칭정보
     * @return 저장결과
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    AppResponse<AgentVo> save(AgentDto agentDto) throws NoLoginException;

    /**
     * 근거AgentId가져오기매칭정보
     *
     * @param agentId Agent ID
     * @return Agent
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    AppResponse<AgentVo> getByAgentId(String agentId);

    /**
     * 삭제Agent매칭
     *
     * @param agentId Agent ID
     * @return 삭제결과
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    AppResponse<AgentVo> delete(String agentId) throws NoLoginException;

    /**
     * 가져오기모든Agent매칭
     *
     * @return 모든Agent매칭
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    AppResponse<List<AgentVo>> listAllAgents() throws NoLoginException;

    /**
     * 새생성Agent, 입력매개agentName, 반환agentId
     *
     * @param agentName Agent이름
     * @return 패키지agentId의결과
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    AppResponse<AgentVo> create(String agentName) throws NoLoginException;
}