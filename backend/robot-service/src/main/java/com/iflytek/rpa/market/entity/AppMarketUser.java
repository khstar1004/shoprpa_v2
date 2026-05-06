package com.iflytek.rpa.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 팀마켓-사람원테이블, n:n의닫기시스템(AppMarketUser)유형
 *
 * @author makejava
 * @since 2024-01-19 14:41:35
 */
@Data
public class AppMarketUser implements Serializable {
    private static final long serialVersionUID = -77353675644566502L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 마켓id
     */
    private String marketId;
    /**
     * 구성원유형: admin,consumer
     */
    private String userType;
    /**
     * 초대사람
     */
    private String creatorId;
    /**
     * 추가입력시간
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

    /**
     * 목록 테넌트id
     */
    private String tenantId;
}