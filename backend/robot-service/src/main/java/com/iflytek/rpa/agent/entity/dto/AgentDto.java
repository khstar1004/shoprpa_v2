package com.iflytek.rpa.agent.entity.dto;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * AgentDTO
 * 기록저장에서content JSON중
 */
@Data
@Accessors(chain = true)
public class AgentDto {

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
     * 에서content JSON중저장, 형식: 
     * [
     *   {"role": "user", "content": "사용자메시지", "timestamp": "2025-01-01 10:00:00"},
     *   {"role": "assistant", "content": "AI돌아가기복사", "timestamp": "2025-01-01 10:00:01"}
     * ]
     */
    private List<ChatMessage> chatHistory;
}