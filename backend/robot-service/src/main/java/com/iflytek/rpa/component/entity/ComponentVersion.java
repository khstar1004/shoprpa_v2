package com.iflytek.rpa.component.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 컴포넌트버전테이블(ComponentVersion)유형
 *
 * @author makejava
 * @since 2024-12-19
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComponentVersion implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 기본 키id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 컴포넌트id
     */
    private String componentId;

    /**
     * 버전
     */
    private Integer version;

    /**
     * 
     */
    private String introduction;

    /**
     * 변경 로그
     */
    private String updateLog;

    /**
     * 생성자id
     */
    private String creatorId;

    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 수정자id
     */
    private String updaterId;

    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 삭제 여부 0: 삭제되지 않음, 1: 삭제됨
     */
    private Integer deleted;

    /**
     * 테넌트id
     */
    private String tenantId;

    /**
     * 매개변수
     */
    private String param;

    /**
     * 발송버전시의테이블단일매개변수정보
     */
    private String paramDetail;

    /**
     * 아이콘
     */
    private String icon;
}