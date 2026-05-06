package com.iflytek.rpa.market.entity.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 앱 마켓분유형관리관리조회DTO
 *
 * @author auto-generated
 */
@Data
public class AppMarketClassificationManageVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 분유형ID
     */
    private Long id;

    /**
     * 분유형이름
     */
    private String name;

    /**
     * : 0-시스템, 1-지정
     */
    private Integer source;

    /**
     * 사용데이터
     */
    private Integer reference;
}