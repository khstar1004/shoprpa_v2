package com.iflytek.rpa.robot.service;

import com.iflytek.rpa.monitor.entity.HisDataEnum;
import java.util.List;

/**
 * 관리관리데이터조각매칭데이터(HisDataEnum)테이블서비스연결
 *
 * @author mjren
 * @since 2024-11-01 11:36:34
 */
public interface HisDataEnumService {

    <T> List<HisDataEnum> getOverViewData(String parentCode, T hisCloudBase, Class<T> clazz);
}