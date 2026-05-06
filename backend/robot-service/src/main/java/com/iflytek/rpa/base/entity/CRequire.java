package com.iflytek.rpa.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.util.Date;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * python관리관리(CRequire)유형
 *
 * @author mjren
 * @since 2024-10-14 17:21:35
 */
@Data
public class CRequire implements Serializable {
    private static final long serialVersionUID = -96631614802732786L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String projectId;
    /**
     * 목록이름
     */
    private String packageName;

    private String packageVersion;

    private String mirror;
    /**
     * 생성자id
     */
    private String creatorId;
    /**
     * 생성 시간
     */
    private Date createTime;
    /**
     * 수정자id
     */
    private String updaterId;
    /**
     * 생성 시간
     */
    private Date updateTime;
    /**
     * 삭제 0: 삭제되지 않음 1: 삭제됨
     */
    private Integer deleted;

    @NotBlank
    private String robotId;

    @NotBlank
    private Integer robotVersion;
}