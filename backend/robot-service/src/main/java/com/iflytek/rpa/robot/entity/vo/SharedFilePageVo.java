package com.iflytek.rpa.robot.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class SharedFilePageVo {
    private Long id;
    /*
     *  파일ID
     */
    private String fileId; // 파일ID
    /*
     *  파일이름
     */
    private String fileName; // 파일이름
    /**
     * 파일유형
     */
    private Integer fileType;
    /*
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime; // 생성 시간
    /*
     *  수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
    /*
     *  생성자이름
     */
    private String creatorName; // 생성자
    /*
     *  계정
     */
    private String phone;
    /*
     *  모듈id
     */
    private String deptId;
    /*
     *  모듈이름
     */
    private String deptName;
    /**
     * 파일태그id합치기
     */
    private List<String> tags;
    /**
     * 파일태그이름합치기
     */
    private List<String> tagsNames;

    /*
     * 파일 경로
     */
    private String filePath; // 파일 경로
}