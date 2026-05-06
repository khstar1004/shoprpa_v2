package com.iflytek.rpa.auth.core.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * 가져오기마켓사용자목록조회DTO
 *
 * @author system
 */
@Data
public class GetMarketUserListDto implements Serializable {
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
     * 로그인이름(사용조회)
     */
    private String userName;

    /**
     * 이름(사용조회)
     */
    private String realName;

    /**
     * 정렬필드(예update_time)
     */
    private String sortBy;

    /**
     * 정렬유형(descend또는ascend)
     */
    private String sortType;

    /**
     * 코드
     */
    private Integer pageNo;

    /**
     * 매크기
     */
    private Integer pageSize;
}