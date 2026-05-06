package com.iflytek.rpa.monitor.dao;

import com.iflytek.rpa.monitor.entity.HisDataEnum;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 관리관리데이터조각매칭데이터(HisDataEnum)테이블데이터베이스방문
 *
 * @author mjren
 * @since 2024-11-01 11:36:34
 */
@Mapper
public interface HisDataEnumDao {

    List<HisDataEnum> getEnumByParentCode(@Param("parentCode") String parentCode);
}