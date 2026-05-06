package com.iflytek.rpa.market.entity.vo;

import java.util.List;
import lombok.Data;

@Data
public class MarketInfoVo {
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