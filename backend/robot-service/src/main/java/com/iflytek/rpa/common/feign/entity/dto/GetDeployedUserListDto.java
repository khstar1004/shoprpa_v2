package com.iflytek.rpa.common.feign.entity.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 가져오기완료모듈사용자목록조회DTO
 *
 * @author system
 */
@Data
public class GetDeployedUserListDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 사용ID
     */
    private String appId;

    /**
     * 마켓ID
     */
    private String marketId;

    /**
     * 테넌트ID
     */
    private String tenantId;

    /**
     * 이름(사용조회)
     */
    private String realName;

    /**
     * 코드
     */
    private Integer pageNo;

    /**
     * 매크기
     */
    private Integer pageSize;
}