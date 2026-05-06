package com.iflytek.rpa.base.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.base.dao.SysProductVersionDao;
import com.iflytek.rpa.base.dao.SysTenantConfigDao;
import com.iflytek.rpa.base.dao.SysVersionDefaultConfigDao;
import com.iflytek.rpa.base.entity.SysProductVersion;
import com.iflytek.rpa.base.entity.SysTenantConfig;
import com.iflytek.rpa.base.entity.SysVersionDefaultConfig;
import com.iflytek.rpa.base.entity.dto.ResourceConfigDto;
import com.iflytek.rpa.base.service.TenantResourceService;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.TenantExpirationDto;
import com.iflytek.rpa.utils.RedisUtils;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 테넌트서비스유형
 */
@Slf4j
@Service
public class TenantResourceServiceImpl extends ServiceImpl<SysTenantConfigDao, SysTenantConfig>
        implements TenantResourceService {

    private static final String REDIS_KEY_PREFIX = "tenant:resource:config:";
    private static final long CACHE_EXPIRE_SECONDS = 3600; // 저장1시간

    @Autowired
    private SysTenantConfigDao sysTenantConfigDao;

    @Autowired
    private SysVersionDefaultConfigDao sysVersionDefaultConfigDao;

    @Autowired
    private SysProductVersionDao sysProductVersionDao;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    public Map<String, ResourceConfigDto> getTenantResourceConfig(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            return Collections.emptyMap();
        }

        // 에서Redis저장가져오기
        String cacheKey = REDIS_KEY_PREFIX + tenantId;
        Object cached = RedisUtils.get(cacheKey);
        if (cached != null) {
            try {
                String jsonStr = cached.toString();
                return JSON.parseObject(jsonStr, new TypeReference<Map<String, ResourceConfigDto>>() {});
            } catch (Exception e) {
                log.warn("파싱저장매칭실패, 를에서데이터베이스다시 로드, tenantId: {}", tenantId, e);
            }
        }

        // 에서데이터베이스가져오기
        SysTenantConfig tenantConfig = sysTenantConfigDao.selectByTenantId(tenantId);
        if (tenantConfig == null) {
            // 결과가테넌트매칭찾을 수 없습니다, 시도
            return initializeTenantConfig(tenantId);
        }

        // 생성전체량매칭
        return buildFullConfig(tenantConfig);
    }

    /**
     * 테넌트매칭(에서버전매칭완료)
     * 직선연결반환전체량매칭, 아니요필요전체
     */
    private Map<String, ResourceConfigDto> initializeTenantConfig(String tenantId) {
        try {
            // 가져오기테넌트의버전정보
            Long versionId = getTenantVersionId(tenantId);
            if (versionId == null) {
                log.warn("불가가져오기테넌트버전정보, tenantId: {}", tenantId);
                return Collections.emptyMap();
            }

            // 가져오기버전매칭
            List<SysVersionDefaultConfig> defaultConfigs = sysVersionDefaultConfigDao.selectByVersionId(versionId);
            if (defaultConfigs.isEmpty()) {
                log.warn("버전매칭비어 있습니다, versionId: {}", versionId);
                return Collections.emptyMap();
            }

            // 생성전체량매칭Map
            Map<String, ResourceConfigDto> fullConfigMap = new HashMap<>();
            for (SysVersionDefaultConfig config : defaultConfigs) {
                ResourceConfigDto dto = new ResourceConfigDto();
                dto.setType(config.getResourceType() == 1 ? "QUOTA" : "SWITCH");
                dto.setBase(config.getDefaultValue());
                dto.setFinalValue(config.getDefaultValue());
                dto.setParent(config.getParentCode());

                // 파싱URL patterns
                if (StringUtils.isNotBlank(config.getUrlPatterns())) {
                    try {
                        List<String> urls = JSON.parseArray(config.getUrlPatterns(), String.class);
                        dto.setUrls(urls);
                    } catch (Exception e) {
                        log.warn("파싱URL patterns실패, resourceCode: {}", config.getResourceCode(), e);
                        dto.setUrls(Collections.emptyList());
                    }
                } else {
                    dto.setUrls(Collections.emptyList());
                }

                fullConfigMap.put(config.getResourceCode(), dto);
            }

            // 저장까지데이터베이스(extraConfigJson비어 있습니다, 원인로있음수정)
            SysTenantConfig tenantConfig = new SysTenantConfig();
            tenantConfig.setTenantId(tenantId);
            tenantConfig.setVersionId(versionId);
            tenantConfig.setExtraConfigJson("{}"); // 빈JSON객체, 테이블있음수정
            tenantConfig.setDeleted(0);
            tenantConfig.setCreateTime(new Date());
            tenantConfig.setUpdateTime(new Date());
            sysTenantConfigDao.insert(tenantConfig);

            // 저장입력저장
            String cacheKey = REDIS_KEY_PREFIX + tenantId;
            RedisUtils.set(cacheKey, JSON.toJSONString(fullConfigMap), CACHE_EXPIRE_SECONDS);

            return fullConfigMap;
        } catch (Exception e) {
            log.error("테넌트매칭실패, tenantId: {}", tenantId, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 가져오기테넌트의버전ID
     * 필요근거서비스, 가능필요에서테넌트 정보중가져오기
     */
    private Long getTenantVersionId(String tenantId) {
        // 조회테넌트매칭테이블, 결과가저장에서이면반환versionId
        SysTenantConfig tenantConfig = sysTenantConfigDao.selectByTenantId(tenantId);
        if (tenantConfig != null && tenantConfig.getVersionId() != null) {
            return tenantConfig.getVersionId();
        }

        String tenantType = "personal";
        try {
            AppResponse<TenantExpirationDto> resp = rpaAuthFeign.getExpiration();
            if (resp != null && resp.ok() && resp.getData() != null
                    && StringUtils.isNotBlank(resp.getData().getTenantType())) {
                tenantType = resp.getData().getTenantType();
            } else {
                log.warn("테넌트 만료 정보를 가져올 수 없어 personal 기본 버전을 사용합니다, tenantId: {}", tenantId);
            }
        } catch (Exception e) {
            log.warn("테넌트 만료 정보 조회 실패, personal 기본 버전을 사용합니다, tenantId: {}", tenantId, e);
        }

        SysProductVersion version;
        if ("professional".equals(tenantType)) {
            version = sysProductVersionDao.selectByVersionCode("professional");
        } else if ("enterprise".equals(tenantType) || tenantType.startsWith("enterprise_")) {
            version = sysProductVersionDao.selectByVersionCode("enterprise");
        } else {
            version = sysProductVersionDao.selectByVersionCode("personal");
        }

        if (version == null && !"personal".equals(tenantType)) {
            log.warn("테넌트 버전 매칭을 찾을 수 없어 personal 기본 버전을 사용합니다, tenantId: {}, tenantType: {}", tenantId, tenantType);
            version = sysProductVersionDao.selectByVersionCode("personal");
        }
        return version != null ? version.getId() : null;
    }

    /**
     * 생성전체량매칭
     * 에서버전매칭및수정생성의매칭정보
     */
    private Map<String, ResourceConfigDto> buildFullConfig(SysTenantConfig tenantConfig) {
        Long versionId = tenantConfig.getVersionId();

        // 가져오기버전매칭
        List<SysVersionDefaultConfig> defaultConfigs = sysVersionDefaultConfigDao.selectByVersionId(versionId);
        if (defaultConfigs.isEmpty()) {
            log.warn("버전매칭비어 있습니다, versionId: {}", versionId);
            return Collections.emptyMap();
        }

        // 파싱수정(extraConfigJson), 저장있음수정의
        // extraConfigJson패키지type, base, final필드, 아니요패키지urls및parent
        Map<String, ResourceConfigDto> modifiedConfigMap = new HashMap<>();
        if (StringUtils.isNotBlank(tenantConfig.getExtraConfigJson())
                && !"{}".equals(tenantConfig.getExtraConfigJson())) {
            try {
                // 파싱로Map<String, Map<String, Object>>
                Map<String, Map<String, Object>> rawMap = JSON.parseObject(
                        tenantConfig.getExtraConfigJson(), new TypeReference<Map<String, Map<String, Object>>>() {});
                // 변환로ResourceConfigDto(패키지type, base, final)
                for (Map.Entry<String, Map<String, Object>> entry : rawMap.entrySet()) {
                    Map<String, Object> configMap = entry.getValue();
                    ResourceConfigDto dto = new ResourceConfigDto();
                    dto.setType((String) configMap.get("type"));
                    dto.setBase(((Number) configMap.get("base")).intValue());
                    dto.setFinalValue(((Number) configMap.get("final")).intValue());
                    modifiedConfigMap.put(entry.getKey(), dto);
                }
            } catch (Exception e) {
                log.warn("파싱수정매칭실패, tenantId: {}", tenantConfig.getTenantId(), e);
            }
        }

        // 생성전체량매칭Map
        Map<String, ResourceConfigDto> fullConfigMap = new HashMap<>();
        for (SysVersionDefaultConfig defaultConfig : defaultConfigs) {
            ResourceConfigDto dto = new ResourceConfigDto();
            dto.setType(defaultConfig.getResourceType() == 1 ? "QUOTA" : "SWITCH");
            dto.setBase(defaultConfig.getDefaultValue());
            dto.setParent(defaultConfig.getParentCode());

            // 결과가있음수정, 사용수정후의final값, 아니요이면사용값
            ResourceConfigDto modifiedConfig = modifiedConfigMap.get(defaultConfig.getResourceCode());
            if (modifiedConfig != null && modifiedConfig.getFinalValue() != null) {
                dto.setFinalValue(modifiedConfig.getFinalValue());
            } else {
                dto.setFinalValue(defaultConfig.getDefaultValue());
            }

            // 파싱URL patterns
            if (StringUtils.isNotBlank(defaultConfig.getUrlPatterns())) {
                try {
                    List<String> urls = JSON.parseArray(defaultConfig.getUrlPatterns(), String.class);
                    dto.setUrls(urls);
                } catch (Exception e) {
                    log.warn("파싱URL patterns실패, resourceCode: {}", defaultConfig.getResourceCode(), e);
                    dto.setUrls(Collections.emptyList());
                }
            } else {
                dto.setUrls(Collections.emptyList());
            }

            fullConfigMap.put(defaultConfig.getResourceCode(), dto);
        }

        // 저장입력저장
        String cacheKey = REDIS_KEY_PREFIX + tenantConfig.getTenantId();
        RedisUtils.set(cacheKey, JSON.toJSONString(fullConfigMap), CACHE_EXPIRE_SECONDS);

        return fullConfigMap;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void regenerateTenantConfig(String tenantId, Map<String, Integer> quotaUpdates) {
        if (StringUtils.isBlank(tenantId) || quotaUpdates == null || quotaUpdates.isEmpty()) {
            return;
        }

        try {
            // 가져오기테넌트매칭기록
            SysTenantConfig tenantConfig = sysTenantConfigDao.selectByTenantId(tenantId);
            if (tenantConfig == null) {
                log.warn("테넌트매칭기록을 찾을 수 없습니다, tenantId: {}", tenantId);
                return;
            }

            // 가져오기버전매칭, 사용가져오기base값
            Long versionId = tenantConfig.getVersionId();
            List<SysVersionDefaultConfig> defaultConfigs = sysVersionDefaultConfigDao.selectByVersionId(versionId);
            Map<String, SysVersionDefaultConfig> defaultConfigMap = defaultConfigs.stream()
                    .collect(Collectors.toMap(SysVersionDefaultConfig::getResourceCode, config -> config));

            // 파싱있음의수정
            Map<String, ResourceConfigDto> currentModifiedMap = new HashMap<>();
            if (StringUtils.isNotBlank(tenantConfig.getExtraConfigJson())
                    && !"{}".equals(tenantConfig.getExtraConfigJson())) {
                try {
                    currentModifiedMap = JSON.parseObject(
                            tenantConfig.getExtraConfigJson(), new TypeReference<Map<String, ResourceConfigDto>>() {});
                } catch (Exception e) {
                    log.warn("파싱있음수정매칭실패, tenantId: {}", tenantId, e);
                }
            }

            // 업데이트final값(직선연결, 아니요계획값)
            for (Map.Entry<String, Integer> entry : quotaUpdates.entrySet()) {
                String resourceCode = entry.getKey();
                Integer newFinalValue = entry.getValue();

                // 가져오기 매칭으로가져오기base값
                SysVersionDefaultConfig defaultConfig = defaultConfigMap.get(resourceCode);
                if (defaultConfig == null) {
                    log.warn("resourceCode와 일치하는 저장 데이터가 없습니다, resourceCode: {}", resourceCode);
                    continue;
                }

                ResourceConfigDto modifiedConfig = currentModifiedMap.get(resourceCode);
                if (modifiedConfig == null) {
                    modifiedConfig = new ResourceConfigDto();
                    modifiedConfig.setType(defaultConfig.getResourceType() == 1 ? "QUOTA" : "SWITCH");
                    modifiedConfig.setBase(defaultConfig.getDefaultValue());
                    currentModifiedMap.put(resourceCode, modifiedConfig);
                }

                // 직선연결final값
                modifiedConfig.setFinalValue(newFinalValue);
            }

            // 생성새의수정JSON(저장있음수정의, final값대기base의아니요저장)
            // 비고: 저장type, base, final필드, 아니요저장urls및parent
            Map<String, Map<String, Object>> newModifiedMap = new HashMap<>();
            for (Map.Entry<String, ResourceConfigDto> entry : currentModifiedMap.entrySet()) {
                ResourceConfigDto config = entry.getValue();
                // 저장final값아니요대기base의
                if (config.getFinalValue() != null
                        && config.getBase() != null
                        && !config.getFinalValue().equals(config.getBase())) {
                    Map<String, Object> configMap = new HashMap<>();
                    configMap.put("type", config.getType());
                    configMap.put("base", config.getBase());
                    configMap.put("final", config.getFinalValue());
                    newModifiedMap.put(entry.getKey(), configMap);
                }
            }

            // 저장까지데이터베이스(저장수정, 결과가있음수정이면비어 있습니다JSON객체)
            if (newModifiedMap.isEmpty()) {
                tenantConfig.setExtraConfigJson("{}");
            } else {
                tenantConfig.setExtraConfigJson(JSON.toJSONString(newModifiedMap));
            }
            tenantConfig.setUpdateTime(new Date());
            sysTenantConfigDao.updateById(tenantConfig);

            // 지우기저장, 아래가져오기시다시 생성
            clearTenantConfigCache(tenantId);

            log.info("테넌트매칭업데이트성공, tenantId: {}, quotaUpdates: {}", tenantId, quotaUpdates);
        } catch (Exception e) {
            log.error("다시 완료테넌트매칭실패, tenantId: {}", tenantId, e);
            throw new RuntimeException("다시 완료테넌트매칭실패", e);
        }
    }

    @Override
    public void clearTenantConfigCache(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            return;
        }
        String cacheKey = REDIS_KEY_PREFIX + tenantId;
        RedisUtils.del(cacheKey);
    }

    @Override
    public ResourceConfigDto getResourceConfig(String tenantId, String resourceCode) {
        Map<String, ResourceConfigDto> configMap = getTenantResourceConfig(tenantId);
        return configMap.get(resourceCode);
    }
}
