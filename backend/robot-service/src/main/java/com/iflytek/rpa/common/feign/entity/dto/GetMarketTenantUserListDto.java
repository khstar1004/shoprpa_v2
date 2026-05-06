package com.iflytek.rpa.common.feign.entity.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 근거사용자ID목록조회테넌트사용자DTO
 *
 * @author system
 */
@Data
public class GetMarketTenantUserListDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 테넌트ID
     */
    private String tenantId;

    /**
     * 사용자ID목록(에서AppMarketUser의creatorId중가져오기)
     */
    private List<String> userIdList;
}