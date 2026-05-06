package com.iflytek.rpa.base.entity.vo;

import lombok.Data;

/**
 * 즐겨찾기의기존가능vo
 */
@Data
public class AtomLikeVo {
    Long likeId;
    String key;
    String atomContent;
    String title;
    String icon;
}