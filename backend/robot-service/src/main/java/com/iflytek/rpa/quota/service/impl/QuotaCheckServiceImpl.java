package com.iflytek.rpa.quota.service.impl;

import com.iflytek.rpa.base.entity.dto.ResourceConfigDto;
import com.iflytek.rpa.base.service.TenantResourceService;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.quota.service.QuotaCheckService;
import com.iflytek.rpa.quota.service.QuotaCountService;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 매칭금액검증도구유형
 * 시스템일의매칭금액검증방법법, 패키지저장기기제어
 */
@Slf4j
@Component
public class QuotaCheckServiceImpl implements QuotaCheckService {

    @Autowired
    private TenantResourceService tenantResourceService;

    @Autowired
    private QuotaCountService quotaCountService;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    public boolean checkDesignerQuota() {
        return checkQuota("designer_count", () -> {
            String tenantId = getTenantId();
            String userId = getUserId();
            return quotaCountService.getDesignerCount(tenantId, userId);
        });
    }

    @Override
    public boolean checkMarketJoinQuota() {
        return checkQuota("market_join_count", () -> {
            String tenantId = getTenantId();
            String userId = getUserId();
            return quotaCountService.getMarketJoinCount(tenantId, userId);
        });
    }

    /**
     * 검증매칭금액출력예외(결과가초과제한)
     * @param resourceCode 코드
     * @param currentCountSupplier 가져오기현재수의데이터
     * @throws ServiceException 결과가매칭금액초과제한
     */
    private boolean checkQuota(String resourceCode, java.util.function.Supplier<Integer> currentCountSupplier) {
        try {
            // 가져오기테넌트ID
            String tenantId = getTenantId();
            if (StringUtils.isBlank(tenantId)) {
                log.warn("불가가져오기테넌트ID, 건너뛰기매칭금액검증");
                return true;
            }

            // 가져오기 매칭
            ResourceConfigDto config = tenantResourceService.getResourceConfig(tenantId, resourceCode);
            if (config == null) {
                log.warn("매칭찾을 수 없습니다, 건너뛰기매칭금액검증, resourceCode: {}", resourceCode);
                return true;
            }

            // 조회단계여부있음
            if (StringUtils.isNotBlank(config.getParent())) {
                ResourceConfigDto parentConfig = tenantResourceService.getResourceConfig(tenantId, config.getParent());
                if (parentConfig == null || parentConfig.getFinalValue() == null || parentConfig.getFinalValue() == 0) {
                    throw new ServiceException("단계사용할 수 없습니다");
                }
            }

            // 가져오기매칭금액제한제어
            Integer quotaLimit = config.getFinalValue();
            if (quotaLimit == null || quotaLimit < 0) {
                // -1테이블아니요제한, 직선연결통신경과
                return true;
            }

            // 가져오기현재수(사용저장)
            Integer currentCount = currentCountSupplier.get();
            if (currentCount == null) {
                currentCount = 0;
            }

            // 검증매칭금액
            if (currentCount >= quotaLimit) {
                log.warn(
                        "매칭금액완료초과제한, resourceCode: {}, currentCount: {}, quotaLimit: {}",
                        resourceCode,
                        currentCount,
                        quotaLimit);
                return false;
            }

            return true;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("매칭금액검증실패, resourceCode: {}", resourceCode, e);
            // 발송예외시, 로완료아니요서비스, 반환true(허용통신경과)
            return true;
        }
    }

    /**
     * 가져오기테넌트ID
     */
    private String getTenantId() {
        try {
            AppResponse<String> response = rpaAuthFeign.getTenantId();
            if (response != null && response.ok() && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.debug("에서인증 서비스가져오기테넌트ID실패", e);
        }
        return null;
    }

    /**
     * 가져오기사용자ID
     */
    private String getUserId() {
        try {
            AppResponse<User> response = rpaAuthFeign.getLoginUser();
            if (response != null && response.ok() && response.getData() != null) {
                return response.getData().getId();
            }
        } catch (Exception e) {
            log.debug("에서인증 서비스가져오기사용자ID실패", e);
        }
        return null;
    }
}