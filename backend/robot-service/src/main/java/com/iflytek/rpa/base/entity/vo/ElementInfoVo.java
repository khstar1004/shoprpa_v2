package com.iflytek.rpa.base.entity.vo;

import lombok.Data;

@Data
public class ElementInfoVo {

    /**
     * 요소id
     */
    private String id;

    /**
     * 원이름
     */
    private String name;

    /**
     * 이미지연결
     */
    private String imageUrl;

    /**
     * parentImageUrl
     */
    private String parentImageUrl;

    /**
     * cv이미지, sigle통신선택, batch데이터가져오기
     */
    private String commonSubType;
}