package com.iflytek.rpa.robot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

@Data
public class SharedFileTag {

    /**
     * 태그id
     */
    @TableId(value = "tag_id", type = IdType.AUTO)
    private Long tagId;

    /**
     * 태그이름
     */
    private String tagName;

    /**
     * 테넌트id
     */
    private String tenantId;

    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

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
     * 생성자id
     */
    private String creatorId;

    /**
     * 수정자id
     */
    private String updaterId;
}