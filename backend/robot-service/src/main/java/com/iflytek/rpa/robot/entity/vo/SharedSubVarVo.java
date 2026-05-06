package com.iflytek.rpa.robot.entity.vo;

import lombok.Data;

/**
 * 공유 변수변수VO
 *
 * @author makejava
 * @since 2024-12-19
 */
@Data
public class SharedSubVarVo {

    /**
     * 변수id
     */
    private Long id;

    /**
     * 공유 변수id
     */
    private Long sharedVarId;

    /**
     * 변수이름
     */
    private String varName;

    /**
     * 유형: 텍스트/비밀번호/배열
     */
    private String varType;

    /**
     * 변수값
     */
    private String varValue;

    /**
     * 여부암호화:1-암호화
     */
    private Integer encrypt;
}