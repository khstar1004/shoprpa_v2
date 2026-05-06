package com.iflytek.rpa.base.entity.dto;

import lombok.Data;

@Data
public class FrontElementDto extends FrontCommonDto {

    /**
     * 아이콘
     */
    private String icon;
    /**
     * 이미지id
     */
    private String imageId;

    /**
     * 요소의단계이미지id
     */
    private String parentImageId;

    /**
     * 원내용
     */
    private String elementData;

    private String commonSubType;
}