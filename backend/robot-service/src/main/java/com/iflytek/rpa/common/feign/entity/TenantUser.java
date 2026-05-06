package com.iflytek.rpa.common.feign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * (TenantUser)유형
 *
 * @author mjren
 * @since 2023-04-19 09:53:20
 */
@Data
public class TenantUser implements Serializable {
    private static final long serialVersionUID = 334855698665464892L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String tenantId;

    private String userId;

    private String phone;
    /**
     * 사용자명
     */
    private String name;
    /**
     * 이름
     */
    private String realName;

    private String jobNo;
    /**
     * 메일함
     */
    private String mail;
    /**
     *  0 1
     */
    private Integer gender;
    /**
     * 0대기 1사용 안 함 2사용
     */
    private Integer activeStatus;
    /**
     * 0사용 1사용 안 함
     */
    private Integer status;

    private Integer deleted;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    private String creatorId;

    private Long updateBy;

    private String deptIdPath;
}