package com.iflytek.rpa.market.entity.vo;

import java.util.List;
import lombok.Data;

@Data
public class AppDetailVo {
    String iconUrl;
    String appName;
    Long downloadNum;
    Long checkNum;
    String introduction;
    String videoPath;

    // 본정보
    String creatorName;
    String category;
    String fileName;
    String filePath;

    // 사용설명
    String useDescription;

    // 버전정보
    List<AppDetailVersionInfo> versionInfoList;
}