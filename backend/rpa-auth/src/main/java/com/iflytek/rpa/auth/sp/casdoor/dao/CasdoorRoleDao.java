package com.iflytek.rpa.auth.sp.casdoor.dao;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.casbin.casdoor.entity.Role;

/**
 * Casdoor 역할데이터방문연결
 *
 * @author Auto Generated
 * @create 2025/12/17
 */
@Mapper
public interface CasdoorRoleDao {

    /**
     * 근거이름조회역할
     *
     * @param keyword      닫기 문자(역할이름)
     * @param owner        테넌트ID(organization name)
     * @param databaseName 데이터베이스이름
     * @return 역할목록(Casdoor Role )
     */
    List<Role> searchRoleByName(
            @Param("keyword") String keyword, @Param("owner") String owner, @Param("databaseName") String databaseName);
}