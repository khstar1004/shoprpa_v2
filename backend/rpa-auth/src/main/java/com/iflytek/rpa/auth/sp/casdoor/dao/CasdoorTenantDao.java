package com.iflytek.rpa.auth.sp.casdoor.dao;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Casdoor 테넌트데이터방문연결
 *
 * @author Auto Generated
 * @create 2025/12/11
 */
@Mapper
public interface CasdoorTenantDao {

    /**
     * 가져오기지원하지 않는유형의테넌트ID목록
     *
     * @param databaseName 데이터베이스이름
     * @return 지원하지 않는유형의테넌트ID목록
     */
    List<String> getNoClassifyTenantIds(@Param("databaseName") String databaseName);
}