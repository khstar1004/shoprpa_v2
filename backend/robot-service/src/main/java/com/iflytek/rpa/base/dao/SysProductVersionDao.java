package com.iflytek.rpa.base.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.base.entity.SysProductVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 제품품목버전테이블 Mapper 연결
 */
@Mapper
public interface SysProductVersionDao extends BaseMapper<SysProductVersion> {

    /**
     * 근거버전코드조회버전정보
     */
    @Select("SELECT * FROM sys_product_version WHERE version_code = #{versionCode} AND deleted = 0 LIMIT 1")
    SysProductVersion selectByVersionCode(@Param("versionCode") String versionCode);
}