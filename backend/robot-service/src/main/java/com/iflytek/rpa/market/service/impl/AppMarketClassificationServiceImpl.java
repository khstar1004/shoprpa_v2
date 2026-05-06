package com.iflytek.rpa.market.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.market.dao.AppApplicationDao;
import com.iflytek.rpa.market.dao.AppMarketClassificationDao;
import com.iflytek.rpa.market.entity.AppMarketClassification;
import com.iflytek.rpa.market.entity.dto.AppMarketClassificationEditDto;
import com.iflytek.rpa.market.entity.dto.AppMarketClassificationManageRequest;
import com.iflytek.rpa.market.entity.dto.AppMarketClassificationManageVo;
import com.iflytek.rpa.market.entity.dto.MarketInfoDto;
import com.iflytek.rpa.market.entity.vo.AppMarketClassificationVo;
import com.iflytek.rpa.market.service.AppApplicationService;
import com.iflytek.rpa.market.service.AppMarketClassificationService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 앱 마켓분유형서비스유형
 *
 * @author auto-generated
 */
@Slf4j
@Service
public class AppMarketClassificationServiceImpl implements AppMarketClassificationService {

    @Autowired
    private AppMarketClassificationDao appMarketClassificationDao;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Resource
    private AppApplicationService appApplicationService;

    @Autowired
    private AppApplicationDao appApplicationDao;

    @Override
    public AppResponse<List<AppMarketClassificationVo>> getClassificationList() throws NoLoginException {
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        List<AppMarketClassificationVo> classificationList =
                appMarketClassificationDao.getClassificationListByTenantId(tenantId);
        return AppResponse.success(classificationList);
    }

    @Override
    public AppResponse<List<AppMarketClassificationManageVo>> getClassificationManageList(
            AppMarketClassificationManageRequest request) throws NoLoginException, JsonProcessingException {
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        List<AppMarketClassificationManageVo> classificationList =
                appMarketClassificationDao.getClassificationManageList(
                        tenantId, request.getName(), request.getSource());
        // 사용
        packageReference(classificationList);
        return AppResponse.success(classificationList);
    }

    private void packageReference(List<AppMarketClassificationManageVo> classificationList)
            throws NoLoginException, JsonProcessingException {
        if (classificationList == null || classificationList.isEmpty()) {
            return;
        }
        // 가져오기분유형사용시스템계획
        List<Map> referenceCountList = appMarketClassificationDao.getCategoryReferenceCount();
        Map<Long, Integer> referenceCountMap = referenceCountList.stream()
                .collect(Collectors.toMap(
                        map -> Long.valueOf(map.get("category").toString()),
                        map -> Integer.valueOf(map.get("reference_count").toString())));

        // 열기시작위검토
        AppResponse<String> auditStatus = appApplicationService.getAuditStatus();
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        if (auditStatus.ok()) {
            Map<Long, Integer> referenceCountForPendingMap = new HashMap<>();
            List<String> pendingMarketInfoJson = appApplicationDao.getPendingMarketInfoJson(tenantId);
            for (String marketInfoJson : pendingMarketInfoJson) {
                ObjectMapper objectMapper = new ObjectMapper();
                MarketInfoDto marketInfoDto = objectMapper.readValue(marketInfoJson, MarketInfoDto.class);
                String category = marketInfoDto.getCategory();
                Long categoryId = Long.valueOf(category);
                List<String> marketIdList = marketInfoDto.getMarketIdList();
                int num = marketIdList.size();
                Integer orDefault = referenceCountForPendingMap.getOrDefault(categoryId, 0);
                referenceCountForPendingMap.put(categoryId, orDefault + num);
            }
            if (referenceCountMap.isEmpty()) {
                referenceCountMap = new HashMap<>(referenceCountForPendingMap);
            } else {
                for (Map.Entry<Long, Integer> entry : referenceCountForPendingMap.entrySet()) {
                    referenceCountMap.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
        }

        // 로매개분유형사용데이터
        for (AppMarketClassificationManageVo classification : classificationList) {
            Integer referenceCount = referenceCountMap.get(classification.getId());
            classification.setReference(referenceCount != null ? referenceCount : 0);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<String> addClassification(AppMarketClassificationEditDto request) throws NoLoginException {
        if (!StringUtils.hasText(request.getName())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "분유형이름비워 둘 수 없습니다");
        }
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();

        // 조회분유형이름여부완료저장에서
        List<AppMarketClassificationVo> existingClassifications =
                appMarketClassificationDao.getClassificationListByTenantId(tenantId);
        boolean nameExists = existingClassifications.stream()
                .anyMatch(classification -> request.getName().equals(classification.getName()));
        if (nameExists) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK, "분유형이름완료저장에서");
        }

        int maxSort = 0;
        if (!existingClassifications.isEmpty()) {
            maxSort = existingClassifications.stream()
                    .map(AppMarketClassificationVo::getSort)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0);
        }

        // 생성새분유형
        AppMarketClassification classification = new AppMarketClassification();
        Date now = new Date();
        classification.setName(request.getName());
        classification.setSource(1); // 1-지정
        classification.setSort(maxSort + 1); // 정렬
        classification.setTenantId(tenantId);
        classification.setCreatorId(userId);
        classification.setCreateTime(now);
        classification.setUpdaterId(userId);
        classification.setUpdateTime(now);
        classification.setDeleted(0);
        int result = appMarketClassificationDao.insert(classification);
        if (result > 0) {
            return AppResponse.success("추가성공");
        } else {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EXCEPTION, "추가분유형실패");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<String> editClassification(AppMarketClassificationEditDto request) throws NoLoginException {
        if (request.getId() == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "ID비워 둘 수 없습니다");
        }
        if (!StringUtils.hasText(request.getName())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "분유형이름비워 둘 수 없습니다");
        }

        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        AppMarketClassification existingClassification = appMarketClassificationDao.selectById(request.getId());
        if (existingClassification == null) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EMPTY, "분류를 찾을 수 없습니다");
        }
        if (!tenantId.equals(existingClassification.getTenantId())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK, "권한이 없습니다수정해당분유형");
        }

        // 조회분유형이름여부완료저장에서(정렬제거현재분유형)
        List<AppMarketClassificationVo> existingClassifications =
                appMarketClassificationDao.getClassificationListByTenantId(tenantId);
        boolean nameExists = existingClassifications.stream()
                .anyMatch(classification -> request.getName().equals(classification.getName()));
        if (nameExists) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK, "분유형이름완료저장에서");
        }

        existingClassification.setName(request.getName());
        existingClassification.setUpdaterId(userId);
        Date now = new Date();
        existingClassification.setUpdateTime(now);

        int result = appMarketClassificationDao.updateById(existingClassification);
        if (result > 0) {
            return AppResponse.success("수정성공");
        } else {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EXCEPTION, "수정분유형실패");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<String> deleteClassification(AppMarketClassificationEditDto request) throws NoLoginException {
        if (request.getId() == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "ID비워 둘 수 없습니다");
        }
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        AppMarketClassification existingClassification = appMarketClassificationDao.selectById(request.getId());
        if (existingClassification == null) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EMPTY, "분류를 찾을 수 없습니다");
        }
        if (!tenantId.equals(existingClassification.getTenantId())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK, "권한이 없습니다삭제해당분유형");
        }
        existingClassification.setDeleted(1);
        existingClassification.setUpdaterId(userId);
        Date now = new Date();
        existingClassification.setUpdateTime(now);
        int result = appMarketClassificationDao.updateById(existingClassification);
        if (result > 0) {
            return AppResponse.success("삭제성공");
        } else {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EXCEPTION, "삭제분유형실패");
        }
    }
}