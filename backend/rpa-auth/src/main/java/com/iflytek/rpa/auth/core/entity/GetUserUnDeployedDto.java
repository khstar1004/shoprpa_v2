package com.iflytek.rpa.auth.core.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * 가져오기미완료모듈사용자목록조회DTO
 *
 * @author system
 */
@Data
public class GetUserUnDeployedDto implements Serializable {
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
     * 사용ID
     */
    private String appId;

    /**
     * 휴대폰 번호(사용조회)
     */
    private String phone;
}