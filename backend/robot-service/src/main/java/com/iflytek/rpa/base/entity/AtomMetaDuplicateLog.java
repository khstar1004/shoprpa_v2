package com.iflytek.rpa.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import javax.validation.constraints.Null;
import lombok.Data;

@Data
public class AtomMetaDuplicateLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 기존가능
     */
    @Null
    private String atomKey;

    /**
     * 기존가능버전
     */
    @Null
    private String version;

    /**
     * 요청 
     */
    @Null
    private String requestBody;

    /**
     * 삭제 여부
     */
    private Integer deleted;

    /**
     * 생성자ID
     */
    private Long creatorId;

    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 수정자ID
     */
    private Long updaterId;

    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}