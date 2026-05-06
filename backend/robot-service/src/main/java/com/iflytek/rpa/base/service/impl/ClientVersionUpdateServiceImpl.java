package com.iflytek.rpa.base.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.base.dao.ClientVersionUpdateDao;
import com.iflytek.rpa.base.entity.ClientUpdateVersion;
import com.iflytek.rpa.base.entity.dto.ClientVersionCheckDto;
import com.iflytek.rpa.base.entity.dto.ClientVersionUpdateDto;
import com.iflytek.rpa.base.entity.vo.ClientVersionCheckVo;
import com.iflytek.rpa.base.entity.vo.ClientVersionUpdateVo;
import com.iflytek.rpa.base.service.ClientVersionUpdateService;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 클라이언트버전업데이트서비스
 */
@Service
public class ClientVersionUpdateServiceImpl extends ServiceImpl<ClientVersionUpdateDao, ClientUpdateVersion>
        implements ClientVersionUpdateService {
    @Autowired
    private IdWorker idWorker;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<ClientVersionUpdateVo> save(ClientVersionUpdateDto dto) throws Exception {
        // 변환버전
        Integer versionNum = getVersionNum(dto.getVersion());
        // 조회버전여부완료저장에서
        ClientUpdateVersion existing = baseMapper.getByVersionNum(versionNum);
        if (existing != null && existing.getDeleted() == 0) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "버전완료저장에서");
        }
        // 생성객체
        ClientUpdateVersion entity = new ClientUpdateVersion();
        long id = idWorker.nextId();
        entity.setId(id);
        entity.setVersion(dto.getVersion());
        entity.setVersionNum(versionNum);
        entity.setDownloadUrl(dto.getDownloadUrl());
        entity.setUpdateInfo(dto.getUpdateInfo());
        entity.setOs(dto.getOs());
        entity.setArch(dto.getArch());
        entity.setDeleted(0);
        // 삽입데이터베이스
        int result = baseMapper.insert(entity);
        if (result <= 0) {
            return AppResponse.error(ErrorCodeEnum.E_SQL, "추가실패");
        }
        // 변환로VO반환
        ClientVersionUpdateVo vo = new ClientVersionUpdateVo();
        BeanUtils.copyProperties(entity, vo);
        return AppResponse.success(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<ClientVersionUpdateVo> update(ClientVersionUpdateDto dto) throws Exception {
        if (dto.getId() == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "ID비워 둘 수 없습니다");
        }
        // 조회기존기록
        ClientUpdateVersion existing = baseMapper.selectById(dto.getId());
        if (existing == null || existing.getDeleted() == 1) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EMPTY, "기록을 찾을 수 없습니다");
        }
        String version = dto.getVersion();
        Integer versionNum = getVersionNum(version);
        // 결과가버전있음변수, 필요조회새버전여부완료저장에서
        if (!existing.getVersion().equals(dto.getVersion())) {
            ClientUpdateVersion versionCheck = baseMapper.getByVersionNum(versionNum);
            if (versionCheck != null
                    && versionCheck.getDeleted() == 0
                    && !versionCheck.getId().equals(dto.getId())) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM, "버전완료저장에서");
            }
        }
        // 업데이트객체
        ClientUpdateVersion entity = new ClientUpdateVersion();
        entity.setId(dto.getId());
        entity.setVersion(dto.getVersion());
        entity.setVersionNum(versionNum);
        entity.setDownloadUrl(dto.getDownloadUrl());
        entity.setUpdateInfo(dto.getUpdateInfo());
        // 업데이트데이터베이스
        int result = baseMapper.updateById(entity);
        if (result <= 0) {
            return AppResponse.error(ErrorCodeEnum.E_SQL, "업데이트실패");
        }
        // 조회업데이트후의기록
        ClientUpdateVersion updated = baseMapper.selectById(dto.getId());
        // 변환로VO반환
        ClientVersionUpdateVo vo = new ClientVersionUpdateVo();
        BeanUtils.copyProperties(updated, vo);
        return AppResponse.success(vo);
    }

    @Override
    public AppResponse<ClientVersionCheckVo> checkVersion(ClientVersionCheckDto dto) throws Exception {
        // 조회새버전
        ClientUpdateVersion latestVersion = baseMapper.getLatestVersion(dto.getOs(), dto.getArch());
        // 생성반환VO
        ClientVersionCheckVo vo = new ClientVersionCheckVo();
        // 결과가있음새버전, 반환아니요필요업데이트
        if (latestVersion == null) {
            vo.setNeedUpdate(0);
            vo.setVersion(null);
            vo.setUpdateInfo(null);
            vo.setDownloadUrl(null);
            return AppResponse.success(vo);
        }
        // 현재버전및새버전
        String currentVersion = dto.getVersion();
        String latestVersionStr = latestVersion.getVersion();
        // 결과가버전, 아니요필요업데이트
        if (currentVersion.equals(latestVersionStr)) {
            vo.setNeedUpdate(0);
            vo.setVersion(latestVersionStr);
            vo.setOs(null);
            vo.setArch(null);
            vo.setUpdateInfo(null);
            vo.setDownloadUrl(null);
        } else {
            // 버전아니요, 필요업데이트
            vo.setNeedUpdate(1);
            vo.setVersion(latestVersionStr);
            vo.setOs(latestVersion.getOs());
            vo.setArch(latestVersion.getArch());
            vo.setUpdateInfo(latestVersion.getUpdateInfo());
            vo.setDownloadUrl(latestVersion.getDownloadUrl());
        }
        return AppResponse.success(vo);
    }

    @Override
    public String checkVersionSimple(String os, String arch, String version) throws Exception {
        // 조회전체영역새버전
        ClientUpdateVersion latestVersion = baseMapper.getLatestVersion(os, arch);
        // 결과가있음새버전, 반환null(테이블완료예새)
        if (latestVersion == null) {
            return null;
        }
        // 현재버전및새버전
        String latestVersionStr = latestVersion.getVersion();
        // 결과가버전, 반환null(테이블완료예새)
        if (version.equals(latestVersionStr)) {
            return null;
        }
        // 버전아니요, 반환새버전의다운로드URL
        return latestVersion.getDownloadUrl();
    }

    /**
     * 를버전문자열변환로버전숫자
     *
     * @param version 버전문자열, 예 "1.2.3"
     * @return 버전숫자
     */
    private Integer getVersionNum(String version) {
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
}