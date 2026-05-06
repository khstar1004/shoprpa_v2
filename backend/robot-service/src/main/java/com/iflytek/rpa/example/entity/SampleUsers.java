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
 * 사용자에서시스템중비고입력의데이터(SampleUsers)유형
 *
 * @author makejava
 * @since 2024-12-19
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SampleUsers implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 기본 키증가ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 사용자일식별자(예 UUID)
     */
    private String creatorId;

    /**
     * 닫기  sample_templates.sample_id
     */
    private String sampleId;

    /**
     * 사용자까지의이름( name, 가능지정)
     */
    private String name;

    /**
     * 에서중비고입력의매칭데이터(JSON 문자열,  Java 순서열)
     */
    private String data;

    /**
     * : system(시스템비고입력)또는 user(사용자생성/수정)
     */
    private String source;

    /**
     * 비고입력시사용의버전, 사용후업그레이드
     */
    private String versionInjected;

    /**
     * 테넌트ID
     */
    private String tenantId;

    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdTime;

    /**
     * 후수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedTime;
}