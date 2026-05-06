package com.iflytek.rpa.component.entity.vo;

import java.util.List;
import lombok.Data;

@Data
public class ComponentInfoVo {
    String name; // 컴포넌트이름
    String icon; // 이미지
    Integer latestVersion; // 새버전
    String creatorName; // 생성자이름
    String introduction; // 새버전의

    List<VersionInfo> versionInfoList;
}