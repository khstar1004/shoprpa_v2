package com.iflytek.rpa.auth.sp.uap.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iflytek.rpa.auth.core.entity.GetDeployedUserListDto;
import com.iflytek.rpa.auth.core.entity.RobotExecute;
import com.iflytek.rpa.auth.sp.uap.entity.SyncUserInfo;
import com.iflytek.sec.uap.client.core.dto.user.ListUserByRoleDto;
import com.iflytek.sec.uap.client.core.dto.user.UapUser;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserDao {
    List<UapUser> queryUapUserByIds(
            @Param("userIds") List<String> userIds,
            @Param("databaseName") String databaseName,
            @Param("tenantId") String tenantId);

    IPage<String> queryUserIdsByRole(
            IPage page,
            @Param("listUserByRoleDto") ListUserByRoleDto listUserByRoleDto,
            @Param("databaseName") String databaseName);

    List<UapUser> queryUapUserByName(
            @Param("userName") String userName,
            @Param("tenantId") String tenantId,
            @Param("databaseName") String databaseName);

    /**
     * 업데이트사용자의삼방법정보
     * @param loginName 로그인이름
     * @param thirdExtInfo 삼방법정보
     * @param databaseName 데이터베이스이름
     */
    void updateThirdExtInfo(
            @Param("loginName") String loginName,
            @Param("thirdExtInfo") String thirdExtInfo,
            @Param("databaseName") String databaseName);

    /**
     * 업데이트사용자의식별자필드 ext_info
     * @param phone 휴대폰 번호
     * @param extInfo 식별자
     * @param databaseName 데이터베이스이름
     */
    void updateExtInfo(
            @Param("phone") String phone, @Param("extInfo") String extInfo, @Param("databaseName") String databaseName);

    /**
     * 근거휴대폰 번호조회사용자의삼방법정보
     * @param phone 휴대폰 번호
     * @param databaseName 데이터베이스이름
     * @return 삼방법정보
     */
    String queryThirdExtInfoByPhone(@Param("phone") String phone, @Param("databaseName") String databaseName);

    /**
     * 근거휴대폰 번호조회사용자의식별자 ext_info
     * @param phone 휴대폰 번호
     * @param databaseName 데이터베이스이름
     * @return 식별자
     */
    String queryExtInfoByPhone(@Param("phone") String phone, @Param("databaseName") String databaseName);

    /**
     * 근거휴대폰 번호조회사용자의로그인이름
     * @param phone 휴대폰 번호
     * @param databaseName 데이터베이스이름
     * @return 로그인이름
     */
    String queryLoginNameByPhone(@Param("phone") String phone, @Param("databaseName") String databaseName);

    /**
     * 근거로그인이름조회휴대폰 번호
     * @param loginName 로그인이름
     * @param databaseName 데이터베이스이름
     * @return 휴대폰 번호
     */
    String queryPhoneByLoginName(@Param("loginName") String loginName, @Param("databaseName") String databaseName);

    /**
     * 조회필요까지ShopRPA 계정의사용자(휴대폰 번호아니요비어 있습니다third_ext_info비어 있습니다)
     * @param databaseName 데이터베이스이름
     * @param loginNames 가능선택, 지정필요의사용자로그인이름목록, 비어 있습니다이면조회모든기호합치기파일의사용자
     * @return 사용자목록
     */
    List<SyncUserInfo> queryUsersToSync(
            @Param("databaseName") String databaseName, @Param("loginNames") List<String> loginNames);

    /**
     * 근거휴대폰 번호조회사용자ID
     * @param phone 휴대폰 번호
     * @param databaseName 데이터베이스이름
     * @return 사용자ID
     */
    String getUserIdByPhone(@Param("phone") String phone, @Param("databaseName") String databaseName);

    /**
     * 량업데이트사용자유형
     * @param userIds 사용자ID목록
     * @param userType 사용자유형
     * @param databaseName 데이터베이스이름
     */
    void batchUpdateUserType(
            @Param("userIds") List<String> userIds,
            @Param("userType") Integer userType,
            @Param("databaseName") String databaseName);

    /**
     * 량업데이트사용자명명칭: 결과가name비어 있습니다, 이면업데이트로login_name
     * @param userIds 사용자ID목록
     * @param databaseName 데이터베이스이름
     */
    void batchUpdateNameFromLoginName(
            @Param("userIds") List<String> userIds, @Param("databaseName") String databaseName);

    String getNameById(String id, String databaseName);

    /**
     * 가져오기완료모듈사용자목록(분)
     * @param dto 조회파일
     * @param databaseName 데이터베이스이름
     * @return 완료모듈사용자분목록
     */
    IPage<RobotExecute> getDeployedUserList(
            IPage page, @Param("dto") GetDeployedUserListDto dto, @Param("databaseName") String databaseName);

    /**
     * 가져오기미완료모듈사용자목록
     * @param dto 조회파일
     * @param databaseName 데이터베이스이름
     * @return 미완료모듈사용자목록
     */
    List<com.iflytek.rpa.auth.core.entity.MarketDto> getUserUnDeployed(
            @Param("dto") com.iflytek.rpa.auth.core.entity.GetUserUnDeployedDto dto,
            @Param("databaseName") String databaseName);

    /**
     * 가져오기마켓사용자목록(분)
     * @param page 분객체
     * @param dto 조회파일
     * @param databaseName 데이터베이스이름
     * @return 마켓사용자분목록
     */
    IPage<com.iflytek.rpa.auth.core.entity.MarketDto> getMarketUserList(
            IPage page,
            @Param("dto") com.iflytek.rpa.auth.core.entity.GetMarketUserListDto dto,
            @Param("databaseName") String databaseName);

    /**
     * 가져오기 공유마켓사용자목록(분)
     * @param page 분객체
     * @param dto 조회파일
     * @param databaseName 데이터베이스이름
     * @return 공유마켓사용자분목록
     */
    IPage<com.iflytek.rpa.auth.core.entity.MarketDto> getMarketUserListByPublic(
            IPage page,
            @Param("dto") com.iflytek.rpa.auth.core.entity.GetMarketUserListByPublicDto dto,
            @Param("databaseName") String databaseName);

    /**
     * 근거휴대폰 번호조회마켓사용자(아니요에서마켓중의사용자)
     * @param dto 조회파일
     * @param databaseName 데이터베이스이름
     * @return 사용자목록
     */
    List<com.iflytek.rpa.auth.core.entity.MarketDto> getMarketUserByPhone(
            @Param("dto") com.iflytek.rpa.auth.core.entity.GetMarketUserByPhoneDto dto,
            @Param("databaseName") String databaseName);

    /**
     * 근거휴대폰 번호조회마켓중의사용자(사용마켓모든, 정렬제거)
     * @param dto 조회파일
     * @param databaseName 데이터베이스이름
     * @return 사용자목록
     */
    List<com.iflytek.rpa.auth.core.entity.MarketDto> getMarketUserByPhoneForOwner(
            @Param("dto") com.iflytek.rpa.auth.core.entity.GetMarketUserByPhoneForOwnerDto dto,
            @Param("databaseName") String databaseName);

    /**
     * 근거사용자ID목록조회테넌트사용자목록
     * @param dto 조회파일
     * @param databaseName 데이터베이스이름
     * @return 테넌트사용자목록
     */
    List<com.iflytek.rpa.auth.core.entity.TenantUser> getMarketTenantUserList(
            @Param("dto") com.iflytek.rpa.auth.core.entity.GetMarketTenantUserListDto dto,
            @Param("databaseName") String databaseName);

    UapUser getUserById(String id, String databaseName);

    IPage<RobotExecute> getDeployedUserListWithoutTenantId(
            Page<RobotExecute> page, GetDeployedUserListDto dto, String databaseName);
}