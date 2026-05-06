package com.iflytek.rpa.auth.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import java.util.List;
import lombok.Data;

/**
 * @author mjren
 * @date 2024-01-22 11:04
 * @copyright Copyright (c) 2024 mjren
 */
@Data
public class MarketDto {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String tenantId;
    /**
     * 마켓id
     */
    private String marketId;
    /**
     * 구성원유형: admin,consumer
     */
    private String userType;
    /**
     *
     */
    private String creatorId;

    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 사용이름
     */
    private String appName;

    private String appId;

    private Integer appVersion;

    /**
     * 서비스, 
     */
    private String dictName;

    /**
     * 
     */
    private String appIntro;

    /**
     * 사용설명
     */
    private String useIntro;

    private Boolean isCreator;

    /**
     * 모든
     */
    private String ownerName;

    private Long ownerId;

    /**
     * 마켓이름
     */
    private String marketName;

    private Boolean isAdded;

    private String userName;

    private String realName;

    private String email;

    private String phone;

    private Integer deleted;

    private Boolean isAdmin;

    private Boolean haveSharePrivate;

    private List<?> appMarketVersionList;

    private Integer pageNo;

    private Integer pageSize;

    private List<String> marketIdList;

    /**
     * 예update_time
     */
    private String sortBy;

    /**
     * desc또는asc
     */
    private String sortType;

    private List<?> userInfoList;

    private String resourceStatus;

    private String robotId;

    private String componentId;

    private Integer updateVersionNum;

    private List<String> userIdList;

    public MarketDto() {}

    public MarketDto(String tenantId, String marketId, String appId) {
        this.marketId = marketId;
        this.tenantId = tenantId;
        this.appId = appId;
    }
}