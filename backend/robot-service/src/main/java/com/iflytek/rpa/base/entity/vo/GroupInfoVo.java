package com.iflytek.rpa.base.entity.vo;

import java.util.List;
import lombok.Data;

@Data
public class GroupInfoVo {
    /**
     * 분그룹이름
     */
    private String name;

    /**
     * 분그룹id
     */
    private String id;

    /**
     * 해당그룹내부모든이미지객체
     */
    private List<ElementInfoVo> elements;
}