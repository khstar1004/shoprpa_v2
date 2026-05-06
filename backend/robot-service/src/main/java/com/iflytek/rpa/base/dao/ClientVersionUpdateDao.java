package com.iflytek.rpa.base.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.base.entity.ClientUpdateVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 클라이언트버전업데이트DAO연결
 *
 * @author system
 * @since 2025-01-XX
 */
@Mapper
public interface ClientVersionUpdateDao extends BaseMapper<ClientUpdateVersion> {

    /**
     * 근거버전조회버전정보
     *
     * @param version 버전
     * @return 버전
     */
    ClientUpdateVersion getByVersionNum(@Param("versionNum") Integer version);

    /**
     * 조회새버전(versionNum대)
     *
     * @param os 운영체제
     * @param arch 아키텍처
     */
    ClientUpdateVersion getLatestVersion(@Param("os") String os, @Param("arch") String arch);

    /**
     * 조회전체영역새버전(versionNum대, 아니요분운영체제및아키텍처)
     *
     * @return 새버전정보
     */
    ClientUpdateVersion getGlobalLatestVersion();
}