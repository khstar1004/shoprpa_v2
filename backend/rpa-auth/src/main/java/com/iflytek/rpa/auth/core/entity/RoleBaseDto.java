package com.iflytek.rpa.auth.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 역할정보 DTO
 * @author xqcao2
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleBaseDto {

    /**
     * 역할ID
     */
    private String id;

    /**
     * 역할코드
     */
    private String code;

    /**
     * 역할이름
     */
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}