package com.iflytek.rpa.resource.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 파일테이블유형
 *
 * @author system
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("file")
public class File {

    /**
     * 기본 키ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 파일의uuid
     */
    @TableField("file_id")
    private String fileId;

    /**
     * 파일에서s3위의경로
     */
    @TableField("path")
    private String path;

    /**
     * 생성 시간
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 수정 시간
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 삭제로그위치
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /**
     * 파일이름
     */
    @TableField("file_name")
    private String fileName;
}