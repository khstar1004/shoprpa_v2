package com.iflytek.rpa.base.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 클라이언트버전업데이트VO
 *
 * @author system
 * @since 2025-01-XX
 */
@Data
public class ClientVersionUpdateVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 기본 키ID
     */
    private Long id;

    /**
     * 버전
     */
    private String version;

    /**
     * 버전숫자
     */
    private Integer versionNum;

    /**
     * 다운로드연결
     */
    private String downloadUrl;

    /**
     * 업데이트내용
     */
    private String updateInfo;

    /**
     * 운영체제
     */
    private String os;

    /**
     * 아키텍처
     */
    private String arch;

    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}