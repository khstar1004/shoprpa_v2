package com.iflytek.rpa.market.entity.dto;

import java.util.List;
import lombok.Data;

/**
 * 마켓정보DTO
 * @author mjren
 * @date 2025-01-27
 */
@Data
public class MarketInfoDto {
    /**
     * 마켓ID목록
     */
    private List<String> marketIdList;

    /**
     * 권한식별자
     */
    private Integer editFlag;

    /**
     * 분유형
     */
    private String category;
}