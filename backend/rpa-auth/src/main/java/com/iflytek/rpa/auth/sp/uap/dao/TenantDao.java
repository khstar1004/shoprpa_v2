package com.iflytek.rpa.auth.sp.uap.dao;

import com.iflytek.rpa.auth.core.entity.UserVo;
import com.iflytek.sec.uap.client.core.dto.tenant.UapTenant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 테넌트닫기데이터방문
 */
@Mapper
public interface TenantDao {

    List<UserVo> getUserByTenantId(
            @Param("databaseName") String databaseName,
            @Param("tenantId") String tenantId,
            @Param("userName") String userName);

    List<String> getAllTenantId(@Param("databaseName") String databaseName);

    /**
     * 근거휴대폰 번호조회사용자의테넌트목록
     *
     * @param databaseName 데이터베이스이름
     * @param phone 휴대폰 번호
     * @return 테넌트목록
     */
    List<UapTenant> queryTenantListByPhone(@Param("databaseName") String databaseName, @Param("phone") String phone);

    String getTenantUserId(
            @Param("databaseName") String databaseName,
            @Param("userId") String userId,
            @Param("tenantId") String tenantId);

    Integer getTenantUserStatus(
            @Param("databaseName") String databaseName,
            @Param("userId") String userId,
            @Param("tenantId") String tenantId);

    Integer enableTenantUser(
            @Param("databaseName") String databaseName, @Param("id") String id, @Param("status") Integer status);

    Integer updateLoginTime(String databaseName, String id);

    List<String> getTenantUserIdsByType(
            @Param("databaseName") String databaseName,
            @Param("tenantId") String tenantId,
            @Param("tenantUserType") Integer tenantUserType);

    List<String> getNoClassifyTenantIds(@Param("databaseName") String databaseName);

    Integer updateTenantClassifyCompleted(
            @Param("databaseName") String databaseName, @Param("tenantIds") List<String> tenantIds);

    /**
     * 가져오기모든테넌트ID목록(테넌트코드으로ep_또는es_열기 )
     * @param databaseName 데이터베이스이름
     * @return 테넌트ID목록
     */
    List<String> getAllEnterpriseTenantId(@Param("databaseName") String databaseName);

    /**
     * 가져오기테넌트사용자유형(1테이블테넌트관리관리원, 테이블통신사용자)
     * @param databaseName 데이터베이스이름
     * @param userId 사용자ID
     * @param tenantId 테넌트ID
     * @return 테넌트사용자유형(가능로null)
     */
    Integer getTenantUserType(
            @Param("databaseName") String databaseName,
            @Param("userId") String userId,
            @Param("tenantId") String tenantId);

    /**
     * 삭제지정테넌트아래의사용자닫기 
     * @param databaseName 데이터베이스이름
     * @param tenantId 테넌트ID
     * @param userId 사용자ID
     * @return 행데이터
     */
    Integer deleteTenantUser(
            @Param("databaseName") String databaseName,
            @Param("tenantId") String tenantId,
            @Param("userId") String userId);

    /**
     * 조회시패키지 tenant_id 및 creator_id 필드의테이블이름(정렬제거 t_uap 열기 의테이블)
     * @param databaseName 데이터베이스이름
     * @return 테이블이름목록
     */
    List<String> getTablesWithTenantId(@Param("databaseName") String databaseName);

    /**
     * 업데이트지정테이블의 tenant_id
     * @param databaseName 데이터베이스이름
     * @param tableName 테이블이름
     * @param oldTenantId 테넌트ID
     * @param newTenantId 새테넌트ID
     * @param userId 사용자ID(creator_id)
     * @return 행데이터
     */
    Integer updateTableTenantId(
            @Param("databaseName") String databaseName,
            @Param("tableName") String tableName,
            @Param("oldTenantId") String oldTenantId,
            @Param("newTenantId") String newTenantId,
            @Param("userId") String userId);

    List<String> getAllTenantIdWithoutClassify(String databaseName);

    Integer updateTenantClassifyFlag(String databaseName, List<String> tenantIds);

    List<String> getManagerUserIds(String databaseName, String tenantId);

    List<String> getNormalUserIds(String databaseName, String tenantId);

    /**
     * 조회기호합치기파일의테넌트사용자(tenant_id아니요에서지정목록중)
     * @param databaseName 데이터베이스이름
     * @param excludeTenantIds 정렬제거의테넌트ID목록
     * @return 테넌트사용자목록(패키지userId및tenantId)
     */
    List<com.iflytek.rpa.auth.core.entity.TenantUser> queryTenantUsersForSync(
            @Param("databaseName") String databaseName, @Param("excludeTenantIds") List<String> excludeTenantIds);

    /**
     * 조회 robot_execute_record 테이블중기호합치기파일의기록ID
     * @param databaseName 데이터베이스이름
     * @param oldTenantId 테넌트ID
     * @param userId 사용자ID(creator_id)
     * @return 기록ID목록
     */
    List<Long> queryRobotExecuteRecordIds(
            @Param("databaseName") String databaseName,
            @Param("oldTenantId") String oldTenantId,
            @Param("userId") String userId);

    /**
     * 근거ID목록 량업데이트 robot_execute_record 테이블의 tenant_id
     * @param databaseName 데이터베이스이름
     * @param newTenantId 새테넌트ID
     * @param ids 기록ID목록
     * @return 행데이터
     */
    Integer updateRobotExecuteRecordTenantIdByIds(
            @Param("databaseName") String databaseName,
            @Param("newTenantId") String newTenantId,
            @Param("ids") List<Long> ids);
}