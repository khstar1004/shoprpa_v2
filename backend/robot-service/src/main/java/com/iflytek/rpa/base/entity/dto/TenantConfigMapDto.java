package com.iflytek.rpa.base.entity.dto;

import java.io.Serializable;
import java.util.Map;
import lombok.Data;

/**
 * 테넌트매칭Map DTO
 * 사용저장의테넌트매칭JSON결과
 * {
 *   "resource_code": ResourceConfigDto
 * }
 */
@Data
public class TenantConfigMapDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 코드까지매칭의
     */
    private Map<String, ResourceConfigDto> configs;
}