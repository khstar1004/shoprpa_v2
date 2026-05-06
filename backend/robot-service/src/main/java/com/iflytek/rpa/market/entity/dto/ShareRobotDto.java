package com.iflytek.rpa.market.entity.dto;

import com.iflytek.rpa.market.entity.AppMarketResource;
import java.util.List;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author mjren
 * @date 2025-07-09 10:03
 * @copyright Copyright (c) 2025 mjren
 */
@Data
public class ShareRobotDto {

    @NotBlank(message = "봇id비워 둘 수 없습니다")
    private String robotId;

    // @NotNull(message = "버전비워 둘 수 없습니다")
    private Integer version;

    private Integer editFlag;

    @NotBlank(message = "행분유형비워 둘 수 없습니다")
    private String category;

    private List<String> marketIdList;

    private List<AppMarketResource> appInsertInfoList;

    private List<AppMarketResource> appUpdateInfoList;

    /**
     * 사용id, id, 컴포넌트id
     */
    private String appId;
    /**
     * 게시사람
     */
    private String creatorId;
    /**
     * 수정자id
     */
    private String updaterId;

    private String tenantId;
    /**
     * 이름
     */
    private String appName;
}