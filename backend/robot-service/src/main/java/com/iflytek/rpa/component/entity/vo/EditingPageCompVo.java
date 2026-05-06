package com.iflytek.rpa.component.entity.vo;

import lombok.Data;

/**
 * 의왼쪽 componentVo
 */
@Data
public class EditingPageCompVo {
    String componentId; // 컴포넌트id
    String name; // 컴포넌트이름
    String icon; // 아이콘
    Integer isLatest; // 여부로새 1 예 ,  0 아니요
}