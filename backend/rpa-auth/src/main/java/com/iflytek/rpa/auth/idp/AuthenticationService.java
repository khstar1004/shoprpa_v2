package com.iflytek.rpa.auth.idp;

import com.iflytek.rpa.auth.core.entity.*;
import com.iflytek.rpa.auth.core.entity.LoginDto;
import com.iflytek.rpa.auth.core.entity.RegisterDto;
import com.iflytek.rpa.auth.core.entity.Tenant;
import com.iflytek.rpa.auth.core.entity.User;
import com.iflytek.rpa.auth.utils.AppResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface AuthenticationService {

    String SCENE_LOGIN = "login";
    String SCENE_REGISTER = "register";
    String SCENE_SET_PASSWORD = "set_password";

    /**
     * 인증(일)
     * 인증사용자, 아니요생성 session
     * 반환시인증, 사용후가져오기테넌트목록
     * 결과가사용자에서UAP찾을 수 없습니다, 회원가입사용자
     *
     * @param loginDto 로그인요청 DTO
     * @param request HTTP요청 (사용회원가입UAP사용자)
     * @return 시인증(가능으로예 token 또는식별자)
     */
    String preAuthenticate(LoginDto loginDto, HttpServletRequest request);

    /**
     * 정상방식로그인(이)
     * 사용자선택테넌트후, 사용시인증완료로그인, 생성 session
     *
     * @param tempToken 시인증
     * @param tenantId 선택의테넌트ID
     * @param servletRequest HTTP요청 
     * @return 로그인사용자 정보
     */
    User loginWithTenant(String tempToken, String tenantId, HttpServletRequest servletRequest);

    /**
     * 가져오기테넌트목록(이전)
     * 아니요인증가능행지정예파싱시인증
     *
     * @param tempToken 시인증
     * @param request   HTTP요청 
     * @return 테넌트목록
     */
    AppResponse<List<Tenant>> getTenantList(String tempToken, HttpServletRequest request);

    /**
     * [완료]기존로그인방법법, 보관사용내용
     * 생성사용 preAuthenticate + loginWithTenant 로그인
     *
     * @param loginDto 로그인요청 DTO
     * @return 로그인결과
     */
    @Deprecated
    User login(LoginDto loginDto, HttpServletRequest servletRequest);

    /**
     * 근거시인증가져오기사용자휴대폰 번호
     * 사용에서미완료생성 session 시조회테넌트목록
     *
     * @param tempToken 시인증
     * @return 사용자휴대폰 번호
     */
    String getPhoneByTempToken(String tempToken);

    /**
     * 근거시인증가져오기사용자휴대폰 번호
     * 사용에서미완료생성 session 시조회테넌트목록
     *
     * @param tempToken 시인증
     * @return 사용자휴대폰 번호
     */
    LoginDto getLoginInfoByTempToken(String tempToken);

    /**
     * 회원가입(일)
     * 필요휴대폰 번호및인증 코드, 아니요필요비밀번호
     * 에서Shoprpa계정및UAP생성사용자(사용비밀번호)
     *
     * @param registerDto 회원가입요청 DTO
     * @return 시인증(사용후비밀번호)
     */
    String register(RegisterDto registerDto, HttpServletRequest request);

    /**
     * 비밀번호로그인(회원가입이)
     * 사용자비밀번호후, 업데이트Shoprpa계정및UAP비밀번호, 로그인
     *
     * @param tempToken 시인증
     * @param password 새비밀번호
     * @param tenantId 선택의테넌트ID
     * @param request HTTP요청 
     * @return 로그인사용자 정보
     */
    User setPasswordAndLogin(String tempToken, String password, String tenantId, HttpServletRequest request);

    /**
     * 조회사용자 정보
     *
     * @param loginName 로그인이름
     * @return 사용자 정보
     */
    boolean queryUserExist(String loginName);

    boolean setPassword(String tempToken, String password, String tenantId, HttpServletRequest request);

    AppResponse<String> logout(HttpServletRequest request, HttpServletResponse response);

    /**
     * 새로고침Token
     * 사용 refreshToken 새로고침 accessToken
     *
     * @param request     HTTP요청 
     * @param accessToken
     * @return 새로고침결과
     */
    AppResponse<Boolean> refreshToken(HttpServletRequest request, String accessToken);

    /**
     * 가져오기인증 코드
     * 완료인증 코드, 저장까지Redis, 전송짧음정보
     * 증가추가정도, 중지아니요서비스의인증 코드사용
     *
     * @param phone 휴대폰 번호
     * @param scene 서비스(예 register/login/set_password)
     * @return 전송결과
     */
    String getVerificationCode(String phone, String scene);

    /**
     * 가져오기인증 코드(로그인)
     *
     * @param phone 휴대폰 번호
     * @return 전송결과
     */
    default String getVerificationCode(String phone) {
        return getVerificationCode(phone, SCENE_LOGIN);
    }

    /**
     * 조회session여부있음
     * 시검증빈여부까지, 결과가빈까지이면강함제어출력로그인
     *
     * @param request HTTP요청 
     * @param response HTTP
     * @return 조회결과, 결과가빈까지반환오류 
     */
    AppResponse<Boolean> checkSession(HttpServletRequest request, HttpServletResponse response);

    /**
     * 조회로그인상태
     * 인증현재요청 여부완료로그인
     *
     * @param request HTTP요청 
     * @return 여부완료로그인
     */
    boolean checkLoginStatus(HttpServletRequest request);

    /**
     * 수정비밀번호
     * 인증비밀번호후, 업데이트로새비밀번호, 완료시인증사용후로그인
     *
     * @param changePasswordDto 수정비밀번호요청 매개변수(패키지계정, 휴대폰 번호, 비밀번호, 새비밀번호, 비밀번호)
     * @return 시인증(tempToken), 사용후가져오기테넌트목록및로그인
     */
    AppResponse<String> changePassword(ChangePasswordDto changePasswordDto);

    AppResponse<String> addUser(AddUserDto user, HttpServletRequest request);
}