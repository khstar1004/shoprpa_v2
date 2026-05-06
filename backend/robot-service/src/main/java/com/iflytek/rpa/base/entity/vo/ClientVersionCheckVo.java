package com.iflytek.rpa.base.entity.vo;

import java.io.Serializable;
import lombok.Data;

/**
 * 클라이언트버전조회VO
 *
 * @author system
 * @since 2025-01-XX
 */
@Data
public class ClientVersionCheckVo implements Serializable {

    /**
     * 여부필요업데이트: 1-필요업데이트, 0-아니요필요업데이트
     */
    private Integer needUpdate;

    /**
     * 새버전
     */
    private String version;

    /**
     * 업데이트정보
     */
    private String updateInfo;

    /**
     * 다운로드주소
     */
    private String downloadUrl;
    /**
     * 운영체제
     */
    private String os;

    /**
     * 아키텍처
     */
    private String arch;
}