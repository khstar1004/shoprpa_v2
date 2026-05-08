package com.iflytek.rpa.common.feign;

import com.iflytek.rpa.common.feign.entity.*;
import com.iflytek.rpa.common.feign.entity.dto.*;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "rpa-auth",
        url = "${auth.base-url:http://localhost:10251}",
        configuration = FeignAutoConfiguration.class)
public interface RpaAuthFeign {

    // ==================== UserController 사용자닫기연결 ====================

    /**
     * 가져오기현재로그인사용자 정보
     * @return 사용자 정보
     */
    @GetMapping("/api/rpa-auth/user/info")
    AppResponse<User> getLoginUser();

    /**
     * 가져오기사용자정보
     * @param tenantId
     * @param dto
     * @return
     */
    @PostMapping("/api/rpa-auth/user/getUserExtendInfo")
    AppResponse<UserExtendDto> getUserExtendInfo(
            @RequestParam("tenantId") String tenantId, @RequestBody GetUserDto dto);

    /**
     * 회원가입(Uap의회원가입연결, 내용준비사용)
     * @param registerDto 회원가입DTO
     * @return 회원가입결과
     */
    @PostMapping("/api/rpa-auth/user/register")
    AppResponse<String> registerUap(@RequestBody Object registerDto);

    /**
     * 추가요소
     * @param createUapUserDto 생성사용자DTO
     * @return 결과
     */
    @PostMapping("/api/rpa-auth/user/add")
    AppResponse<String> addUser(@RequestBody Object createUapUserDto);

    /**
     * 요소
     * @param updateUapUserDto 업데이트사용자DTO
     * @return 결과
     */
    @PostMapping("/api/rpa-auth/user/edit")
    AppResponse<String> editUser(@RequestBody Object updateUapUserDto);

    /**
     * 삭제요소
     * @param userDto 사용자삭제DTO
     * @return 결과
     */
    @PostMapping("/api/rpa-auth/user/delete")
    AppResponse<String> deleteUser(@RequestBody Object userDto);

    /**
     * 사용/사용 안 함요소
     * @param userDto 사용자사용DTO
     * @return 결과
     */
    @PostMapping("/api/rpa-auth/user/enable")
    AppResponse<String> enableUser(@RequestBody Object userDto);

    /**
     * 변수변경모듈
     * @param userDto 사용자변수변경모듈DTO
     * @return 결과
     */
    @PostMapping("/api/rpa-auth/user/changeDept")
    AppResponse<String> changeDept(@RequestBody Object userDto);

    /**
     * 조회현재기기의전체사용자(모듈추가, 모듈사람드롭다운)
     * @param orgId 모듈ID
     * @return 사용자목록
     */
    @GetMapping("/api/rpa-auth/user/queryAllListByOrgId")
    AppResponse<List<User>> queryUserDetailListByOrgId(@RequestParam("orgId") String orgId);

    /**
     * 분조회현재기기의사용자
     * @param listUserDto 조회사용자DTO
     * @return 사용자분목록
     */
    @PostMapping("/api/rpa-auth/user/queryListByOrgId")
    AppResponse<PageDto<DeptUserDto>> queryUserListByOrgId(@RequestBody Object listUserDto);

    /**
     * 분가져오기역할지정의사용자목록, 가능근거로그인이름또는이름조회
     * @param listUserByRoleDto 조회사용자DTO
     * @return 사용자분목록
     */
    @PostMapping("/api/rpa-auth/user/queryBindListByRole")
    AppResponse<PageDto<User>> queryBindListByRole(@RequestBody Object listUserByRoleDto);

    /**
     * 사람원해제역할
     * @param bindRoleDto 지정역할DTO
     * @return 결과
     */
    @PostMapping("/api/rpa-auth/user/unbindRole")
    AppResponse<String> unbindRole(@RequestBody Object bindRoleDto);

    /**
     * 이름검색모든요소또는모듈
     * @param name 이름
     * @return 검색결과
     */
    @GetMapping("/api/rpa-auth/user/searchDeptOrUser")
    AppResponse<GetDeptOrUserDto> searchDeptOrUser(@RequestParam("name") String name);

    /**
     * 역할관리관리-근거모듈id조회모듈아래의사람원및모듈
     * @param id 모듈ID
     * @return 모듈및사람원목록
     */
    @GetMapping("/api/rpa-auth/user/queryUserAndDept")
    AppResponse<List<CurrentDeptUserDto>> queryUserAndDept(@RequestParam("id") String id);

    /**
     * 역할관리관리-근거이름문자또는휴대폰 번호조회요소
     * @param keyWord 닫기 문자
     * @return 사용자목록
     */
    @GetMapping("/api/rpa-auth/user/searchUserWithStatus")
    AppResponse<List<CurrentDeptUserDto>> searchUserWithStatus(@RequestParam("keyWord") String keyWord);

    /**
     * 역할관리관리-추가구성원
     * @param bindUserListDto 지정사용자목록DTO
     * @return 결과
     */
    @PostMapping("/api/rpa-auth/user/batchBindRole")
    AppResponse<String> bindUserListRole(@RequestBody Object bindUserListDto);

    /**
     * 중-봇-모든아래선택-조회연결
     * 근거입력의닫기 문자(이름또는휴대폰 번호)조회사용자
     * @param keyword 닫기 문자
     * @param deptId 모듈ID
     * @return 사용자목록
     */
    @PostMapping("/api/rpa-auth/user/getUserByNameOrPhone")
    AppResponse<List<User>> getUserByNameOrPhone(
            @RequestParam("keyword") String keyword, @RequestParam(value = "deptId", required = false) String deptId);

    /**
     * 가져오기현재로그인사용자
     * @return 현재로그인사용자 정보
     */
    @GetMapping("/api/rpa-auth/user/current")
    AppResponse<User> getCurrentLoginUser();

    /**
     * 가져오기현재로그인사용자ID
     * @return 현재로그인사용자ID
     */
    @GetMapping("/api/rpa-auth/user/current/id")
    AppResponse<String> getCurrentUserId();

    /**
     * 가져오기현재로그인사용자명
     * @return 현재로그인사용자명
     */
    @GetMapping("/api/rpa-auth/user/current/username")
    AppResponse<String> getCurrentLoginUsername();

    /**
     * 근거사용자ID조회로그인이름
     * @param id 사용자ID
     * @return 로그인이름
     */
    @GetMapping("/api/rpa-auth/user/loginName")
    AppResponse<String> getLoginNameById(@RequestParam("id") String id);

    /**
     * 근거사용자ID조회이름
     * @param id 사용자ID
     * @return 사용자이름
     */
    @GetMapping("/api/rpa-auth/user/realName")
    AppResponse<String> getRealNameById(@RequestParam("id") String id);

    /**
     * 근거ID가져오기사용자이름(필요하지 않습니다테넌트 정보)
     * @param id
     * @return
     */
    @GetMapping("/api/rpa-auth/user/getNameById")
    AppResponse<String> getNameById(@RequestParam("id") String id);

    /**
     * 가져오기완료모듈사용자목록(분)
     * @param dto 조회파일
     * @return 완료모듈사용자분목록
     */
    @PostMapping("/api/rpa-auth/user/getDeployedUserList")
    AppResponse<PageDto<RobotExecute>> getDeployedUserList(@RequestBody GetDeployedUserListDto dto);

    @PostMapping("/api/rpa-auth/user/getDeployedUserListWithoutTenantId")
    AppResponse<PageDto<RobotExecute>> getDeployedUserListWithoutTenantId(@RequestBody GetDeployedUserListDto dto);

    /**
     * 가져오기미완료모듈사용자목록
     * @param dto 조회파일
     * @return 미완료모듈사용자목록
     */
    @PostMapping("/api/rpa-auth/user/getUserUnDeployed")
    AppResponse<List<MarketDto>> getUserUnDeployed(@RequestBody GetUserUnDeployedDto dto);

    /**
     * 가져오기마켓사용자목록(분)
     * @param dto 조회파일
     * @return 마켓사용자분목록
     */
    @PostMapping("/api/rpa-auth/user/getMarketUserList")
    AppResponse<PageDto<MarketDto>> getMarketUserList(@RequestBody GetMarketUserListDto dto);

    /**
     * 가져오기 공유마켓사용자목록(분)
     * @param dto 조회파일
     * @return 공유마켓사용자분목록
     */
    @PostMapping("/api/rpa-auth/user/getMarketUserListByPublic")
    AppResponse<PageDto<MarketDto>> getMarketUserListByPublic(@RequestBody GetMarketUserListByPublicDto dto);

    /**
     * 근거휴대폰 번호조회마켓사용자(아니요에서마켓중의사용자)
     * @param dto 조회파일
     * @return 사용자목록
     */
    @PostMapping("/api/rpa-auth/user/getMarketUserByPhone")
    AppResponse<List<MarketDto>> getMarketUserByPhone(@RequestBody GetMarketUserByPhoneDto dto);

    /**
     * 근거휴대폰 번호조회마켓중의사용자(사용마켓모든, 정렬제거)
     * @param dto 조회파일
     * @return 사용자목록
     */
    @PostMapping("/api/rpa-auth/user/getMarketUserByPhoneForOwner")
    AppResponse<List<MarketDto>> getMarketUserByPhoneForOwner(@RequestBody GetMarketUserByPhoneForOwnerDto dto);

    /**
     * 근거사용자ID목록조회테넌트사용자목록
     * @param dto 조회파일
     * @return 테넌트사용자목록
     */
    @PostMapping("/api/rpa-auth/user/getMarketTenantUserList")
    AppResponse<List<TenantUser>> getMarketTenantUserList(@RequestBody GetMarketTenantUserListDto dto);

    /**
     * 근거사용자ID조회사용자 정보
     * @param id 사용자ID
     * @return 사용자 정보
     */
    @GetMapping("/api/rpa-auth/user/infoById")
    AppResponse<User> getUserInfoById(@RequestParam("id") String id);

    /**
     * 근거휴대폰 번호조회사용자이름
     * @param phone 휴대폰 번호
     * @return 사용자이름
     */
    @GetMapping("/api/rpa-auth/user/phone/realName")
    AppResponse<String> getRealNameByPhone(@RequestParam("phone") String phone);

    /**
     * 근거휴대폰 번호조회로그인이름
     * @param phone 휴대폰 번호
     * @return 로그인이름
     */
    @GetMapping("/api/rpa-auth/user/phone/loginName")
    AppResponse<String> getLoginNameByPhone(@RequestParam("phone") String phone);

    /**
     * 근거휴대폰 번호조회사용자 정보
     * @param phone 휴대폰 번호
     * @return 사용자 정보
     */
    @GetMapping("/api/rpa-auth/user/phone/info")
    AppResponse<User> getUserInfoByPhone(@RequestParam("phone") String phone);

    /**
     * 근거사용자ID목록조회사용자 정보목록(다중지원100개id)
     * @param userIdList 사용자ID목록
     * @return 사용자 정보목록
     */
    @PostMapping("/api/rpa-auth/user/queryByIds")
    AppResponse<List<User>> queryUserListByIds(@RequestBody List<String> userIdList);

    /**
     * 근거이름조회사람원
     * @param keyword 닫기 문자
     * @param deptId 모듈ID(가능선택)
     * @return 사용자 정보목록
     */
    @GetMapping("/api/rpa-auth/user/search/name")
    AppResponse<List<User>> searchUserByName(
            @RequestParam("keyword") String keyword, @RequestParam(value = "deptId", required = false) String deptId);

    /**
     * 근거휴대폰 번호조회사람원
     * @param keyword 닫기 문자
     * @param deptId 모듈ID(가능선택)
     * @return 사용자 정보목록
     */
    @GetMapping("/api/rpa-auth/user/search/phone")
    AppResponse<List<User>> searchUserByPhone(
            @RequestParam("keyword") String keyword, @RequestParam(value = "deptId", required = false) String deptId);

    /**
     * 근거이름또는휴대폰 번호조회사람원
     * @param keyword 닫기 문자
     * @param deptId 모듈ID(가능선택)
     * @return 사용자 정보목록
     */
    @GetMapping("/api/rpa-auth/user/search")
    AppResponse<List<User>> searchUserByNameOrPhone(
            @RequestParam("keyword") String keyword, @RequestParam(value = "deptId", required = false) String deptId);

    // ==================== TenantController 테넌트닫기연결 ====================
    /**
     * 가져오기테넌트ID
     * @return
     */
    @GetMapping("/api/rpa-auth/tenant/getTenantId")
    AppResponse<String> getTenantId();

    /**
     * 근거테넌트id가져오기모든조직목록
     * @param tenantId
     * @return
     */
    @GetMapping("/api/rpa-auth/tenant/getAllOrgList")
    AppResponse<List<Org>> queryAllOrgList(@RequestParam("tenantId") String tenantId);

    /**
     * 현재로그인사용자에서사용의테넌트목록
     * @return 테넌트목록
     */
    @GetMapping("/api/rpa-auth/tenant/getTenantListInApp")
    AppResponse<List<Tenant>> getTenantListInApp();

    /**
     * 정보조회
     * @return 정보
     */
    @GetMapping("/api/rpa-auth/tenant/getTenantInfo")
    AppResponse<TenantInfoDto> getTenantInfo();

    /**
     * 변경수정관리관리원(지원하지 않음)
     * @param id 관리관리원ID
     * @return 결과
     */
    @GetMapping("/api/rpa-auth/tenant/changeManager")
    AppResponse<String> changeManager(@RequestParam("id") String id);

    /**
     * 가져오기모든사용자
     * @param userName 사용자명
     * @return 사용자목록
     */
    @PostMapping("/api/rpa-auth/tenant/all-user")
    AppResponse<List<UserVo>> getAllUser(@RequestParam("userName") String userName);

    /**
     * 가져오기현재로그인의테넌트ID
     * @return 현재로그인의테넌트ID
     */
    @GetMapping("/api/rpa-auth/tenant/current/id")
    AppResponse<String> getCurrentTenantId();

    /**
     * 가져오기현재로그인의테넌트이름
     * @return 현재로그인의테넌트이름
     */
    @GetMapping("/api/rpa-auth/tenant/current/name")
    AppResponse<String> getCurrentTenantName();

    /**
     * 근거테넌트ID조회테넌트 정보
     * @param tenantId 테넌트ID
     * @return 테넌트 정보
     */
    @GetMapping("/api/rpa-auth/tenant/info")
    AppResponse<Tenant> queryTenantInfoById(@RequestParam("tenantId") String tenantId);

    /**
     * 테넌트
     * @param tenantId 테넌트id
     * @return 결과
     */
    @PostMapping("/api/rpa-auth/tenant/switch")
    AppResponse<String> switchTenant(@RequestParam("tenantId") String tenantId);

    /**
     * 가져오기지원하지 않는유형의테넌트ID목록
     * @return 지원하지 않는유형의테넌트ID목록
     */
    @GetMapping("/api/rpa-auth/tenant/getNoClassifyTenantIds")
    AppResponse<List<String>> getNoClassifyTenantIds();

    /**
     * 업데이트테넌트분유형완료로그
     * @param tenantIds 테넌트ID목록
     * @return 업데이트의기록데이터
     */
    @PostMapping("/api/rpa-auth/tenant/updateTenantClassifyCompleted")
    AppResponse<Integer> updateTenantClassifyCompleted(@RequestBody List<String> tenantIds);

    /**
     * 가져오기모든테넌트ID목록(테넌트코드으로ep_또는es_열기 )
     * @return 테넌트ID목록
     */
    @GetMapping("/api/rpa-auth/tenant/getAllEnterpriseTenantId")
    AppResponse<List<String>> getAllEnterpriseTenantId();

    /**
     * 가져오기모든테넌트ID목록(정렬제거default-tenant)
     * @return 테넌트ID목록
     */
    @GetMapping("/api/rpa-auth/tenant/getAllTenantId")
    AppResponse<List<String>> getAllTenantId();

    /**
     * 가져오기테넌트관리관리원ID목록
     * @param tenantId 테넌트ID
     * @return 테넌트관리관리원ID목록
     */
    @GetMapping("/api/rpa-auth/tenant/getTenantManagerIds")
    AppResponse<List<String>> getTenantManagerIds(@RequestParam("tenantId") String tenantId);

    /**
     * 가져오기테넌트통신사용자ID목록
     * @param tenantId 테넌트ID
     * @return 테넌트통신사용자ID목록
     */
    @GetMapping("/api/rpa-auth/tenant/getTenantNormalUserIds")
    AppResponse<List<String>> getTenantNormalUserIds(@RequestParam("tenantId") String tenantId);

    /**
     * 가져오기테넌트사용자유형(1테이블테넌트관리관리원, 테이블통신사용자)
     * @param userId 사용자ID
     * @param tenantId 테넌트ID
     * @return 테넌트사용자유형(가능로null)
     */
    @GetMapping("/api/rpa-auth/tenant/getTenantUserType")
    AppResponse<Integer> getTenantUserType(
            @RequestParam("userId") String userId, @RequestParam("tenantId") String tenantId);

    // ==================== RoleController 역할닫기연결 ====================

    /**
     * 가져오기사용자역할목록
     * @return 역할목록
     */
    @GetMapping("/api/rpa-auth/role/getUserRoleList")
    AppResponse<List<Role>> getUserRoleList();

    /**
     * 조회역할
     * @param tenantId
     * @param dto
     * @return
     */
    @PostMapping("/api/rpa-auth/role/queryDetail")
    AppResponse<Role> queryRoleDetail(@RequestParam("tenantId") String tenantId, @RequestBody GetRoleDto dto);

    /**
     * 조회사용내부전체역할목록
     * @return 역할목록
     */
    @GetMapping("/api/rpa-auth/role/getUserRoleListInApp")
    AppResponse<List<Role>> queryRoleTreeList();

    /**
     * 추가역할
     * @param createRoleDto 생성역할DTO
     * @return 결과
     */
    @PostMapping("/api/rpa-auth/role/add")
    AppResponse<String> addRole(@RequestBody Object createRoleDto);

    /**
     * 역할
     * @param updateRoleDto 업데이트역할DTO
     * @return 결과
     */
    @PostMapping("/api/rpa-auth/role/update")
    AppResponse<String> updateRole(@RequestBody Object updateRoleDto);

    /**
     * 삭제역할
     * @param deleteCommonDto 삭제역할DTO
     * @return 삭제결과
     */
    @PostMapping("/api/rpa-auth/role/delete")
    AppResponse<String> deleteRole(@RequestBody Object deleteCommonDto);

    /**
     * 근거이름조회역할
     * @param listRoleDto 조회역할DTO
     * @return 역할분목록
     */
    @PostMapping("/api/rpa-auth/role/search")
    AppResponse<PageDto<Role>> searchRole(@RequestBody Object listRoleDto);

    // ==================== ResourceController 권한닫기연결 ====================

    /**
     * 현재로그인사용자에서사용중의정보
     * @return 목록
     */
    @GetMapping("/api/rpa-auth/resource/currentResourceList")
    AppResponse<List<Resource>> getUserResourceList();

    // ==================== LoginController 로그인로그아웃닫기연결 ====================

    /**
     * 출력로그인
     * @return 로그아웃결과
     */
    @PostMapping("/api/rpa-auth/logout")
    AppResponse<String> logout();

    /**
     * 조회로그인상태
     * @return 로그인상태
     */
    @GetMapping("/api/rpa-auth/login-status")
    AppResponse<Boolean> loginStatus();

    /**
     * 가져오기token
     * @return token
     */
    @GetMapping("/api/rpa-auth/token")
    AppResponse<String> getToken();

    /**
     * 일: 인증
     * 인증사용자(휴대폰 번호+비밀번호 또는 휴대폰 번호+인증 코드)
     * 반환시인증, 사용후가져오기테넌트목록
     * @param loginDto 로그인요청 매개변수
     * @return 시인증
     */
    @PostMapping("/api/rpa-auth/pre-authenticate")
    AppResponse<String> preAuthenticate(@RequestBody Object loginDto);

    /**
     * 이: 가져오기테넌트목록
     * 사용시인증가져오기사용자의테넌트목록
     * 시미완료생성 session
     * @param tempToken 시인증
     * @return 테넌트목록
     */
    @GetMapping("/api/rpa-auth/tenant/list")
    AppResponse<List<Tenant>> getTenantList(@RequestParam(value = "tempToken", required = false) String tempToken);

    @GetMapping("/api/rpa-auth/tenant/expiration")
    AppResponse<TenantExpirationDto> getExpiration();

    /**
     * 삼: 정상방식로그인
     * 사용자선택테넌트후, 사용시인증및테넌트ID완료로그인
     * 시생성 session
     * @param tempToken 시인증
     * @param tenantId 선택의테넌트ID
     * @return 로그인성공반환사용자 정보
     */
    @PostMapping("/api/rpa-auth/login")
    AppResponse<User> login(@RequestParam("tempToken") String tempToken, @RequestParam("tenantId") String tenantId);

    /**
     * 전송짧음정보인증 코드
     * 사용비밀로그인및회원가입
     * @param phone 휴대폰 번호
     * @return 전송결과
     */
    @PostMapping("/api/rpa-auth/verification-code/send")
    AppResponse<String> sendVerificationCode(@RequestParam("phone") String phone);

    /**
     * 비밀번호
     * 사용자비밀번호후, 업데이트ShopRPA 계정및UAP비밀번호
     * @param setPasswordDto 비밀번호요청 매개변수
     * @return 여부성공
     */
    @PostMapping("/api/rpa-auth/password/set")
    AppResponse<Boolean> setPasswordAndLogin(@RequestBody Object setPasswordDto);

    /**
     * 조회사용자여부완료회원가입
     * @param phone 휴대폰 번호또는로그인이름
     * @return 여부완료회원가입
     */
    @GetMapping("/api/rpa-auth/user/exist")
    AppResponse<Boolean> checkUserExist(@RequestParam("phone") String phone);

    /**
     * 삭제ShopRPA 계정
     * @param phone 휴대폰 번호
     * @return 삭제결과
     */
    @PostMapping("/api/rpa-auth/iflytek-account/delete")
    AppResponse<String> deleteIflytekAccount(@RequestParam("phone") String phone);

    /**
     * 사용자회원가입(일)
     * 입력휴대폰 번호, 인증 코드, 사용자명
     * 에서ShopRPA 계정및UAP생성사용자(사용비밀번호)
     * 반환시인증사용후비밀번호
     * @param registerDto 회원가입요청 매개변수
     * @return 시인증
     */
    @PostMapping("/api/rpa-auth/register")
    AppResponse<String> register(@RequestBody Object registerDto);

    /**
     * 새로고침Token
     * 사용 refreshToken 새로고침 accessToken
     *
     * @param accessToken accessToken
     * @return 새로고침결과
     */
    @PostMapping("/api/rpa-auth/refresh-token")
    AppResponse<Boolean> refreshToken(@RequestParam("accessToken") String accessToken);

    // ==================== DeptController 모듈닫기연결 ====================

    /**
     * 가져오기모듈
     * @return 모듈
     */
    @GetMapping("/api/rpa-auth/dept/queryTreeList")
    AppResponse<Object> queryDeptTreeList();

    /**
     * 통신경과모듈의id조회모든모듈
     * @param dto 조회모듈DTO
     * @return 모듈목록
     */
    @PostMapping("/api/rpa-auth/dept/queryDeptNodeByPid")
    AppResponse<List<DeptTreeNodeVo>> queryDeptTreeByPid(@RequestBody Object dto);

    /**
     * 가져오기테넌트이름
     * @return 테넌트이름
     */
    @GetMapping("/api/rpa-auth/dept/queryTenantName")
    AppResponse<String> queryTenantName();

    /**
     * 통신경과deptId조회모듈이름
     * @param dto 조회모듈ID DTO
     * @return 모듈이름
     */
    @PostMapping("/api/rpa-auth/dept/queryDeptNameByDeptId")
    AppResponse<DeptNameVo> queryDeptNameByDeptId(@RequestBody Object dto);

    /**
     * 추가모듈
     * @param createUapOrgDto 생성모듈DTO
     * @return 결과
     */
    @PostMapping("/api/rpa-auth/dept/add")
    AppResponse<String> addDept(@RequestBody Object createUapOrgDto);

    /**
     * 모듈
     * @param editOrgDto 모듈DTO
     * @return 결과
     */
    @PostMapping("/api/rpa-auth/dept/edit")
    AppResponse<String> editDept(@RequestBody Object editOrgDto);

    /**
     * 삭제모듈
     * @param deleteCommonDto 삭제모듈DTO
     * @return 결과
     */
    @PostMapping("/api/rpa-auth/dept/delete")
    AppResponse<String> deleteDept(@RequestBody Object deleteCommonDto);

    /**
     * 조회모듈, 사람데이터, 사람
     * @return 모듈및사람원정보
     */
    @GetMapping("/api/rpa-auth/dept/treeAndPerson")
    AppResponse<java.util.Map<String, Object>> treeAndPerson();

    /**
     * 모듈사람데이터정보조회
     * @param dto 조회모듈DTO
     * @return 모듈사람요소목록
     */
    @PostMapping("/api/rpa-auth/dept/queryDeptPersonNodeByPid")
    AppResponse<List<DeptPersonTreeNodeVo>> queryDeptPersonNodeByPid(@RequestBody Object dto);

    /**
     * 조회현재기기의모든사용자
     * @param dto 조회모듈ID DTO
     * @return 사용자목록
     */
    @PostMapping("/api/rpa-auth/dept/queryUserListByDeptId")
    AppResponse<List<UserVo>> queryAllUserByDeptId(@RequestBody Object dto);

    /**
     * 가져오기현재로그인사용자의모듈levelCode, deptIdPath
     * @return 모듈levelCode
     */
    @GetMapping("/api/rpa-auth/dept/current/levelCode")
    AppResponse<String> getCurrentLevelCode();

    /**
     * 가져오기현재로그인사용자의모듈ID
     * @return 모듈ID
     */
    @GetMapping("/api/rpa-auth/dept/current/id")
    AppResponse<String> getCurrentDeptId();

    /**
     * 가져오기현재로그인사용자의모듈정보
     * @return 모듈정보
     */
    @GetMapping("/api/rpa-auth/dept/current")
    AppResponse<Org> getCurrentDeptInfo();

    /**
     * 근거모듈ID조회모듈정보
     * @param id 모듈ID
     * @return 모듈정보
     */
    @GetMapping("/api/rpa-auth/dept/info")
    AppResponse<Org> getDeptInfoByDeptId(@RequestParam("id") String id);

    /**
     * 조회모듈ID의levelCode
     * @param id 모듈ID
     * @return levelCode
     */
    @GetMapping("/api/rpa-auth/dept/levelCode")
    AppResponse<String> getLevelCodeByDeptId(@RequestParam("id") String id);

    /**
     * 조회지정기기모든기기의사용자수
     * @param id 모듈ID
     * @return 사용자수
     */
    @GetMapping("/api/rpa-auth/dept/userNum")
    AppResponse<Long> getUserNumByDeptId(@RequestParam("id") String id);

    /**
     * 근거모듈ID목록가져오기모듈정보목록
     * @param orgIdList 모듈ID목록
     * @return 모듈정보목록
     */
    @PostMapping("/api/rpa-auth/dept/queryByIds")
    AppResponse<List<Org>> queryOrgListByIds(@RequestBody List<String> orgIdList);

    /**
     * 근거사용자ID가져오기모듈ID
     * @param userId 사용자ID
     * @return 모듈ID
     */
    @GetMapping("/api/rpa-auth/dept/user/deptId")
    AppResponse<String> getDeptIdByUserId(
            @RequestParam("userId") String userId, @RequestParam("tenantId") String tenantId);

    /**
     * 조회데이터권한, 예일개모듈목록
     * @return 데이터권한
     */
    @GetMapping("/api/rpa-auth/dept/dataAuth")
    AppResponse<DataAuthDetailDo> getDataAuthWithDeptList();

    // ==================== DataAuthController 데이터권한닫기연결 ====================

    /**
     * 근거역할ID조회권한목록
     * @param tenantId
     * @param roleId
     * @return
     */
    @GetMapping("/api/rpa-auth/dataAuth/getAuthorityListByRoleId")
    AppResponse<List<Authority>> queryAuthorityListByRoleId(
            @RequestParam("tenantId") String tenantId, @RequestParam("roleId") String roleId);

    /**
     * 조회선택의데이터권한
     * @param roleId 역할ID
     * @return 데이터권한목록
     */
    @GetMapping("/api/rpa-auth/dataAuth/queryCheckedDataAuth")
    AppResponse<List<DataAuthorityWithDimDictDto>> getCheckedDataAuth(@RequestParam("roleId") String roleId);

    /**
     * 역할지정데이터권한
     * @param bindRoleDataAuthDto 지정역할데이터권한DTO
     * @return 결과
     */
    @PostMapping("/api/rpa-auth/dataAuth/bindDataAuth")
    AppResponse<String> bindDataAuth(@RequestBody Object bindRoleDataAuthDto);

    // ==================== AuthController 메뉴권한닫기연결 ====================

    /**
     * 현재로그인사용자에서사용중의메뉴정보
     * @return 메뉴
     */
    @GetMapping("/api/rpa-auth/menu/getUserAuthTreeInApp")
    AppResponse<List<TreeNode>> getUserAuthTreeInApp();

    /**
     * 조회메뉴, 권한
     * @param roleId 역할ID
     * @return 메뉴권한
     */
    @GetMapping("/api/rpa-auth/menu/getAuthResourceTreeInApp")
    AppResponse<TreeNode> getAuthResourceTreeInApp(@RequestParam("roleId") String roleId);

    /**
     * 저장메뉴, 
     * @param roleAuthResourceDto 역할권한DTO
     * @return 결과
     */
    @PostMapping("/api/rpa-auth/menu/save")
    AppResponse<String> saveRoleAuth(@RequestBody Object roleAuthResourceDto);
}
