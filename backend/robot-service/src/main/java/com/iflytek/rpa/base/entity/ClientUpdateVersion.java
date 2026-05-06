package com.iflytek.rpa.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 클라이언트버전조회테이블(ClientUpdateVersion)유형
 *
 * @author system
 * @since 2025-01-XX
 */
@Data
public class ClientUpdateVersion implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 기본 키ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 버전
     */
    private String version;

    /**
     * 운영체제
     */
    private String os;

    /**
     * 아키텍처
     */
    private String arch;

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
     * 생성 시간
     */
    private Date createTime;

    /**
     * 수정 시간
     */
    private Date updateTime;

    /**
     * 삭제 여부 0: 삭제되지 않음, 1: 삭제됨
     */
    private Integer deleted;
}