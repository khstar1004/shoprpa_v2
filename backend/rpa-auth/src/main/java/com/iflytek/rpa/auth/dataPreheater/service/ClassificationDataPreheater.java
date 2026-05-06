package com.iflytek.rpa.auth.dataPreheater.service;

import com.iflytek.rpa.auth.core.service.TenantService;
import com.iflytek.rpa.auth.dataPreheater.dao.AppMarketClassificationDao;
import com.iflytek.rpa.auth.dataPreheater.entity.InitDataEvent;
import com.iflytek.rpa.auth.utils.AppResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 앱 마켓-분유형
 */
@Component
@Slf4j
public class ClassificationDataPreheater {

    @Autowired
    private AppMarketClassificationDao appMarketClassificationDao;

    @Autowired
    private TenantService tenantService;

    @EventListener(classes = {ApplicationReadyEvent.class, InitDataEvent.class})
    @Transactional(rollbackFor = Exception.class)
    public void importClassification(Object event) throws Exception {
        log.info("-----------------열기 가져오기분유형데이터-----------------");
        AppResponse<List<String>> noClassifyTenantIds = tenantService.getNoClassifyTenantIds();
        List<String> tenantIds = new ArrayList<>();
        if (Objects.nonNull(noClassifyTenantIds) && noClassifyTenantIds.ok() && noClassifyTenantIds.getData() != null) {
            tenantIds = noClassifyTenantIds.getData();
        }

        for (String tenantId : tenantIds) {
            // 행데이터데이터테이블조회재복사삽입
            Integer dataCount = appMarketClassificationDao.countByTenantId(tenantId);
            if (dataCount > 0) {
                continue;
            }

            Integer i = appMarketClassificationDao.insertDefaultClassification(tenantId);
            log.info("테넌트[{}]분유형데이터가져오기완료, 가져오기 데이터: {}", tenantId, i);
        }
        if (!tenantIds.isEmpty()) {
            // 통신경과Feign호출rpa-auth서비스업데이트테넌트분유형로그
            AppResponse<Integer> updateResponse = tenantService.updateTenantClassifyCompleted(tenantIds);
            Integer i = -1;
            if (Objects.nonNull(updateResponse) && updateResponse.ok() && updateResponse.getData() != null) {
                i = updateResponse.getData();
            }

            if (i != tenantIds.size()) {
                throw new RuntimeException("분유형데이터가져오기예외, 확인하세요!");
            }
        }
        log.info("-----------------분유형데이터가져오기완료-----------------");
    }
}