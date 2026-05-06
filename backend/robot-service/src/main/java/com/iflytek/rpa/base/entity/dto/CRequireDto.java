package com.iflytek.rpa.base.entity.dto;

import java.util.Date;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CRequireDto {
    /**
     * ID
     */
    private Integer id;
    /**
     * ID
     */
    @NotBlank
    private String robotId;
    /**
     * 봇버전
     */
    private Integer robotVersion;
    /**
     * 이름
     */
    private String packageName;
    /**
     * 버전
     */
    private String packageVersion;
    /**
     * 이미지
     */
    private String mirror;
    /**
     * 생성자
     */
    private String creatorId;
    /**
     * 생성 시간
     */
    private Date createTime;
    /**
     * 수정자
     */
    private String updaterId;
    /**
     * 수정 시간
     */
    private Date updateTime;
    /**
     * 삭제로그(0테이블저장에서 1테이블삭제)
     */
    private Integer deleted = 0;
}