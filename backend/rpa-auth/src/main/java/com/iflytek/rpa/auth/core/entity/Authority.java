package com.iflytek.rpa.auth.core.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.Date;

/**
 * @desc: 권한유형
 * @author: weilai <laiwei3@iflytek.com>
 * @create: 2025/11/24 17:41
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Authority implements Serializable {

    private static final long serialVersionUID = -4275488494609444232L;

    /**
     * ID
     */
    private String id;

    /**
     * 권한이름
     */
    private String name;

    /**
     * 권한코드
     */
    private String code;

    /**
     * 권한상태{0중지사용 1사용}
     */
    private Integer status = 1;

    /**
     * 권한유형 0:MENU, 1:URL, 2:AUTH
     */
    private Integer type;

    /**
     * 단계권한ID
     */
    private String parentId;

    /**
     * 권한단계
     */
    private Integer level;

    /**
     * 사용 ID
     */
    private String appId;

    /**
     * 사용name
     */
    private String appName;

    /**
     * 정도ID
     */
    private String dimId;

    /**
     * 정도이름
     */
    private String dimName;

    /**
     * 주소유형0위아래문서1이름2경로
     */
    private Integer urlType;

    /**
     * 권한주소
     */
    private String url;

    /**
     * 정렬
     */
    private Integer sort = 1;

    /**
     * 아이콘
     */
    private String ico;

    /**
     * 비고
     */
    private String remark;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getDimId() {
        return dimId;
    }

    public void setDimId(String dimId) {
        this.dimId = dimId;
    }

    public String getDimName() {
        return dimName;
    }

    public void setDimName(String dimName) {
        this.dimName = dimName;
    }

    public Integer getUrlType() {
        return urlType;
    }

    public void setUrlType(Integer urlType) {
        this.urlType = urlType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getIco() {
        return ico;
    }

    public void setIco(String ico) {
        this.ico = ico;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
}