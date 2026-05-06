package com.iflytek.rpa.robot.entity.dto;

import lombok.Data;

@Data
public class ShareDesignDto {
    // 봇id
    String robotId;
    // 공유봇의사용자id
    String sharedUserId;
    // 공유봇사용자의테넌트id
    String sharedTenantId;
    // 수신봇의사용자id
    String receivedUserId;
    // 수신봇사용자의테넌트id
    String receivedTenantId;
}