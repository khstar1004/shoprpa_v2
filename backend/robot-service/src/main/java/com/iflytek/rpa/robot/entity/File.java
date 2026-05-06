package com.iflytek.rpa.robot.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 파일테이블(File)유형
 *
 * @author mjren
 * @since 2024-11-07 10:26:27
 */
public class File implements Serializable {
    private static final long serialVersionUID = -82285542284681246L;
    /**
     * 기본 키ID
     */
    private Integer id;
    /**
     * 파일의uuid
     */
    private String fileId;
    /**
     * 파일에서s3위의경로
     */
    private String path;
    /**
     * 생성 시간
     */
    private Date createTime;
    /**
     * 수정 시간
     */
    private Date updateTime;
    /**
     * 삭제로그위치
     */
    private Integer deleted;
    /**
     * 파일이름
     */
    private String fileName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}