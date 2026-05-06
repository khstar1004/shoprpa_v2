package com.iflytek.rpa.auth.core.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * 근거휴대폰 번호조회마켓사용자DTO
 *
 * @author system
 */
@Data
public class GetMarketUserByPhoneDto implements Serializable {
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

    private String keyword;
}