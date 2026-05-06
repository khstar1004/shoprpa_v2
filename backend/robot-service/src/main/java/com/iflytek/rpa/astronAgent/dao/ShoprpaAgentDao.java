package com.iflytek.rpa.astronAgent.dao;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * ShoprpaAgent데이터방문
 */
@Mapper
public interface ShoprpaAgentDao {

    /**
     * 근거사용자ID가져오기테넌트ID목록
     * @param databaseName 데이터베이스이름
     * @param userId 사용자ID
     * @return 테넌트ID목록
     */
    List<String> getTenantIdsByUserId(@Param("databaseName") String databaseName, @Param("userId") String userId);

    /**
     * 근거테넌트ID목록가져오기개사람테넌트ID
     * @param databaseName 데이터베이스이름
     * @param tenantIds 테넌트ID목록
     * @return 개사람테넌트ID
     */
    String getPersonalTenantId(@Param("databaseName") String databaseName, @Param("tenantIds") List<String> tenantIds);
}