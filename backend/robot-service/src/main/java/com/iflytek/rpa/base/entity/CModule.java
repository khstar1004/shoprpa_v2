package com.iflytek.rpa.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.util.Date;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Null;
import lombok.Data;

/**
 * 클라이언트-python모듈
 * @author bywei4
 */
@Data
public class CModule implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 모듈id
     */
    @Null
    private String moduleId;
    /**
     * 전체량python코드데이터
     */
    @Null
    private String moduleContent;
    /**
     * python파일이름
     */
    @Null
    private String moduleName;

    private Integer deleted;

    private String creatorId;

    private Date createTime;

    private String updaterId;

    private Date updateTime;

    @NotBlank
    private String robotId;

    @NotBlank
    private Integer robotVersion;

    private String breakpoint;
}