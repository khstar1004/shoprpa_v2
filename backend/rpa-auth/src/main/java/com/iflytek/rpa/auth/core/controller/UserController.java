package com.iflytek.rpa.auth.core.controller;

import com.iflytek.rpa.auth.auditRecord.constants.AuditLog;
import com.iflytek.rpa.auth.core.entity.*;
import com.iflytek.rpa.auth.core.entity.CreateUapUserDto;
import com.iflytek.rpa.auth.core.entity.UpdateUapUserDto;
import com.iflytek.rpa.auth.core.service.UserService;
import com.iflytek.rpa.auth.utils.AppResponse;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 요소
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 회원가입
     * @param request
     * @return
     */
    @PostMapping("/register")
    public AppResponse<String> register(@RequestBody @Valid RegisterDto registerDto, HttpServletRequest request)
            throws IOException {

        return userService.register(registerDto, request);
    }

    /**
     * 조회현재로그인의사용자 정보
     * @param request
     * @return
     */
    @GetMapping("/info")
    public AppResponse<User> getUserInfo(HttpServletRequest request) {

        return userService.getUserInfo(request);
    }

    /**
     * 추가요소
     * @param createUapUserDto
     * @param request
     * @return
     */
    @AuditLog(moduleName = "관리자 권한", typeName = "추가구성원")
    @PostMapping("/add")
    public AppResponse<String> addUser(@RequestBody CreateUapUserDto createUapUserDto, HttpServletRequest request)
            throws IOException {
        return userService.addUser(createUapUserDto, request);
    }

    /**
     * 요소
     * @param
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public AppResponse<String> editUser(@RequestBody UpdateUapUserDto updateUapUserDto, HttpServletRequest request)
            throws IOException {
        return userService.editUser(updateUapUserDto, request);
    }

    /**
     * 삭제요소
     * @param
     * @param request
     * @return
     */
    @AuditLog(moduleName = "관리자 권한", typeName = "제거구성원")
    @PostMapping("/delete")
    public AppResponse<String> deleteUser(@RequestBody UserDeleteDto userDto, HttpServletRequest request)
            throws IOException {
        return userService.deleteUser(userDto, request);
    }

    /**
     * 사용/사용 안 함요소
     * @param
     * @param request
     * @return
     */
    @PostMapping("/enable")
    public AppResponse<String> enableUser(@RequestBody UserEnableDto userDto, HttpServletRequest request) {
        return userService.enableUser(userDto, request);
    }

    /**
     * 변수변경모듈
     * @param
     * @param request
     * @return
     */
    @PostMapping("/changeDept")
    public AppResponse<String> changeDept(@RequestBody UserChangeDeptDto userDto, HttpServletRequest request) {
        return userService.changeDept(userDto, request);
    }

    /**
     * 조회현재기기의전체사용자(모듈추가, 모듈사람드롭다운)
     * @param
     * @param request
     * @return
     */
    @GetMapping("/queryAllListByOrgId")
    public AppResponse<List<User>> queryUserDetailListByOrgId(
            @RequestParam("orgId") String orgId, HttpServletRequest request) throws IOException {
        return userService.queryUserDetailListByOrgId(orgId, request);
    }

    /**
     * 분조회현재기기의사용자
     * @param
     * @param request
     * @return
     */
    @PostMapping("/queryListByOrgId")
    public AppResponse<PageDto<DeptUserDto>> queryUserListByOrgId(
            @RequestBody ListUserDto listUserDto, HttpServletRequest request) throws IOException { //

        return userService.queryUserListByOrgId(listUserDto, request);
    }

    /**
     * 분가져오기역할지정의사용자목록, 가능근거로그인이름또는이름조회
     * @param
     * @param request
     * @return
     */
    @PostMapping("/queryBindListByRole")
    public AppResponse<PageDto<User>> queryBindListByRole(
            @RequestBody ListUserByRoleDto listUserByRoleDto, HttpServletRequest request) throws IOException {
        return userService.queryBindListByRole(listUserByRoleDto, request);
    }

    /**
     * 사람원해제역할
     * @param bindRoleDto
     * @param request
     * @return
     */
    @PostMapping("/unbindRole")
    public AppResponse<String> unbindRole(@RequestBody BindRoleDto bindRoleDto, HttpServletRequest request)
            throws IOException {
        return userService.unbindRole(bindRoleDto, request);
    }

    /**
     * 이름검색모든요소또는모듈
     * @param
     * @param request
     * @return
     */
    @GetMapping("/searchDeptOrUser")
    public AppResponse<GetDeptOrUserDto> searchDeptOrUser(
            @RequestParam("name") String name, HttpServletRequest request) {
        return userService.searchDeptOrUser(name, request);
    }

    /**
     * 역할관리관리-근거모듈id조회모듈아래의사람원및모듈
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/queryUserAndDept")
    public AppResponse<List<CurrentDeptUserDto>> queryUserAndDept(
            @RequestParam("id") String id, HttpServletRequest request) {

        return userService.queryUserAndDept(id, request);
    }

    /**
     * 역할관리관리-근거이름문자또는휴대폰 번호조회요소
     * @param keyWord
     * @param request
     * @return
     */
    @GetMapping("/searchUserWithStatus")
    public AppResponse<List<CurrentDeptUserDto>> searchUserWithStatus(
            @RequestParam("keyWord") String keyWord, HttpServletRequest request) throws IOException {

        return userService.searchUserWithStatus(keyWord, request);
    }

    /**
     * 역할관리관리-추가구성원
     * @param bindUserListDto
     * @param request
     * @return
     */
    @PostMapping("/batchBindRole")
    AppResponse<String> bindUserListRole(@RequestBody BindUserListDto bindUserListDto, HttpServletRequest request)
            throws IOException {

        return userService.bindUserListRole(bindUserListDto, request);
    }

    /**
     * 중-봇-모든아래선택-조회연결
     * 근거입력의닫기 문자(이름또는휴대폰 번호)조회사용자
     * @param keyword
     * @param deptId
     * @return
     */
    @PostMapping("/getUserByNameOrPhone")
    AppResponse<List<UserSearchDto>> getUserByNameOrPhone(String keyword, String deptId, HttpServletRequest request) {
        return userService.getUserByNameOrPhone(keyword, deptId, request);
    }

    /**
     * 가져오기사용자정보
     * @param tenantId
     * @param getUserDto
     * @param request
     * @return
     */
    @PostMapping("/getUserExtendInfo")
    public AppResponse<UserExtendDto> queryUserExtendInfo(
            @RequestParam("tenantId") String tenantId, @RequestBody GetUserDto getUserDto, HttpServletRequest request)
            throws IOException {
        return userService.queryUserExtendInfo(tenantId, getUserDto, request);
    }

    /**
     * 가져오기현재로그인사용자
     * @param request HTTP요청 
     * @return 현재로그인사용자 정보
     */
    @GetMapping("/current")
    public AppResponse<User> getCurrentLoginUser(HttpServletRequest request) {
        return userService.getCurrentLoginUser(request);
    }

    /**
     * 가져오기현재로그인사용자ID
     * @param request HTTP요청 
     * @return 현재로그인사용자ID
     */
    @GetMapping("/current/id")
    public AppResponse<String> getCurrentUserId(HttpServletRequest request) {
        return userService.getCurrentUserId(request);
    }

    /**
     * 가져오기현재로그인사용자명
     * @param request HTTP요청 
     * @return 현재로그인사용자명
     */
    @GetMapping("/current/username")
    public AppResponse<String> getCurrentLoginUsername(HttpServletRequest request) {
        return userService.getCurrentLoginUsername(request);
    }

    /**
     * 근거사용자ID조회로그인이름
     * @param id 사용자ID
     * @param request HTTP요청 
     * @return 로그인이름
     */
    @GetMapping("/loginName")
    public AppResponse<String> getLoginNameById(@RequestParam("id") String id, HttpServletRequest request) {
        return userService.getLoginNameById(id, request);
    }

    /**
     * 근거사용자ID조회이름
     * @param id 사용자ID
     * @param request HTTP요청 
     * @return 사용자이름
     */
    @GetMapping("/realName")
    public AppResponse<String> getRealNameById(@RequestParam("id") String id, HttpServletRequest request) {
        return userService.getRealNameById(id, request);
    }

    /**
     * 근거사용자ID조회사용자 정보
     * @param id 사용자ID
     * @param request HTTP요청 
     * @return 사용자 정보
     */
    @GetMapping("/infoById")
    public AppResponse<User> getUserInfoById(@RequestParam("id") String id, HttpServletRequest request) {
        return userService.getUserInfoById(id, request);
    }

    /**
     * 근거휴대폰 번호조회사용자이름
     * @param phone 휴대폰 번호
     * @param request HTTP요청 
     * @return 사용자이름
     */
    @GetMapping("/phone/realName")
    public AppResponse<String> getRealNameByPhone(@RequestParam("phone") String phone, HttpServletRequest request) {
        return userService.getRealNameByPhone(phone, request);
    }

    /**
     * 근거휴대폰 번호조회로그인이름
     * @param phone 휴대폰 번호
     * @param request HTTP요청 
     * @return 로그인이름
     */
    @GetMapping("/phone/loginName")
    public AppResponse<String> getLoginNameByPhone(@RequestParam("phone") String phone, HttpServletRequest request) {
        return userService.getLoginNameByPhone(phone, request);
    }

    /**
     * 여부사용자(ext_info = 1 테이블사용자)
     * @param phone 휴대폰 번호
     * @return 여부사용자
     */
    @GetMapping("/history")
    public AppResponse<Boolean> isHistoryUser(@RequestParam(required = false) String phone) {
        return userService.isHistoryUser(phone);
    }

    /**
     * 근거휴대폰 번호조회사용자 정보
     * @param phone 휴대폰 번호
     * @param request HTTP요청 
     * @return 사용자 정보
     */
    @GetMapping("/phone/info")
    public AppResponse<User> getUserInfoByPhone(@RequestParam("phone") String phone, HttpServletRequest request) {
        return userService.getUserInfoByPhone(phone, request);
    }

    /**
     * 근거사용자ID목록조회사용자 정보목록(다중지원100개id)
     * @param userIdList 사용자ID목록
     * @param request HTTP요청 
     * @return 사용자 정보목록
     */
    @PostMapping("/queryByIds")
    public AppResponse<List<User>> queryUserListByIds(
            @RequestBody List<String> userIdList, HttpServletRequest request) {
        return userService.queryUserListByIds(userIdList, request);
    }

    /**
     * 근거이름조회사람원
     * @param keyword 닫기 문자
     * @param deptId 모듈ID(가능선택)
     * @param request HTTP요청 
     * @return 사용자 정보목록
     */
    @GetMapping("/search/name")
    public AppResponse<List<User>> searchUserByName(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "deptId", required = false) String deptId,
            HttpServletRequest request) {
        return userService.searchUserByName(keyword, deptId, request);
    }

    /**
     * 근거휴대폰 번호조회사람원
     * @param keyword 닫기 문자
     * @param deptId 모듈ID(가능선택)
     * @param request HTTP요청 
     * @return 사용자 정보목록
     */
    @GetMapping("/search/phone")
    public AppResponse<List<User>> searchUserByPhone(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "deptId", required = false) String deptId,
            HttpServletRequest request) {
        return userService.searchUserByPhone(keyword, deptId, request);
    }

    /**
     * 근거이름또는휴대폰 번호조회사람원
     * @param keyword 닫기 문자
     * @param deptId 모듈ID(가능선택)
     * @param request HTTP요청 
     * @return 사용자 정보목록
     */
    @GetMapping("/search")
    public AppResponse<List<User>> searchUserByNameOrPhone(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "deptId", required = false) String deptId,
            HttpServletRequest request) {
        return userService.searchUserByNameOrPhone(keyword, deptId, request);
    }

    /**
     * 가져오기현재사용자권한목록
     * @param request HTTP요청 
     * @return 사용자권한목록
     */
    @GetMapping("/current/permissions")
    public AppResponse<List<Permission>> getCurrentUserPermissionList(HttpServletRequest request) throws IOException {
        return userService.getCurrentUserPermissionList(request);
    }

    /**
     * 가져오기Casdoor로그인재지정URL(Casdoor사용)
     * @param request HTTP요청 
     * @return 로그인재지정URL
     */
    @GetMapping("/redirect-url")
    public AppResponse<String> getRedirectUrl(HttpServletRequest request) {
        return userService.getRedirectUrl(request);
    }

    /**
     * Casdoor OAuth로그인(Casdoor사용)
     * @param code OAuth권한 부여코드
     * @param state OAuth state매개변수
     * @param request HTTP요청 
     * @return 사용자 정보
     */
    @PostMapping("/sign/in")
    public AppResponse<User> signIn(
            @RequestParam("code") String code, @RequestParam("state") String state, HttpServletRequest request)
            throws IOException {
        return userService.signIn(code, state, request);
    }

    /**
     * 조회사용자로그인상태(Casdoor사용)
     * @param request HTTP요청 
     * @return 사용자 정보, 결과가로그인되지 않았습니다반환오류
     */
    @GetMapping("/login-check")
    public AppResponse<User> checkLoginStatus(HttpServletRequest request) {
        return userService.checkLoginStatus(request);
    }

    /**
     * 새로고침서버token(Casdoor사용, accessToken경과시사용)
     * @param request HTTP요청 
     * @return 결과
     */
    @PostMapping("/refresh-token")
    public AppResponse<String> refreshToken(HttpServletRequest request) {
        return userService.refreshToken(request);
    }

    /**
     * 가져오기현재로그인사용자의권한
     * 클라이언트시작시호출연결
     * 근거session조회테넌트code, 결과가예테넌트, 이면조회데이터베이스중권한
     * 결과가있음데이터, 있음모든권한
     *
     * @param request HTTP요청 
     * @return 사용자권한정보
     */
    @GetMapping("/entitlement")
    public AppResponse<UserEntitlementDto> getCurrentUserEntitlement(HttpServletRequest request) {
        return userService.getCurrentUserEntitlement(request);
    }

    /**
     * 근거사용자ID조회사용자 정보
     * @param id 사용자ID
     * @param request HTTP요청 
     * @return 사용자 정보
     */
    @GetMapping("/getNameById")
    public AppResponse<String> getNameById(@RequestParam("id") String id, HttpServletRequest request) {
        return userService.getNameById(id, request);
    }

    /**
     * 가져오기완료모듈사용자목록(분)
     * @param dto 조회파일
     * @param request HTTP요청 
     * @return 완료모듈사용자분목록
     */
    @PostMapping("/getDeployedUserList")
    public AppResponse<PageDto<RobotExecute>> getDeployedUserList(
            @RequestBody GetDeployedUserListDto dto, HttpServletRequest request) {
        return userService.getDeployedUserList(dto, request);
    }

    /**
     * 가져오기완료모듈사용자목록 있음테넌트id
     * @param dto 조회파일
     * @param request HTTP요청 
     * @return 완료모듈사용자분목록
     */
    @PostMapping("/getDeployedUserListWithoutTenantId")
    public AppResponse<PageDto<RobotExecute>> getDeployedUserListWithoutTenantId(
            @RequestBody GetDeployedUserListDto dto, HttpServletRequest request) {
        return userService.getDeployedUserListWithoutTenantId(dto, request);
    }

    /**
     * 가져오기미완료모듈사용자목록
     * @param dto 조회파일
     * @param request HTTP요청 
     * @return 미완료모듈사용자목록
     */
    @PostMapping("/getUserUnDeployed")
    public AppResponse<List<MarketDto>> getUserUnDeployed(
            @RequestBody GetUserUnDeployedDto dto, HttpServletRequest request) {
        return userService.getUserUnDeployed(dto, request);
    }

    /**
     * 가져오기마켓사용자목록(분)
     * @param dto 조회파일
     * @param request HTTP요청 
     * @return 마켓사용자분목록
     */
    @PostMapping("/getMarketUserList")
    public AppResponse<PageDto<MarketDto>> getMarketUserList(
            @RequestBody GetMarketUserListDto dto, HttpServletRequest request) {
        return userService.getMarketUserList(dto, request);
    }

    /**
     * 가져오기 공유마켓사용자목록(분)
     * @param dto 조회파일
     * @param request HTTP요청 
     * @return 공유마켓사용자분목록
     */
    @PostMapping("/getMarketUserListByPublic")
    public AppResponse<PageDto<MarketDto>> getMarketUserListByPublic(
            @RequestBody GetMarketUserListByPublicDto dto, HttpServletRequest request) {
        return userService.getMarketUserListByPublic(dto, request);
    }

    /**
     * 근거휴대폰 번호조회마켓사용자(아니요에서마켓중의사용자)
     * @param dto 조회파일
     * @param request HTTP요청 
     * @return 사용자목록
     */
    @PostMapping("/getMarketUserByPhone")
    public AppResponse<List<MarketDto>> getMarketUserByPhone(
            @RequestBody GetMarketUserByPhoneDto dto, HttpServletRequest request) {
        return userService.getMarketUserByPhone(dto, request);
    }

    /**
     * 근거휴대폰 번호조회마켓중의사용자(사용마켓모든, 정렬제거)
     * @param dto 조회파일
     * @param request HTTP요청 
     * @return 사용자목록
     */
    @PostMapping("/getMarketUserByPhoneForOwner")
    public AppResponse<List<MarketDto>> getMarketUserByPhoneForOwner(
            @RequestBody GetMarketUserByPhoneForOwnerDto dto, HttpServletRequest request) {
        return userService.getMarketUserByPhoneForOwner(dto, request);
    }

    /**
     * 근거사용자ID목록조회테넌트사용자목록
     * @param dto 조회파일
     * @param request HTTP요청 
     * @return 테넌트사용자목록
     */
    @PostMapping("/getMarketTenantUserList")
    public AppResponse<List<TenantUser>> getMarketTenantUserList(
            @RequestBody GetMarketTenantUserListDto dto, HttpServletRequest request) {
        return userService.getMarketTenantUserList(dto, request);
    }
}