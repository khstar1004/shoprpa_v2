package com.iflytek.rpa.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * (AppMarketDict)유형
 *
 * @author mjren
 * @since 2024-03-25 10:44:06
 */
@Data
public class AppMarketDict implements Serializable {
    private static final long serialVersionUID = -18658781229065808L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 서비스코드: 1, 행유형, 2, 역할공가능marketRoleFunc
     */
    private String businessCode;
    /**
     * 행이름, 역할공가능이름
     */
    private String name;
    /**
     * 행코드, 공가능코드
     */
    private String dictCode;
    /**
     * T있음권한, F권한이 없습니다
     */
    private String dictValue;
    /**
     * 마켓모든, 관리관리원, 통신사용자
     */
    private String userType;
    /**
     * 설명
     */
    private String description;
    /**
     * 정렬
     */
    private Integer seq;

    private String creatorId;

    private Date createTime;

    private String updaterId;

    private Date updateTime;

    private Integer deleted;
}