package com.iflytek.rpa.market.entity.dto;

import lombok.Data;

/**
 * 완료초대연결요청 DTO
 */
@Data
public class InviteLinkDto {
    /**
     * 마켓id
     */
    private String marketId;

    /**
     * 실패시간유형: 4시간, 24시간, 7, 30
     * 값: 4H, 24H, 7D, 30D
     */
    private String expireType;
}