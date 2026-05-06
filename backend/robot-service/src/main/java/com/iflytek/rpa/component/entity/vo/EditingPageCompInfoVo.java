package com.iflytek.rpa.component.entity.vo;

import lombok.Data;

/**
 * 봇, 컴포넌트VO
 */
@Data
public class EditingPageCompInfoVo {
    String componentId; // 컴포넌트id
    String name; // 컴포넌트이름
    String introduction; // 컴포넌트
    Integer version; // "V" + Integer version
    Integer latestVersion; // "V" + Integer latestVersion
    Integer isLatest; // 1 예, 0 아니요
}