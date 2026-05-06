package com.iflytek.rpa.base.service;

import com.iflytek.rpa.base.entity.dto.ResourceConfigDto;
import java.util.Map;

/**
 * 테넌트서비스연결
 */
public interface TenantResourceService {

    /**
     * 가져오기테넌트의매칭(저장)
     * @param tenantId 테넌트ID
     * @return 매칭Map, key로resourceCode, value로ResourceConfigDto(완료정보)
     */
    Map<String, ResourceConfigDto> getTenantResourceConfig(String tenantId);

    /**
     * 다시 완료업데이트테넌트의매칭JSON
     * 테넌트수정매칭금액시호출방법법, 직선연결수정final값
     * @param tenantId 테넌트ID
     * @param quotaUpdates 매칭금액업데이트기록, key로resourceCode, value로새의final값
     */
    void regenerateTenantConfig(String tenantId, Map<String, Integer> quotaUpdates);

    /**
     * 지우기테넌트매칭저장
     * @param tenantId 테넌트ID
     */
    void clearTenantConfigCache(String tenantId);

    /**
     * 근거코드가져오기 매칭
     * @param tenantId 테넌트ID
     * @param resourceCode 코드
     * @return 매칭, 결과가찾을 수 없습니다반환null
     */
    ResourceConfigDto getResourceConfig(String tenantId, String resourceCode);
}