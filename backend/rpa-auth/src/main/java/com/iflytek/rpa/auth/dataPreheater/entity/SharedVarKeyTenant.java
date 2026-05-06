package com.iflytek.rpa.auth.dataPreheater.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;

/**
 * 공유 변수테넌트키테이블(SharedVarKeyTenant)유형
 *
 * @author jqfang3
 * @since 2025-07-21
 */
@Data
public class SharedVarKeyTenant implements Serializable {
    private static final long serialVersionUID = 221473413657231318L;

    /**
     * 기본 키id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 테넌트id
     */
    private String tenantId;

    /**
     * 공유 변수테넌트키
     */
    @TableField(value = "`key`")
    private String key;

    /**
     * 삭제 여부 0: 삭제되지 않음, 1: 삭제됨
     */
    private Integer deleted;
}