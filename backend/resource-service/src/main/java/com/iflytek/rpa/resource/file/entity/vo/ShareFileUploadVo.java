package com.iflytek.rpa.resource.file.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShareFileUploadVo {
    String fileId;
    Integer type; // 파일유형코드
    String fileName; // 파일이름
}