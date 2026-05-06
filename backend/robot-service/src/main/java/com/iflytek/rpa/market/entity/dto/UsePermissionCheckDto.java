package com.iflytek.rpa.market.entity.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UsePermissionCheckDto {
    /**
     * 사용ID()
     */
    @NotBlank(message = "사용ID비워 둘 수 없습니다")
    private String appId;

    /**
     * 마켓ID()
     */
    @NotBlank(message = "마켓ID비워 둘 수 없습니다")
    private String marketId;

    /**
     * 사용자ID
     */
    private String userId;

    /**
     * 테넌트ID
     */
    private String tenantId;
}