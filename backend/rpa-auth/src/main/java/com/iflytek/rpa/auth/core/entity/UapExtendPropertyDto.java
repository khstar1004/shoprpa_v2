package com.iflytek.rpa.auth.core.entity;

/**
 * 유형추가속성 DTO
 * @author xqcao2
 *
 */
public class UapExtendPropertyDto {

    /**
     * 속성ID
     */
    private String id;

    /**
     * 속성 의값
     */
    private String value;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}