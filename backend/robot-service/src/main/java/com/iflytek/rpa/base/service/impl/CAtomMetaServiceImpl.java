package com.iflytek.rpa.base.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.rpa.base.dao.AtomMetaDuplicateLogDao;
import com.iflytek.rpa.base.dao.CAtomMetaDao;
import com.iflytek.rpa.base.entity.*;
import com.iflytek.rpa.base.entity.dto.AtomKeyListDto;
import com.iflytek.rpa.base.entity.dto.AtomListDto;
import com.iflytek.rpa.base.service.CAtomMetaService;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.utils.ListBatchUtil;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author mjren
 * @date 2025-02-18 14:53
 * @copyright Copyright (c) 2025 mjren
 */
@Service("cAtomMetaService")
public class CAtomMetaServiceImpl extends ServiceImpl<CAtomMetaDao, CAtomMeta> implements CAtomMetaService {

    @Autowired
    private CAtomMetaDao cAtomMetaDao;

    @Autowired
    private AtomMetaDuplicateLogDao atomMetaDuplicateLogDao;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    public AppResponse<?> getAtomTree(String atomKey) {
        String atomCommonInfo = cAtomMetaDao.getLatestAtomByKey(atomKey);
        return AppResponse.success(atomCommonInfo);
    }

    @Override
    public AppResponse<?> getAtomListByParentKey(String parentKey) {
        if (StringUtils.isBlank(parentKey)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        List<String> atomContentList = cAtomMetaDao.getLatestAtomListByParentKey(parentKey);
        return AppResponse.success(atomContentList);
    }

    @Override
    public AppResponse<?> getLatestAtomByKey(String atomKey) {
        if (StringUtils.isBlank(atomKey)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        String atomContent = cAtomMetaDao.getLatestAtomByKey(atomKey);
        return AppResponse.success(atomContent);
    }

    @Override
    public AppResponse<?> getLatestAtomsByList(AtomKeyListDto dto) {
        List<String> atomKeyList = dto.getAtomKeyList();
        atomKeyList.removeIf(Objects::isNull);
        if (atomKeyList.isEmpty()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "atomKeyList비워 둘 수 없습니다");
        }
        return AppResponse.success(cAtomMetaDao.getLatestAtomsByList(atomKeyList));
    }

    @Override
    public AppResponse<?> getAtomList(AtomListDto atomListDto) {
        if (atomListDto == null || CollectionUtil.isEmpty(atomListDto.getAtomList())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        if (atomListDto.getAtomList().size() > 500) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE_NOT_SUPPORT, "일다중조회500");
        }

        List<CAtomMeta> atomMetaList = cAtomMetaDao.selectAtomList(atomListDto.getAtomList());
        // 근거atomkey+verison분그룹
        Map<String, String> atomMap = atomMetaList.stream()
                .collect(Collectors.toMap(
                        atom -> atom.getAtomKey() + "_" + atom.getVersion(),
                        CAtomMeta::getAtomContent,
                        (existing, replacement) -> existing // 관리의
                        ));
        List<AtomListDto.Atom> atomList = atomListDto.getAtomList();
        List<String> result = new ArrayList<>();
        for (AtomListDto.Atom atom : atomList) {
            if (null == atom) {
                continue;
            }
            result.add(atomMap.get(atom.getKey() + "_" + atom.getVersion()));
        }

        return AppResponse.success(result);
    }

    @Override
    public AppResponse<?> addAtomCommonInfo(AtomCommon atomCommon) throws JsonProcessingException {
        CAtomMeta atomCommonCount = cAtomMetaDao.getAtomCommonBaseInfoByAtomKey("atomCommon");
        if (atomCommonCount != null) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "데이터완료저장에서, 요청 재복사추가");
        } else {
            // 추가
            CAtomMeta atomMeta = new CAtomMeta();
            atomMeta.setParentKey("root");
            atomMeta.setAtomKey("atomCommon");
            ObjectMapper mapper = new ObjectMapper();
            // null값
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            atomMeta.setAtomContent(mapper.writeValueAsString(atomCommon));
            atomMeta.setCreatorId("1");
            atomMeta.setUpdaterId("1");
            atomMeta.setVersion("1");
            atomMeta.setVersionNum(1000000);
            atomMeta.setDeleted(0);
            cAtomMetaDao.insert(atomMeta);
        }
        return AppResponse.success("업데이트또는추가성공");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> updateAtomCommonInfo(AtomCommon atomCommon) throws JsonProcessingException {
        // 덮어쓰기업데이트, 데이터베이스중있음일기록, 예새의
        CAtomMeta atomMeta = cAtomMetaDao.selectOne(new LambdaQueryWrapper<CAtomMeta>()
                .eq(CAtomMeta::getAtomKey, "atomCommon")
                .eq(CAtomMeta::getDeleted, 0));
        ObjectMapper mapper = new ObjectMapper();
        // null값
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        atomMeta.setAtomContent(mapper.writeValueAsString(atomCommon));
        atomMeta.setUpdateTime(new Date());
        cAtomMetaDao.updateById(atomMeta);

        // 근거새의단계닫기시스템   업데이트모든데이터 의 ParentKey
        // updateAtomParentKey(atomCommon);

        return AppResponse.success("업데이트또는추가성공");
    }

    private void updateAtomParentKey(AtomCommon atomCommon) {
        List<AtomicTree> atomicTree = atomCommon.getAtomicTree();
        List<AtomicTree> atomicTreeExtend = atomCommon.getAtomicTreeExtend();
        atomicTree.addAll(atomicTreeExtend);
        Map<String, String> atomParentKeyMap = new HashMap<>();
        processTreeListToMapWithAllPath(atomicTree, "", atomParentKeyMap);

        Set<String> atomKeys = atomParentKeyMap.keySet();
        List<CAtomMeta> atomList = cAtomMetaDao.getKeyAndParentKeyByKeySet(atomKeys);

        //   c_atom_meta 및  atomParentKeyMap 중의 parentKey행, 결과가아니요, 이면입력업데이트큐중
        List<CAtomMeta> preUpdateList = new ArrayList<>();
        for (CAtomMeta cAtomMeta : atomList) {
            String atomKey = cAtomMeta.getAtomKey();
            String newParentKey = atomParentKeyMap.get(atomKey);
            // 결과가parentKey아니요, 이면업데이트
            if (!Objects.equals(cAtomMeta.getParentKey(), newParentKey)) {
                cAtomMeta.setParentKey(newParentKey);
                preUpdateList.add(cAtomMeta);
            }
        }

        // 량업데이트
        if (!preUpdateList.isEmpty()) {
            ListBatchUtil.process(preUpdateList, 10, updateBatchList -> {
                cAtomMetaDao.updateBatchParentKey(updateBatchList);
            });
        }
    }

    private void processTreeListToMapWithAllPath(
            List<AtomicTree> atomicTreeList, String parentKey, Map<String, String> resultMap) {
        for (AtomicTree atomicTree : atomicTreeList) {
            // 현재기존가능의key
            String key = atomicTree.getKey();
            // 결과가있음, 이면관리
            if (!CollectionUtil.isEmpty(atomicTree.getAtomics())) {
                List<AtomicTree> atomics = atomicTree.getAtomics();
                if (parentKey.isEmpty()) {
                    processTreeListToMapWithAllPath(atomics, key, resultMap);
                } else {
                    processTreeListToMapWithAllPath(atomics, parentKey + '/' + key, resultMap);
                }
            } else {
                resultMap.put(key, parentKey);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> saveAtomicsInfo(Map<String, Atomic> atomNewMap, String saveWay)
            throws JsonProcessingException {
        if (CollectionUtil.isEmpty(atomNewMap)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "JSON 데이터가 비어 있습니다");
        }
        Set<String> atomKeySet = atomNewMap.keySet();
        if (CollectionUtil.isEmpty(atomNewMap)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "key 데이터가 비어 있습니다");
        }
        List<CAtomMeta> insertOrUpdateAtomList = new ArrayList<>();
        // 근거key목록조회새버전atomContent
        // 개  조회출력  새버전의 기존가능
        List<CAtomMeta> atomMetaOldList = cAtomMetaDao.getLatestAtomListByKeySet(atomKeySet);
        String atomCommonInfoStr = cAtomMetaDao.getLatestAtomByKey("atomCommon");
        if (StringUtils.isBlank(atomCommonInfoStr)) {
            return AppResponse.error(ErrorCodeEnum.E_SQL, "공유 데이터 정보가 없습니다");
        }
        // 반대순서열: 를 JSON 변환로객체
        ObjectMapper mapper = new ObjectMapper();

        // 기존가능 의단계닫기시스템
        AtomCommon atomCommon = mapper.readValue(atomCommonInfoStr, AtomCommon.class);
        if (atomCommon == null
                || CollectionUtil.isEmpty(atomCommon.getAtomicTree())
                || CollectionUtil.isEmpty(atomCommon.getAtomicTreeExtend())) {
            return AppResponse.error(ErrorCodeEnum.E_SQL, "단계 대기 정보가 없습니다");
        }
        List<AtomicTree> atomicTree = atomCommon.getAtomicTree();
        List<AtomicTree> atomicTreeExtend = atomCommon.getAtomicTreeExtend();
        atomicTree.addAll(atomicTreeExtend);
        Map<String, String> atomParentKeyMap = new HashMap<>();
        // 완료parentKey및key의Map
        processTreeListToMapWithAllPath(atomicTree, "", atomParentKeyMap);

        // 의기존가능있음있음데이터    가져오기출력 새버전의기존가능 행
        if (!CollectionUtil.isEmpty(atomMetaOldList)) {
            // 근거atomkey분그룹
            Map<String, CAtomMeta> atomMetaOldMap = atomMetaOldList.stream()
                    .collect(Collectors.toMap(
                            CAtomMeta::getAtomKey, cAtomMeta -> cAtomMeta, (existing, replacement) -> existing));

            atomNewMap.forEach((atomKey, atomContentNew) -> {
                CAtomMeta atomMetaOld = atomMetaOldMap.get(atomKey);
                try {
                    //  새 기존가능의예외(새의 기존가능 행)
                    if (null == atomMetaOld || isAtomContentDifferent(atomContentNew, atomMetaOld)) {
                        insertOrUpdateAtomList.add(
                                createAtomMeta(atomParentKeyMap.getOrDefault(atomKey, ""), atomContentNew));
                    }
                } catch (JsonProcessingException e) {
                    log.error("json변환예외: {}", e);
                    throw new RuntimeException(e);
                }
            });
        } else {
            // 직선연결삽입
            atomNewMap.forEach((atomKey, atomContentNew) -> {
                try {
                    insertOrUpdateAtomList.add(
                            createAtomMeta(atomParentKeyMap.getOrDefault(atomKey, ""), atomContentNew));
                } catch (JsonProcessingException e) {
                    log.error("json변환예외: {}", e);
                    throw new RuntimeException(e);
                }
            });
        }

        if (!CollectionUtil.isEmpty(insertOrUpdateAtomList)) {
            if ("insert".equals(saveWay)) {
                // 량삽입
                ListBatchUtil.process(insertOrUpdateAtomList, 50, this::saveBatch);

                // 근거본조회의 key  및버전 조회 데이터베이스중여부있음 복사의데이터, 결과가있음재복사의데이터기록본의요청 
                checkDuplicateData(insertOrUpdateAtomList, atomNewMap, saveWay);
            } else if ("update".equals(saveWay)) {
                ListBatchUtil.process(insertOrUpdateAtomList, 50, updateBatchList -> {
                    cAtomMetaDao.UpdateBatchByKeyAndVersion(updateBatchList);
                });
            }
        }

        return AppResponse.success("저장성공");
    }

    private void checkDuplicateData(
            List<CAtomMeta> insertOrUpdateAtomList, Map<String, Atomic> atomNewMap, String saveWay) {
        for (CAtomMeta cAtomMeta : insertOrUpdateAtomList) {
            String atomKey = cAtomMeta.getAtomKey();
            String version = cAtomMeta.getVersion();
            // 사용 MyBatis XML , 근거 atomKey 및 version 조회삭제되지 않음의데이터
            List<CAtomMeta> existingAtomMeta = cAtomMetaDao.selectByKeyAndVersion(atomKey, version);
            if (existingAtomMeta.size() > 1) {
                // 가져오기 요청 
                Map map = new HashMap();
                map.put("atomMap", atomNewMap);
                map.put("saveWay", saveWay);
                String bodyStr = map.toString();

                log.error(String.format("발송재복사데이터, atomKey: %s, version: %s, 요청 : %s", atomKey, version, bodyStr));
                AtomMetaDuplicateLog atomMetaDuplicateLog = new AtomMetaDuplicateLog();
                atomMetaDuplicateLog.setAtomKey(atomKey);
                atomMetaDuplicateLog.setVersion(version);
                atomMetaDuplicateLog.setRequestBody(bodyStr);
                atomMetaDuplicateLog.setCreatorId(1L);
                atomMetaDuplicateLog.setUpdaterId(1L);
                atomMetaDuplicateLog.setDeleted(0);
                atomMetaDuplicateLog.setCreateTime(new Date());
                atomMetaDuplicateLog.setUpdateTime(new Date());
                atomMetaDuplicateLogDao.insert(atomMetaDuplicateLog);
            }
        }
    }

    /**
     * 가져오기key, parentKey값
     * @param atomicTreeList
     * @param parentKey
     * @param resultMap
     */
    private void processTreeListToMap(
            List<AtomicTree> atomicTreeList, String parentKey, Map<String, String> resultMap) {
        for (AtomicTree atomicTree : atomicTreeList) {
            String key = atomicTree.getKey();
            resultMap.put(key, parentKey);

            if (!CollectionUtil.isEmpty(atomicTree.getAtomics())) {
                List<AtomicTree> atomics = atomicTree.getAtomics();
                processTreeListToMap(atomics, key, resultMap);
            }
        }
    }

    private CAtomMeta createAtomMeta(String parentKey, Atomic atomContentNew) throws JsonProcessingException {
        CAtomMeta atomMeta = new CAtomMeta();
        atomMeta.setParentKey(parentKey);
        atomMeta.setAtomKey(atomContentNew.getKey());
        ObjectMapper mapper = new ObjectMapper();
        // null값
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        atomMeta.setAtomContent(mapper.writeValueAsString(atomContentNew));
        atomMeta.setCreatorId("1");
        atomMeta.setUpdaterId("1");
        atomMeta.setVersion(atomContentNew.getVersion());
        atomMeta.setVersionNum(getVersion(atomContentNew.getVersion()));
        atomMeta.setDeleted(0);
        return atomMeta;
    }

    private Integer getVersion(String version) {
        if (StringUtils.isBlank(version)) {
            throw new IllegalArgumentException("버전실패");
        }
        String[] versionSplit = version.split("\\.");
        int splitSize = versionSplit.length;
        // 확인버전다중있음3모듈분(major, minor, patch)
        if (splitSize > 3) {
            throw new IllegalArgumentException("버전 형식이 올바르지 않습니다");
        }
        //  major, minor, patch
        int major = 0;
        int minor = 0;
        int patch = 0;
        // 파싱 major
        if (splitSize >= 1) {
            major = Integer.parseInt(versionSplit[0]);
        }
        // 파싱 minor
        if (splitSize >= 2) {
            minor = Integer.parseInt(versionSplit[1]);
        }
        // 파싱 patch
        if (splitSize >= 3) {
            patch = Integer.parseInt(versionSplit[2]);
        }
        // 계획변환후의버전
        return major * 1_000_000 + minor * 1_000 + patch;
    }

    private Boolean isAtomContentDifferent(Atomic newAtom, CAtomMeta oldAtomMeta) throws JsonProcessingException {
        // version,여부있음아니요
        Atomic atomic = new Atomic();
        BeanUtils.copyProperties(newAtom, atomic);
        atomic.setVersion(null);
        String oldAtomContent = oldAtomMeta.getAtomContent();
        // 변환로객체
        ObjectMapper mapper = new ObjectMapper();
        Atomic oldAtom = mapper.readValue(oldAtomContent, Atomic.class);
        oldAtom.setVersion(null);
        // 여부있음아니요
        return !areObjectsEqual(atomic, oldAtom);
    }

    private boolean areObjectsEqual(Object obj1, Object obj2) {
        // 가져오기객체의모든필드
        Field[] fields = obj1.getClass().getDeclaredFields();
        // 필드수
        if (fields.length != obj2.getClass().getDeclaredFields().length) {
            return false;
        }
        // 매개필드의이름및값
        for (Field field : fields) {
            field.setAccessible(true); // 허용방문있음필드
            try {
                Object value1 = field.get(obj1);
                Object value2 = field.get(obj2);

                // 필드값
                if (value1 == null) {
                    if (value2 != null) {
                        return false;
                    }
                } else if (!value1.equals(value2)) {
                    return false;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    @Override
    public Map getLatestAllAtoms() throws JsonProcessingException {
        List<String> atomContentList = cAtomMetaDao.getLatestAllAtoms();
        HashMap<String, Atomic> map = new HashMap<>();
        for (String atomConten : atomContentList) {
            ObjectMapper mapper = new ObjectMapper();
            Atomic atomic = mapper.readValue(atomConten, Atomic.class);
            String key = atomic.getKey();
            map.put(key, atomic);
        }
        return map;
    }
}