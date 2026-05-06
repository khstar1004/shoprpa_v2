package com.iflytek.rpa.base.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 프로세스매개변수테이블
 *
 * @author tzzhang
 * @since
 */
@Data
public class CParam implements Serializable {
    private static final long serialVersionUID = -2745694034538081329L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 매개변수 
     */
    @JSONField(name = "var_direction")
    private int varDirection;

    /**
     * 매개변수이름
     */
    @JSONField(name = "var_name")
    private String varName;

    /**
     * 매개변수유형
     */
    @JSONField(name = "var_type")
    private String varType;

    /**
     * 매개변수내용
     */
    @JSONField(name = "var_value")
    private String varValue;

    /**
     * 매개변수설명
     */
    @JSONField(name = "var_describe")
    private String varDescribe;

    /**
     * 프로세스id
     */
    @JSONField(name = "process_id")
    private String processId;

    @JSONField(name = "creator_id")
    private String creatorId;

    @JSONField(name = "updater_id")
    private String updaterId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    private Integer deleted;

    @JSONField(name = "robot_id")
    private String robotId;

    @JSONField(name = "robot_version")
    private Integer robotVersion;

    /**
     * 프로세스id
     */
    private String moduleId;
}