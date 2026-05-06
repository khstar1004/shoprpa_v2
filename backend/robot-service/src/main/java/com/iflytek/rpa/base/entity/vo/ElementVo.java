package com.iflytek.rpa.base.entity.vo;

import lombok.Data;

@Data
public class ElementVo {
    /**
     * 요소id
     */
    private String id;

    /**
     * 원이름
     */
    private String name;
    /**
     * 아이콘
     */
    private String icon;
    //    /**
    //     * 이미지id
    //     */
    //    @JsonIgnore
    //    private String imageId;
    //
    //    /**
    //     * 요소의단계이미지id
    //     */
    //    @JsonIgnore
    //    private String parentImageId;

    /**
     * 이미지다운로드주소
     */
    private String imageUrl;

    /**
     * 요소의단계이미지다운로드주소
     */
    private String parentImageUrl;

    /**
     * 원내용
     */
    private String elementData;
}