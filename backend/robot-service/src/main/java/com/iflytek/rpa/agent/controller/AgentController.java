package com.iflytek.rpa.agent.controller;

import com.iflytek.rpa.agent.entity.dto.AgentDto;
import com.iflytek.rpa.agent.entity.vo.AgentVo;
import com.iflytek.rpa.agent.service.AgentService;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * Agent매칭제어기기
 */
@RestController
@RequestMapping("/agent")
public class AgentController {

    @Resource
    private AgentService agentService;

    /**
     * 저장Agent매칭
     *
     * @param agentDto Agent매칭정보
     * @return 저장결과
     * @throws Exception 예외
     */
    @PostMapping("/save")
    public AppResponse<AgentVo> save(@RequestBody AgentDto agentDto) throws Exception {
        return agentService.save(agentDto);
    }

    /**
     * 근거AgentId가져오기매칭정보
     *
     * @param agentId Agent ID
     * @return Agent매칭정보
     * @throws Exception 예외
     */
    @GetMapping("/detail")
    public AppResponse<AgentVo> getByAgentId(@RequestParam("agentId") String agentId) throws Exception {
        return agentService.getByAgentId(agentId);
    }

    /**
     * 삭제Agent매칭
     *
     * @param agentId Agent ID
     * @return 삭제결과
     * @throws Exception 예외
     */
    @GetMapping("/delete")
    public AppResponse<AgentVo> delete(@RequestParam("agentId") String agentId) throws Exception {
        return agentService.delete(agentId);
    }

    /**
     * 가져오기모든Agent매칭목록
     *
     * @return 모든Agent매칭
     * @throws Exception 예외
     */
    @GetMapping("/list")
    public AppResponse<List<AgentVo>> listAllAgents() throws Exception {
        return agentService.listAllAgents();
    }

    /**
     * 새생성Agent, 입력매개agentName, 반환agentId
     *
     * @param agentName Agent이름
     * @return 패키지agentId의결과
     * @throws Exception 예외
     */
    @PostMapping("/create")
    public AppResponse<AgentVo> create(@RequestBody AgentDto agentDto) throws Exception {
        return agentService.create(agentDto.getAgentName());
    }
}