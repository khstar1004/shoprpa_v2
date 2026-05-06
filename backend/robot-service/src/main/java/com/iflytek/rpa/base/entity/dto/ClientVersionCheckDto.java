package com.iflytek.rpa.base.entity.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 클라이언트버전조회DTO
 *
 * @author system
 * @since 2025-01-XX
 */
@Data
public class ClientVersionCheckDto {

    /**
     * 클라이언트현재버전
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
}