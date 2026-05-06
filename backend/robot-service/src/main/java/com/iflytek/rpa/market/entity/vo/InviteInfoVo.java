package com.iflytek.rpa.market.entity.vo;

import lombok.Data;

/**
 * 초대정보 VO
 */
@Data
public class InviteInfoVo extends AcceptResultVo {
    /**
     * 초대사람이름
     */
    private String inviterName;

    /**
     * 팀이름
     */
    private String marketName;
}