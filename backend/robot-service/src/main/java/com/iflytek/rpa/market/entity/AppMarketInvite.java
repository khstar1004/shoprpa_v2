package com.iflytek.rpa.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 팀마켓-초대연결테이블(AppMarketInvite)유형
 */
@Data
public class AppMarketInvite implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 초대연결key
     */
    private String inviteKey;

    /**
     * 마켓id
     */
    private String marketId;

    /**
     * 초대사람id
     */
    private String inviterId;

    /**
     * 실패시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expireTime;
    /**
     * 실패시간유형: 4시간, 24시간, 7, 30
     * 값: 4H, 24H, 7D, 30D
     */
    private String expireType;
    /**
     * 현재완료추가입력사람데이터
     */
    private Integer currentJoinCount;
    /**
     * 대추가입력사람데이터
     */
    private Integer maxJoinCount;

    /**
     * 생성자id
     */
    private String creatorId;

    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 수정자id
     */
    private String updaterId;

    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 삭제 여부 0: 삭제되지 않음, 1: 삭제됨
     */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}