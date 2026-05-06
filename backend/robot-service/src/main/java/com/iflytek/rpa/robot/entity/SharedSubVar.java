package com.iflytek.rpa.robot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;

/**
 * 공유 변수-변수(SharedSubVar)유형
 *
 * @author makejava
 * @since 2024-12-19
 */
@Data
public class SharedSubVar implements Serializable {
    private static final long serialVersionUID = 222473423657236318L;

    /**
     * 변수id
     */
    @TableId(value = "id", type = IdType.AUTO)
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
     * 변수유형: text/password/array
     */
    private String varType;

    /**
     * 변수값, 암호화이면로비밀문서, 아니요이면로문서
     */
    private String varValue;

    /**
     * 여부암호화:1-암호화
     */
    private Integer encrypt;

    /**
     * 삭제 여부 0: 삭제되지 않음, 1: 삭제됨
     */
    private Integer deleted;
}