package com.iflytek.rpa.market.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

/**
 * @author mjren
 * @date 2025-07-01 10:16
 * @copyright Copyright (c) 2025 mjren
 */
@Data
public class AppApplicationTenant {
    private String tenantId;
    private Short auditEnable;

    /**
     * 검토열기닫기상태변수변경시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date auditEnableTime;

    /**
     * 검토열기닫기상태변수변경사람
     */
    private String auditEnableOperator;

    /**
     * 검토열기닫기상태변수변경원인
     */
    private String auditEnableReason;
}