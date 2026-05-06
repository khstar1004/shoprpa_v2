package com.iflytek.rpa.auth.core.service;

import com.iflytek.rpa.auth.core.entity.*;
import com.iflytek.rpa.auth.utils.AppResponse;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 사용자서비스
 */
public interface UserService {

    /**
     * 회원가입
     * @param registerDto 회원가입정보
     * @param request HTTP요청 
     * @return 테넌트ID
     */
    AppResponse<String> register(RegisterDto registerDto, HttpServletRequest request) throws IOException;

    /**
     * 이름검색모든요소또는모듈
     * @param name 이름
     * @param request HTTP요청 
     * @return 모듈또는사용자 정보
     */
    AppResponse<GetDeptOrUserDto> searchDeptOrUser(String name, HttpServletRequest request);

    /**
     * 요소
     * @param updateUapUserDto 업데이트사용자 정보
     * @param request HTTP요청 
     * @return 결과
     */
    AppResponse<String> editUser(UpdateUapUserDto updateUapUserDto, HttpServletRequest request) throws IOException;

    /**
     * 추가요소
     * @param createUapUserDto 생성사용자 정보
     * @param request HTTP요청 
     * @return 결과
     */
    AppResponse<String> addUser(CreateUapUserDto createUapUserDto, HttpServletRequest request) throws IOException;

    /**
     * 분조회현재기기의사용자
     * @param listUserDto 조회파일
     * @param request HTTP요청 
     * @return 분사용자목록
     */
    AppResponse<PageDto<DeptUserDto>> queryUserListByOrgId(ListUserDto listUserDto, HttpServletRequest request)
            throws IOException;

    /**
     * 역할관리관리-근거모듈id조회모듈아래의사람원및모듈
     * @param id 모듈ID
     * @param request HTTP요청 
     * @return 모듈사용자목록
     */
    AppResponse<List<CurrentDeptUserDto>> queryUserAndDept(String id, HttpServletRequest request);

    /**
     * 역할관리관리-근거이름문자또는휴대폰 번호조회요소
     * @param keyWord 닫기 문자
     * @param request HTTP요청 
     * @return 사용자목록
     */
    AppResponse<List<CurrentDeptUserDto>> searchUserWithStatus(String keyWord, HttpServletRequest request)
            throws IOException;

    /**
     * 역할관리관리-추가구성원
     * @param bindUserListDto 지정사용자목록DTO
     * @param request HTTP요청 
     * @return 결과
     */
    AppResponse<String> bindUserListRole(BindUserListDto bindUserListDto, HttpServletRequest request)
            throws IOException;

    /**
     * 사람원해제역할
     * @param bindRoleDto 지정역할DTO
     * @param request HTTP요청 
     * @return 결과
     */
    AppResponse<String> unbindRole(BindRoleDto bindRoleDto, HttpServletRequest request) throws IOException;

    /**
     * 분가져오기역할지정의사용자목록, 가능근거로그인이름또는이름조회
     * @param listUserByRoleDto 조회파일
     * @param request HTTP요청 
     * @return 분사용자목록
     */
    AppResponse<PageDto<User>> queryBindListByRole(ListUserByRoleDto listUserByRoleDto, HttpServletRequest request)
            throws IOException;

    /**
     * 가져오기현재로그인사용자
     * @param request HTTP요청 
     * @return 현재로그인사용자 정보
     */
    AppResponse<User> getCurrentLoginUser(HttpServletRequest request);

    /**
     * 가져오기현재로그인사용자ID
     * @param request HTTP요청 
     * @return 현재로그인사용자ID
     */
    AppResponse<String> getCurrentUserId(HttpServletRequest request);

    /**
     * 가져오기현재로그인사용자명
     * @param request HTTP요청 
     * @return 현재로그인사용자명
     */
    AppResponse<String> getCurrentLoginUsername(HttpServletRequest request);

    /**
     * 근거사용자ID조회로그인이름
     * @param id 사용자ID
     * @param request HTTP요청 
     * @return 로그인이름
     */
    AppResponse<String> getLoginNameById(String id, HttpServletRequest request);

    /**
     * 근거사용자ID조회이름
     * @param id 사용자ID
     * @param request HTTP요청 
     * @return 사용자이름
     */
    AppResponse<String> getRealNameById(String id, HttpServletRequest request);

    /**
     * 근거사용자ID조회사용자 정보
     * @param id 사용자ID
     * @param request HTTP요청 
     * @return 사용자 정보
     */
    AppResponse<User> getUserInfoById(String id, HttpServletRequest request);

    /**
     * 근거휴대폰 번호조회사용자이름
     * @param phone 휴대폰 번호
     * @param request HTTP요청 
     * @return 사용자이름
     */
    AppResponse<String> getRealNameByPhone(String phone, HttpServletRequest request);

    /**
     * 근거휴대폰 번호조회로그인이름
     * @param phone 휴대폰 번호
     * @param request HTTP요청 
     * @return 로그인이름
     */
    AppResponse<String> getLoginNameByPhone(String phone, HttpServletRequest request);

    /**
     * 여부사용자(ext_info = 1 테이블사용자)
     * @param phone 휴대폰 번호
     * @return 여부사용자
     */
    AppResponse<Boolean> isHistoryUser(String phone);

    /**
     * 근거휴대폰 번호조회사용자 정보
     * @param phone 휴대폰 번호
     * @param request HTTP요청 
     * @return 사용자 정보
     */
    AppResponse<User> getUserInfoByPhone(String phone, HttpServletRequest request);

    /**
     * 근거사용자ID목록조회사용자 정보목록(다중지원100개id)
     * @param userIdList 사용자ID목록
     * @param request HTTP요청 
     * @return 사용자 정보목록
     */
    AppResponse<List<User>> queryUserListByIds(List<String> userIdList, HttpServletRequest request);

    /**
     * 근거이름조회사람원
     * @param keyword 닫기 문자
     * @param deptId 모듈ID(가능선택)
     * @param request HTTP요청 
     * @return 사용자 정보목록
     */
    AppResponse<List<User>> searchUserByName(String keyword, String deptId, HttpServletRequest request);

    /**
     * 근거휴대폰 번호조회사람원
     * @param keyword 닫기 문자
     * @param deptId 모듈ID(가능선택)
     * @param request HTTP요청 
     * @return 사용자 정보목록
     */
    AppResponse<List<User>> searchUserByPhone(String keyword, String deptId, HttpServletRequest request);

    /**
     * 근거이름또는휴대폰 번호조회사람원
     * @param keyword 닫기 문자
     * @param deptId 모듈ID(가능선택)
     * @param request HTTP요청 
     * @return 사용자 정보목록
     */
    AppResponse<List<User>> searchUserByNameOrPhone(String keyword, String deptId, HttpServletRequest request);

    /**
     * 조회현재로그인의사용자 정보
     * @param request HTTP요청 
     * @return 사용자 정보
     */
    AppResponse<User> getUserInfo(HttpServletRequest request);

    /**
     * 출력로그인
     * @param request HTTP요청 
     * @param response HTTP
     * @return 결과
     */
    AppResponse<String> logout(HttpServletRequest request, HttpServletResponse response) throws IOException;

    /**
     * 변수변경모듈
     * @param userChangeDeptDto 변수변경모듈DTO
     * @param request HTTP요청 
     * @return 결과
     */
    AppResponse<String> changeDept(UserChangeDeptDto userChangeDeptDto, HttpServletRequest request);

    /**
     * 삭제요소
     * @param userDeleteDto 삭제요소DTO
     * @param request HTTP요청 
     * @return 결과
     */
    AppResponse<String> deleteUser(UserDeleteDto userDeleteDto, HttpServletRequest request) throws IOException;

    /**
     * 사용/사용 안 함요소
     * @param userEnableDto 사용/사용 안 함DTO
     * @param request HTTP요청 
     * @return 결과
     */
    AppResponse<String> enableUser(UserEnableDto userEnableDto, HttpServletRequest request);

    /**
     * 조회현재기기의전체사용자(모듈추가, 모듈사람드롭다운)
     * @param orgId 모듈ID
     * @param request HTTP요청 
     * @return 사용자목록
     */
    AppResponse<List<User>> queryUserDetailListByOrgId(String orgId, HttpServletRequest request) throws IOException;

    /**
     * 이름또는휴대폰 번호검색모든요소(사용봇모든아래)
     * @param keyword 닫기 문자(이름또는휴대폰 번호)
     * @param deptId 모듈ID
     * @return 사용자검색결과목록
     */
    AppResponse<List<UserSearchDto>> getUserByNameOrPhone(String keyword, String deptId, HttpServletRequest request);

    /**
     * 가져오기사용자정보(패키지정보대기)
     * @param tenantId 테넌트ID
     * @param getUserDto 조회매개변수
     * @param request HTTP요청 
     * @return 사용자정보
     */
    AppResponse<UserExtendDto> queryUserExtendInfo(String tenantId, GetUserDto getUserDto, HttpServletRequest request)
            throws IOException;

    /**
     * 가져오기현재사용자권한목록
     * @param request HTTP요청 
     * @return 사용자목록
     */
    AppResponse<List<Permission>> getCurrentUserPermissionList(HttpServletRequest request) throws IOException;

    /**
     * 가져오기Casdoor로그인재지정URL(Casdoor사용)
     * @param request HTTP요청 
     * @return 로그인재지정URL
     */
    AppResponse<String> getRedirectUrl(HttpServletRequest request);

    /**
     * Casdoor OAuth로그인(Casdoor사용)
     * @param code OAuth권한 부여코드
     * @param state OAuth state매개변수
     * @param request HTTP요청 
     * @return 사용자 정보
     */
    AppResponse<User> signIn(String code, String state, HttpServletRequest request) throws IOException;

    /**
     * 조회사용자로그인상태(Casdoor사용)
     * @param request HTTP요청 
     * @return 사용자 정보, 결과가로그인되지 않았습니다반환오류
     */
    AppResponse<User> checkLoginStatus(HttpServletRequest request);

    /**
     * 새로고침서버token(Casdoor사용, accessToken경과시사용)
     * @param request HTTP요청 
     * @return 결과
     */
    AppResponse<String> refreshToken(HttpServletRequest request);

    /**
     * 가져오기현재로그인사용자의권한
     * 근거session조회테넌트code, 결과가예테넌트, 이면조회데이터베이스중권한
     * 결과가있음데이터, 있음모든권한
     *
     * @param request HTTP요청 
     * @return 사용자권한정보
     */
    AppResponse<UserEntitlementDto> getCurrentUserEntitlement(HttpServletRequest request);

    AppResponse<String> getNameById(String id, HttpServletRequest request);

    /**
     * 가져오기완료모듈사용자목록(분)
     * @param dto 조회파일
     * @param request HTTP요청 
     * @return 완료모듈사용자분목록
     */
    AppResponse<PageDto<RobotExecute>> getDeployedUserList(GetDeployedUserListDto dto, HttpServletRequest request);

    /**
     * 가져오기미완료모듈사용자목록
     * @param dto 조회파일
     * @param request HTTP요청 
     * @return 미완료모듈사용자목록
     */
    AppResponse<List<MarketDto>> getUserUnDeployed(GetUserUnDeployedDto dto, HttpServletRequest request);

    /**
     * 가져오기마켓사용자목록(분)
     * @param dto 조회파일
     * @param request HTTP요청 
     * @return 마켓사용자분목록
     */
    AppResponse<PageDto<MarketDto>> getMarketUserList(GetMarketUserListDto dto, HttpServletRequest request);

    /**
     * 가져오기 공유마켓사용자목록(분)
     * @param dto 조회파일
     * @param request HTTP요청 
     * @return 공유마켓사용자분목록
     */
    AppResponse<PageDto<MarketDto>> getMarketUserListByPublic(
            GetMarketUserListByPublicDto dto, HttpServletRequest request);

    /**
     * 근거휴대폰 번호조회마켓사용자(아니요에서마켓중의사용자)
     * @param dto 조회파일
     * @param request HTTP요청 
     * @return 사용자목록
     */
    AppResponse<List<MarketDto>> getMarketUserByPhone(GetMarketUserByPhoneDto dto, HttpServletRequest request);

    /**
     * 근거휴대폰 번호조회마켓중의사용자(사용마켓모든, 정렬제거)
     * @param dto 조회파일
     * @param request HTTP요청 
     * @return 사용자목록
     */
    AppResponse<List<MarketDto>> getMarketUserByPhoneForOwner(
            GetMarketUserByPhoneForOwnerDto dto, HttpServletRequest request);

    /**
     * 근거사용자ID목록조회테넌트사용자목록
     * @param dto 조회파일
     * @param request HTTP요청 
     * @return 테넌트사용자목록
     */
    AppResponse<List<TenantUser>> getMarketTenantUserList(GetMarketTenantUserListDto dto, HttpServletRequest request);

    AppResponse<PageDto<RobotExecute>> getDeployedUserListWithoutTenantId(
            GetDeployedUserListDto dto, HttpServletRequest request);
}