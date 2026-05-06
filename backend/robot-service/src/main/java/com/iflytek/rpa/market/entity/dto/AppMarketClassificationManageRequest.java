package com.iflytek.rpa.market.entity.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 앱 마켓분유형관리관리조회요청 DTO
 *
 * @author auto-generated
 */
@Data
public class AppMarketClassificationManageRequest implements Serializable {
    /**
     * 분유형이름
     */
    private String name;

    /**
     * : 0-시스템, 1-지정
     */
    private Integer source;
}