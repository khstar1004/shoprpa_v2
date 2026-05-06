package com.iflytek.rpa.market.dao;

import com.iflytek.rpa.market.entity.AppApplicationTenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AppApplicationTenantDao {

    /**
     * 근거테넌트ID조회검토열기닫기상태
     * @param tenantId 테넌트ID
     * @return 검토열기닫기상태
     */
    AppApplicationTenant getByTenantId(@Param("tenantId") String tenantId);

    /**
     * 업데이트검토열기닫기상태
     * @param tenantId 테넌트ID
     * @param auditEnable 검토열기닫기상태 1-사용 0-사용 안 함
     * @param operator 사람
     * @param reason 변수변경원인
     * @return 업데이트결과
     */
    int updateAuditEnable(
            @Param("tenantId") String tenantId,
            @Param("auditEnable") Short auditEnable,
            @Param("operator") String operator,
            @Param("reason") String reason);

    /**
     * 삽입검토열기종료 매칭
     * @param tenantId 테넌트ID
     * @param auditEnable 검토열기닫기상태 1-사용 0-사용 안 함
     * @param operator 사람
     * @param reason 변수변경원인
     * @return 삽입결과
     */
    int insertOrUpdateAuditEnable(
            @Param("tenantId") String tenantId,
            @Param("auditEnable") Short auditEnable,
            @Param("operator") String operator,
            @Param("reason") String reason);
}