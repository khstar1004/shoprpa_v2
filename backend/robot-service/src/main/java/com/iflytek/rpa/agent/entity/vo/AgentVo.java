package com.iflytek.rpa.agent.entity.vo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iflytek.rpa.agent.entity.dto.ChatMessage;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Agent반환VO
 * 사용목록반환(평면결과)
 */
@Data
@Accessors(chain = true)
public class AgentVo {

    /**
     * RPA Agent ID
     */
    private String agentId;

    /**
     * Agent이름
     */
    private String agentName;

    /**
     * 시스템안내
     */
    private String systemPrompt;

    /**
     * MCP서비스서버목록
     */
    private JSONArray mcpServers;

    /**
     * RPA봇매칭
     */
    private JSONObject rpaRobots;

    /**
     * 기록목록
     */
    private List<ChatMessage> chatHistory;
}