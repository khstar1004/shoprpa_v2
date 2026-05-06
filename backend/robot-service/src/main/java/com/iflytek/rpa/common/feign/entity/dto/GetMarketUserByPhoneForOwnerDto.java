package com.iflytek.rpa.common.feign.entity.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 근거휴대폰 번호조회마켓중의사용자DTO(사용마켓모든)
 *
 * @author system
 */
@Data
public class GetMarketUserByPhoneForOwnerDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 마켓ID
     */
    private String marketId;

    /**
     * 테넌트ID
     */
    private String tenantId;

    /**
     * 휴대폰 번호(사용조회)
     */
    private String phone;

    /**
     * 현재사용자ID(사용정렬제거)
     */
    private String userId;
}