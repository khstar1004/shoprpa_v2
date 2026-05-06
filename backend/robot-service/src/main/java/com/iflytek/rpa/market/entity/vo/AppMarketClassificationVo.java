package com.iflytek.rpa.market.entity.vo;

import java.io.Serializable;
import lombok.Data;

/**
 * 앱 마켓분유형VO
 *
 * @author auto-generated
 */
@Data
public class AppMarketClassificationVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 분유형ID
     */
    private String id;

    /**
     * 분유형이름
     */
    private String name;

    private Integer sort;
}