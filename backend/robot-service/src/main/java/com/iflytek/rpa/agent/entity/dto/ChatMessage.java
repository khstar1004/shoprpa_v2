package com.iflytek.rpa.agent.entity.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 메시지결과
 */
@Data
@Accessors(chain = true)
public class ChatMessage {

    /**
     * 역할: user또는assistant
     */
    private String role;

    /**
     * 메시지내용
     */
    private String content;

    /**
     * 생성 시간
     */
    private String createTime;
}