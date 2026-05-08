package com.iflytek.rpa.agent.service.impl;

import com.alibaba.fastjson.JSON;
import com.iflytek.rpa.agent.dao.AgentDao;
import com.iflytek.rpa.agent.entity.Agent;
import com.iflytek.rpa.agent.entity.dto.AgentDto;
import com.iflytek.rpa.agent.entity.vo.AgentVo;
import com.iflytek.rpa.agent.service.AgentService;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Agent Service유형
 */
@Service
public class AgentServiceImpl implements AgentService {

    @Resource
    private IdWorker idWorker;

    @Resource
    private AgentDao agentDao;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    public AppResponse<AgentVo> save(AgentDto agentDto) throws NoLoginException {
        String agentId = agentDto.getAgentId();
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();

        // 를DTO변환로JSON문자열저장
        String content = JSON.toJSONString(agentDto);

        Agent agent = new Agent().setAgentId(agentId).setContent(content).setUpdaterId(userId);

        // agentId찾을 수 없습니다, 이면생성새의agentId
        if (StringUtils.isEmpty(agentId)) {
            agentId = "agent_" + idWorker.nextId();
            agent.setCreatorId(userId).setAgentId(agentId);
            agentDto.setAgentId(agentId); // 업데이트DTO중의agentId
            content = JSON.toJSONString(agentDto); // 다시 순서열
            agent.setContent(content);
            create(agent);
            // 저장연결반환agentId
            return AppResponse.success(new AgentVo().setAgentId(agentId));
        }

        // agentId저장에서, 이면업데이트완료있음의Agent매칭
        int result = agentDao.updateContent(agent);
        if (result != 1) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EXCEPTION.getCode(), "Agent 설정 업데이트에 실패했습니다");
        }

        return AppResponse.success(new AgentVo().setAgentId(agentId));
    }

    @Override
    public AppResponse<AgentVo> getByAgentId(String agentId) {
        Agent agent = agentDao.getByAgentId(agentId);

        if (agent == null) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EMPTY, "Agent 설정을 찾을 수 없습니다");
        }

        // 를저장의JSON문자열파싱로DTO
        AgentDto agentDto = JSON.parseObject(agent.getContent(), AgentDto.class);

        // API, 반환평면의AgentVo
        AgentVo agentVo = new AgentVo()
                .setAgentId(agent.getAgentId())
                .setAgentName(agentDto.getAgentName())
                .setSystemPrompt(agentDto.getSystemPrompt())
                .setMcpServers(agentDto.getMcpServers())
                .setRpaRobots(agentDto.getRpaRobots())
                .setChatHistory(agentDto.getChatHistory());

        return AppResponse.success(agentVo);
    }

    @Override
    public AppResponse<AgentVo> delete(String agentId) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();

        int result = agentDao.deleteAgent(agentId, userId);

        if (result != 1) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EXCEPTION.getCode(), "Agent 설정 삭제에 실패했습니다");
        }
        return AppResponse.success(new AgentVo().setAgentId(agentId));
    }

    @Override
    public AppResponse<List<AgentVo>> listAllAgents() throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();

        List<Agent> agentList = agentDao.listAgentsByUserId(userId);

        List<AgentVo> agentVoList = agentList.stream()
                .map(agent -> {
                    // 를저장의JSON문자열파싱로DTO
                    AgentDto agentDto = JSON.parseObject(agent.getContent(), AgentDto.class);

                    // API, 반환평면의AgentVo
                    return new AgentVo()
                            .setAgentId(agent.getAgentId())
                            .setAgentName(agentDto.getAgentName())
                            .setSystemPrompt(agentDto.getSystemPrompt())
                            .setMcpServers(agentDto.getMcpServers())
                            .setRpaRobots(agentDto.getRpaRobots())
                            .setChatHistory(agentDto.getChatHistory());
                })
                .collect(Collectors.toList());

        return AppResponse.success(agentVoList);
    }

    /**
     * 생성새의Agent매칭
     */
    private Integer create(Agent agent) {
        int insert = agentDao.insertAgent(agent);

        if (insert != 1) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EXCEPTION.getCode(), "Agent 설정 생성에 실패했습니다");
        }

        return insert;
    }

    @Override
    public AppResponse<AgentVo> create(String agentName) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();

        // 완료새의agentId
        String agentId = "agent_" + idWorker.nextId();

        // 생성소내용의DTO, 패키지agentId및agentName
        AgentDto agentDto = new AgentDto().setAgentId(agentId).setAgentName(agentName);

        String content = JSON.toJSONString(agentDto);

        Agent agent = new Agent()
                .setAgentId(agentId)
                .setContent(content)
                .setCreatorId(userId)
                .setUpdaterId(userId);

        create(agent);

        return AppResponse.success(new AgentVo().setAgentId(agentId));
    }
}
