package com.iflytek.rpa.conf.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * UAP사용자데이터방문
 */
@Mapper
public interface UapUserDao {

    /**
     * 삽입사용자
     * @param databaseName 데이터베이스이름
     * @param userId 사용자ID
     * @param loginName 로그인이름(휴대폰 번호)
     * @param password 비밀번호
     * @param phone 휴대폰 번호
     * @return 행데이터
     */
    int insertUser(
            @Param("databaseName") String databaseName,
            @Param("userId") String userId,
            @Param("loginName") String loginName,
            @Param("password") String password,
            @Param("phone") String phone);

    /**
     * 삽입테넌트사용자닫기시스템
     * @param databaseName 데이터베이스이름
     * @param tenantUserId 테넌트사용자닫기시스템ID
     * @param tenantId 테넌트ID
     * @param userId 사용자ID
     * @return 행데이터
     */
    int insertTenantUser(
            @Param("databaseName") String databaseName,
            @Param("tenantUserId") String tenantUserId,
            @Param("tenantId") String tenantId,
            @Param("userId") String userId);

    int insertRoleUser(
            @Param("databaseName") String databaseName,
            @Param("roleUserId") String roleUserId,
            @Param("roleId") String roleId,
            @Param("tenantId") String tenantId,
            @Param("userId") String userId);

    /**
     * 근거로그인이름또는휴대폰 번호조회사용자여부저장에서
     * @param databaseName 데이터베이스이름
     * @param loginName 로그인이름(휴대폰 번호)
     * @param phone 휴대폰 번호
     * @return 사용자수
     */
    int countUserByLoginNameOrPhone(
            @Param("databaseName") String databaseName,
            @Param("loginName") String loginName,
            @Param("phone") String phone);

    /**
     * 근거매칭필드이름조회매칭값
     * @param databaseName 데이터베이스이름
     * @param fieldName 매칭필드이름
     * @return 매칭값
     */
    String getConfigValue(@Param("databaseName") String databaseName, @Param("fieldName") String fieldName);

    /**
     * 근거로그인이름또는휴대폰 번호조회사용자ID
     * @param databaseName 데이터베이스이름
     * @param loginName 로그인이름(휴대폰 번호)
     * @param phone 휴대폰 번호
     * @return 사용자ID
     */
    String getUserIdByLoginNameOrPhone(
            @Param("databaseName") String databaseName,
            @Param("loginName") String loginName,
            @Param("phone") String phone);
}