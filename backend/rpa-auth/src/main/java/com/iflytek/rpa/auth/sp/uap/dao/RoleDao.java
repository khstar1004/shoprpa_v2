package com.iflytek.rpa.auth.sp.uap.dao;

import com.iflytek.rpa.auth.core.entity.AppInfoBo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RoleDao {
    Integer getUnspecifiedRoleWithTenant(String databaseName, String tenantId);

    AppInfoBo selectAppInfo(String databaseName);

    Integer insertUnspecifiedRole(
            @Param("databaseName") String databaseName, @Param("appId") String appId, @Param("appName") String appName);

    void insertUnspecifiedTenantBind(@Param("databaseName") String databaseName, @Param("tenantId") String tenantId);

    Integer getUnspecifiedRole(@Param("databaseName") String databaseName);

    /**
     * 조회지정역할아래의사용자ID목록
     * @param databaseName 데이터베이스이름
     * @param roleId 역할ID
     * @param tenantId 테넌트ID
     * @return 사용자ID목록
     */
    List<String> getUserIdsByRoleId(
            @Param("databaseName") String databaseName,
            @Param("roleId") String roleId,
            @Param("tenantId") String tenantId);

    /**
     * 를지정사용자목록의역할까지"지정되지 않았습니다"역할
     * @param databaseName 데이터베이스이름
     * @param userIds 사용자ID목록
     * @param tenantId 테넌트ID
     */
    void migrateUsersToUnspecifiedRole(
            @Param("databaseName") String databaseName,
            @Param("userIds") List<String> userIds,
            @Param("tenantId") String tenantId);

    List<String> getBindUnspecifiedRoleIds(
            @Param("userIds") List<String> userIds,
            @Param("tenantId") String tenantId,
            @Param("databaseName") String databaseName);

    void batchDeleteUnspecifiedRoleBind(@Param("ids") List<String> ids, @Param("databaseName") String databaseName);

    /**
     * 근거역할이름조회역할ID
     * @param databaseName 데이터베이스이름
     * @param roleName 역할이름
     * @return 역할ID
     */
    String getRoleIdByName(@Param("databaseName") String databaseName, @Param("roleName") String roleName);

    /**
     * 조회테넌트역할닫기 여부저장에서
     * @param databaseName 데이터베이스이름
     * @param tenantId 테넌트ID
     * @param roleId 역할ID
     * @return 닫기 수
     */
    Integer checkTenantRoleExists(
            @Param("databaseName") String databaseName,
            @Param("tenantId") String tenantId,
            @Param("roleId") String roleId);

    /**
     * 삽입테넌트역할닫기 
     * @param databaseName 데이터베이스이름
     * @param tenantId 테넌트ID
     * @param roleId 역할ID
     */
    void insertTenantRole(
            @Param("databaseName") String databaseName,
            @Param("tenantId") String tenantId,
            @Param("roleId") String roleId);

    /**
     * 를사용자역할기록의테넌트ID업데이트로지정테넌트
     * @param databaseName 데이터베이스이름
     * @param userId 사용자ID
     * @param roleId 역할ID
     * @param tenantId 목록 테넌트ID
     * @return 행데이터
     */
    int updateUserRoleTenant(
            @Param("databaseName") String databaseName,
            @Param("userId") String userId,
            @Param("roleId") String roleId,
            @Param("tenantId") String tenantId);

    /**
     * 조회사용자역할닫기 여부저장에서(조회 tenant_id, user_id, role_id)
     * @param databaseName 데이터베이스이름
     * @param tenantId 테넌트ID
     * @param userId 사용자ID
     * @param roleId 역할ID
     * @return 닫기 수
     */
    Integer checkUserRoleExists(
            @Param("databaseName") String databaseName,
            @Param("tenantId") String tenantId,
            @Param("userId") String userId,
            @Param("roleId") String roleId);

    /**
     * 조회사용자역할닫기 여부저장에서(조회 user_id, role_id, 아니요 tenant_id)
     * @param databaseName 데이터베이스이름
     * @param userId 사용자ID
     * @param roleId 역할ID
     * @return 닫기 수
     */
    Integer checkUserRoleExistsByUserAndRole(
            @Param("databaseName") String databaseName, @Param("userId") String userId, @Param("roleId") String roleId);

    /**
     * 삽입사용자역할닫기 
     * @param databaseName 데이터베이스이름
     * @param tenantId 테넌트ID
     * @param userId 사용자ID
     * @param roleId 역할ID
     */
    void insertUserRole(
            @Param("databaseName") String databaseName,
            @Param("tenantId") String tenantId,
            @Param("userId") String userId,
            @Param("roleId") String roleId);
}