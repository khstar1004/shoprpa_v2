package com.iflytek.rpa.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 팀마켓-팀테이블(AppMarket)유형
 *
 * @author makejava
 * @since 2024-01-19 14:41:33
 */
@Data
public class AppMarket implements Serializable {
    private static final long serialVersionUID = -41282788033507896L;
    /**
     * 팀마켓id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String marketId;
    /**
     * 마켓이름
     */
    private String marketName;
    /**
     * 마켓공유
     */
    private String marketShare;
    /**
     * 사용공유
     */
    private String appShare;
    /**
     * 마켓설명
     */
    private String marketDescribe;
    /**
     * 마켓유형: team,official,public(공유마켓)
     */
    private String marketType;
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

    private Boolean toDissolve;

    private String userType;

    private String userName;

    private String userPhone;

    private String tenantId;

    private String newOwner;
}