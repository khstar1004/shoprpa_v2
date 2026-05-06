package com.iflytek.rpa.robot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/*
 * 공유파일유형
 */
@Data
public class SharedFile implements Serializable {
    private static final long serialVersionUID = -491204885219115201L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 파일id
     */
    private String fileId;
    /**
     * 공유파일이름
     */
    private String fileName;
    /**
     * 파일유형
     */
    private Integer fileType;

    /**
     * 테넌트id
     */
    private String tenantId;
    /**
     * 파일량상태
     */
    private Integer fileIndexStatus;
    /**
     * 모듈id
     */
    private String deptId;

    /**
     * 태그id합치기
     */
    private String tags;

    /**
     * S3저장경로
     */
    private String path;

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
}