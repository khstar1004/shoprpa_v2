package com.iflytek.rpa.auth.sp.casdoor.dao;

import com.iflytek.rpa.auth.core.entity.GetMarketTenantUserListDto;
import com.iflytek.rpa.auth.core.entity.TenantUser;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.casbin.casdoor.entity.User;

/**
 * Casdoor 사용자데이터방문연결
 *
 * @author Auto Generated
 * @create 2025/12/11
 */
@Mapper
public interface CasdoorUserDao {

    /**
     * 근거이름조회사용자
     *
     * @param keyword 닫기 문자(이름)
     * @param owner 테넌트ID(organization name)
     * @param databaseName 데이터베이스이름
     * @return 사용자목록
     */
    List<User> searchUserByName(
            @Param("keyword") String keyword, @Param("owner") String owner, @Param("databaseName") String databaseName);

    /**
     * 근거휴대폰 번호조회사용자
     *
     * @param keyword 닫기 문자(휴대폰 번호)
     * @param owner 테넌트ID(organization name)
     * @param databaseName 데이터베이스이름
     * @return 사용자목록
     */
    List<User> searchUserByPhone(
            @Param("keyword") String keyword, @Param("owner") String owner, @Param("databaseName") String databaseName);

    /**
     * 근거이름또는휴대폰 번호조회사용자
     *
     * @param keyword 닫기 문자(이름또는휴대폰 번호)
     * @param owner 테넌트ID(organization name)
     * @param databaseName 데이터베이스이름
     * @return 사용자목록
     */
    List<User> searchUserByNameOrPhone(
            @Param("keyword") String keyword, @Param("owner") String owner, @Param("databaseName") String databaseName);

    /**
     * 근거사용자ID목록조회테넌트사용자목록
     *
     * @param dto 조회파일(패키지테넌트ID및사용자ID목록)
     * @param databaseName 데이터베이스이름
     * @return 테넌트사용자목록
     */
    List<TenantUser> getMarketTenantUserList(
            @Param("dto") GetMarketTenantUserListDto dto, @Param("databaseName") String databaseName);
}