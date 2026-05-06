package com.iflytek.rpa.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 제품품목버전테이블유형
 * 저장개사람버전, 버전, 버전대기버전정보
 */
@Data
@TableName("sys_product_version")
public class SysProductVersion implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 기본 키ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 버전코드(예: personal, professional, enterprise)
     */
    private String versionCode;

    /**
     * 삭제식별자: 0-삭제되지 않음, 1-삭제됨
     */
    private Integer deleted;

    /**
     * 생성 시간
     */
    private Date createTime;
}