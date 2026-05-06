package com.iflytek.rpa.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 시스템지정의라이브러리(SampleTemplates)유형
 *
 * @author makejava
 * @since 2024-12-19
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SampleTemplates implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 기본 키
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * id
     */
    private String sampleId;

    /**
     * 버전이름
     */
    private String name;

    /**
     * 유형: robot_design, robot_execute, schedule_task 대기
     */
    private String type;

    /**
     * 버전(예 1.2.0)
     */
    private String version;

    /**
     * 매칭데이터(JSON 형식), 데이터베이스일행의데이터
     */
    private String data;

    /**
     * 설명
     */
    private String description;

    /**
     * 여부사용(false 이면새사용자아니요비고입력)
     */
    private Integer isActive;

    /**
     * 삭제(물품관리삭제)
     */
    private Integer isDeleted;

    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdTime;

    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedTime;
}