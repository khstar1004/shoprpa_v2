package com.iflytek.rpa.base.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.rpa.base.dao.CAtomMetaNewDao;
import com.iflytek.rpa.base.entity.AtomCommon;
import com.iflytek.rpa.base.entity.AtomicTree;
import com.iflytek.rpa.base.entity.CAtomMetaNew;
import com.iflytek.rpa.base.entity.vo.CAtomMetaNewVo;
import com.iflytek.rpa.base.service.CAtomMetaNewService;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.TenantExpirationDto;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * 새기존가능Service
 */
@Service("cAtomMetaNewService")
public class CAtomMetaNewServiceImpl extends ServiceImpl<CAtomMetaNewDao, CAtomMetaNew> implements CAtomMetaNewService {

    @Autowired
    private CAtomMetaNewDao cAtomMetaNewDao;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    public AppResponse<String> getAtomTree() throws JsonProcessingException {
        String atomContent = cAtomMetaNewDao.getAtomContentByKey("atomCommon");
        if (StringUtils.isEmpty(atomContent)) {
            return AppResponse.success("");
        }
        AppResponse<TenantExpirationDto> resp = rpaAuthFeign.getExpiration();
        if (resp == null || !resp.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        TenantExpirationDto data = resp.getData();
        String tenantType = data.getTenantType();

        // 개사람버전 제거 remote 닫기기존가능
        if (tenantType.equals("personal")) {
            Set<String> remoteKeys = new HashSet<>();
            remoteKeys.add("enterprise");
            remoteKeys.add("Enterprise.upload_to_sharefolder");
            remoteKeys.add("Enterprise.download_from_sharefolder");
            remoteKeys.add("Enterprise.get_shared_variable");
            ObjectMapper objectMapper = new ObjectMapper();
            AtomCommon atomCommon = objectMapper.readValue(atomContent, AtomCommon.class);
            List<AtomicTree> atomicTreeList = atomCommon.getAtomicTree();
            atomicTreeList.removeIf(atomicTree -> remoteKeys.contains(atomicTree.getKey()));
            for (AtomicTree atomicTree : atomicTreeList) {
                List<AtomicTree> atomics = atomicTree.getAtomics();
                atomics.removeIf(atomic -> remoteKeys.contains(atomic.getKey()));
                if (null != atomics) {
                    for (AtomicTree atomic : atomics) {
                        List<AtomicTree> innerAtomics = atomic.getAtomics();
                        if (null != innerAtomics) {
                            innerAtomics.removeIf(innerAtomic -> remoteKeys.contains(innerAtomic.getKey()));
                        }
                    }
                }
            }
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            atomContent = objectMapper.writeValueAsString(atomCommon);
        }
        return AppResponse.success(atomContent);
    }

    @Override
    public AppResponse<List<CAtomMetaNewVo>> getListByKeys(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "keys비워 둘 수 없습니다");
        }
        // 제거null값
        keys.removeIf(Objects::isNull);
        if (keys.isEmpty()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "keys비워 둘 수 없습니다");
        }
        List<CAtomMetaNewVo> result = cAtomMetaNewDao.getListByKeys(keys);
        return AppResponse.success(result);
    }

    @Override
    public List<CAtomMetaNewVo> getAll() {
        return cAtomMetaNewDao.getAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<String> allUpdate(List<CAtomMetaNewVo> atomMetaNewVoList) {
        if (CollectionUtils.isEmpty(atomMetaNewVoList)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "데이터비워 둘 수 없습니다");
        }

        // 가져오기데이터베이스중있음의모든기록
        List<CAtomMetaNewVo> existingList = cAtomMetaNewDao.getAll();
        Set<String> existingKeySet =
                existingList.stream().map(CAtomMetaNewVo::getAtomKey).collect(Collectors.toSet());

        // 가져오기 입력의모든atomKey
        Set<String> inputKeySet = atomMetaNewVoList.stream()
                .map(CAtomMetaNewVo::getAtomKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 출력필요삭제의기록(데이터베이스중저장된 입력데이터중찾을 수 없습니다)
        Set<String> keysToDelete = new HashSet<>(existingKeySet);
        keysToDelete.removeAll(inputKeySet);

        // 삭제데이터베이스중다중의기록
        for (String keyToDelete : keysToDelete) {
            cAtomMetaNewDao.deleteByAtomKey(keyToDelete);
        }

        // 입력의데이터행삽입또는업데이트
        for (CAtomMetaNewVo vo : atomMetaNewVoList) {
            if (vo.getAtomKey() == null) {
                continue;
            }
            if (existingKeySet.contains(vo.getAtomKey())) {
                // 저장에서이면업데이트
                cAtomMetaNewDao.updateByAtomKey(vo.getAtomKey(), vo.getAtomContent(), vo.getSort());
            } else {
                // 찾을 수 없습니다이면삽입
                CAtomMetaNew entity = new CAtomMetaNew();
                entity.setAtomKey(vo.getAtomKey());
                entity.setAtomContent(vo.getAtomContent());
                entity.setSort(vo.getSort());
                entity.setCreateTime(new Date());
                entity.setUpdateTime(new Date());
                cAtomMetaNewDao.insert(entity);
            }
        }

        return AppResponse.success("업데이트성공");
    }

    @Override
    public AppResponse<String> updateAtomTree(String treeContent) {
        int i = cAtomMetaNewDao.updateByAtomKey("atomCommon", treeContent, 1);
        if (i > 0) {
            return AppResponse.success("업데이트성공");
        }
        return AppResponse.error("업데이트실패");
    }
}