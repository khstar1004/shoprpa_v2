package com.iflytek.rpa.robot.entity.vo;

import lombok.Data;

/**
 * 클라이언트공유변수VO
 *
 * @author jqfang3
 * @since 2025-07-21
 */
@Data
public class ClientSharedSubVarVo {

    /**
     * 변수이름
     */
    private String varName;

    /**
     * 변수유형
     */
    private String varType;

    /**
     * 변수여부암호화
     */
    private Integer encrypt;

    /**
     * 변수값(암호화후의데이터)
     */
    private String varValue;
}