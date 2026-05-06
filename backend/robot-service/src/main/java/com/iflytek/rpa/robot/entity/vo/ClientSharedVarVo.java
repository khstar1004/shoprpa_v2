package com.iflytek.rpa.robot.entity.vo;

import java.util.List;
import lombok.Data;

/**
 * 클라이언트공유 변수VO
 *
 * @author jqfang3
 * @since 2025-07-21
 */
@Data
public class ClientSharedVarVo {

    /**
     * 공유 변수ID
     */
    private Long id;

    /**
     * 공유 변수이름
     */
    private String sharedVarName;

    /**
     * 공유 변수유형
     */
    private String sharedVarType;

    /**
     * 공유 변수여부암호화
     */
    private Integer encrypt;

    /**
     * 공유 변수값(암호화후데이터)
     */
    private String sharedVarValue;

    /**
     * 변수목록
     */
    private List<ClientSharedSubVarVo> subVarList;
}