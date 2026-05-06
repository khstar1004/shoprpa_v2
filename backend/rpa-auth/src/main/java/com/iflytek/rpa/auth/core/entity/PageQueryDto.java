package com.iflytek.rpa.auth.core.entity;

/**
 * 분요청 DTO
 * @author xqcao2
 *
 */
public class PageQueryDto {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}