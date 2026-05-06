package com.iflytek.rpa.market.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

/**
 * @author mjren
 * @date 2025-07-01 10:14
 * @copyright Copyright (c) 2025 mjren
 */
@Data
public class AppApplication {
    private Long id;

    private String robotId;

    private Integer robotVersion;

    private String applicationType;

    private String status;

    private String securityLevel;

    private String allowedDept;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expireTime;

    private String auditOpinion;

    private String creatorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    private String updaterId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    private Integer deleted;

    private String tenantId;

    private Integer clientDeleted;

    private Integer cloudDeleted;

    private Integer defaultPass;

    /**
     * 팀마켓id, 사용일발송위신청, 검토통신경과후공유까지해당마켓
     */
    @TableField(exist = false)
    private String marketId;

    /**
     * 마켓정보JSON문자열, 패키지marketIdList, editFlag, category대기정보
     */
    private String marketInfo;
    /**
     * 발송버전정보JSON문자열
     */
    private String publishInfo;
}