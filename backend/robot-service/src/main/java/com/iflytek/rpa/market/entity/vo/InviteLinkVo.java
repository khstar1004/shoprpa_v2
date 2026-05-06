package com.iflytek.rpa.market.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

/**
 * 초대연결VO
 */
@Data
public class InviteLinkVo {
    /**
     * 초대키
     */
    private String inviteKey;

    /**
     * 경과제한
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expireTime;

    /**
     * 실패시간유형: 4시간, 24시간, 7, 30
     * 값: 4H, 24H, 7D, 30D
     */
    private String expireType;
    /**
     * 여부초과경과사람데이터제한제어: 0-미완료초과경과, 1-초과경과
     */
    private Integer overNumLimit;
}