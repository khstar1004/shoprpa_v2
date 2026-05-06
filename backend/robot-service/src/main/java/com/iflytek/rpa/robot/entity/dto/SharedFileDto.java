package com.iflytek.rpa.robot.entity.dto;

import java.util.List;
import lombok.Data;

@Data
public class SharedFileDto {
    /*
     * fileId
     */
    private String fileId;

    /**
     * 파일이름
     */
    private String fileName;
    /**
     * 파일유형
     */
    private Integer fileType;
    /*
     * 태그ID목록(문자열방식, 예"1,2,3")
     */
    private List<Long> tags;
}