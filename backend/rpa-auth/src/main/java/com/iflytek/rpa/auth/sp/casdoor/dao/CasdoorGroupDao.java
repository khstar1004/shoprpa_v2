package com.iflytek.rpa.auth.sp.casdoor.dao;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.casbin.casdoor.entity.Group;

/**
 * Casdoor 그룹(모듈)데이터방문연결
 *
 * @author Auto Generated
 * @create 2025/12/11
 */
@Mapper
public interface CasdoorGroupDao {

    /**
     * 근거이름조회그룹(모듈)
     *
     * @param keyword 닫기 문자(모듈이름)
     * @param owner 테넌트ID(organization name)
     * @param databaseName 데이터베이스이름
     * @return 그룹목록
     */
    List<Group> searchDeptByName(
            @Param("keyword") String keyword, @Param("owner") String owner, @Param("databaseName") String databaseName);
}