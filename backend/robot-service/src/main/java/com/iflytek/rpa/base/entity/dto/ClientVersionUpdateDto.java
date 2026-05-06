package com.iflytek.rpa.base.entity.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 클라이언트버전업데이트DTO
 *
 * @author system
 * @since 2025-01-XX
 */
@Data
public class ClientVersionUpdateDto {

    /**
     * 기본 키ID(업데이트시)
     */
    private Long id;

    /**
     * 버전
     */
    @NotBlank(message = "버전비워 둘 수 없습니다")
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
     * 다운로드연결
     */
    @NotBlank(message = "다운로드연결비워 둘 수 없습니다")
    private String downloadUrl;

    /**
     * 업데이트내용
     */
    private String updateInfo;
}