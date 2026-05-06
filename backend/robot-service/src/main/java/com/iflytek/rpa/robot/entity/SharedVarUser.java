package com.iflytek.rpa.robot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;

/**
 * 공유 변수및사용자의테이블(SharedVarUser)유형
 *
 * @author makejava
 * @since 2024-12-19
 */
@Data
public class SharedVarUser implements Serializable {
    private static final long serialVersionUID = 221373423657236319L;

    /**
     * 기본 키id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 공유 변수id
     */
    private Long sharedVarId;

    /**
     * 사용자id
     */
    private String userId;

    /**
     * 사용자이름
     */
    private String userName;

    /**
     * 사용자휴대폰 번호
     */
    private String userPhone;

    /**
     * 삭제 여부 0: 삭제되지 않음, 1: 삭제됨
     */
    private Integer deleted;
}