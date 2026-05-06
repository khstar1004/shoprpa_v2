package com.iflytek.rpa.component.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

@Data
public class VersionInfo {
    Integer version; // 버전

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    Date createTime; // 생성 시간

    String updateLog; // 변경 로그
}