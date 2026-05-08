package com.iflytek.rpa.auth.idp.enterpriseIdentity;

import com.iflytek.rpa.auth.core.entity.*;
import com.iflytek.rpa.auth.core.entity.ChangePasswordDto;
import com.iflytek.rpa.auth.core.entity.LoginDto;
import com.iflytek.rpa.auth.core.entity.RegisterDto;
import com.iflytek.rpa.auth.core.entity.Tenant;
import com.iflytek.rpa.auth.core.entity.User;
import com.iflytek.rpa.auth.idp.AuthenticationService;
import com.iflytek.rpa.auth.utils.AppResponse;
import com.iflytek.rpa.auth.utils.ErrorCodeEnum;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * SSO인증 서비스
 * 사용있음모듈, 연결의OAuth2/OIDC인증시스템
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "private-enterprise")
public class EnterpriseAuthenticationServiceImpl implements AuthenticationService {

    @Override
    public String preAuthenticate(LoginDto loginDto, HttpServletRequest request) {
        // Enterprise SSO adapter is intentionally fail-closed until an OAuth2/OIDC provider is configured.
        // 1. 재지정까지SSO로그인
        // 2. 관리OAuth2권한 부여코드돌아가기조정
        // 3. token
        // 4. 완료시인증
        throw new UnsupportedOperationException("SSO인증공가능대기");
    }

    @Override
    public User loginWithTenant(String tempToken, String tenantId, HttpServletRequest servletRequest) {
        // Enterprise SSO adapter is intentionally fail-closed until an OAuth2/OIDC provider is configured.
        // 1. 인증시인증
        // 2. 에서저장중가져오기사용자 정보(패키지platform)
        // 3. 까지UAP생성session
        // 4. 를platform저장까지session
        // 5. 있음클라이언트로그인실행단일로그인
        throw new UnsupportedOperationException("SSO정상방식로그인공가능대기");
    }

    @Override
    @Deprecated
    public User login(LoginDto loginDto, HttpServletRequest servletRequest) {
        throw new UnsupportedOperationException("SSO 모드는 기존 로그인 경로를 지원하지 않습니다");
    }

    @Override
    public String getPhoneByTempToken(String tempToken) {
        // Enterprise SSO temp-token lookup is not available without a configured provider.
        throw new UnsupportedOperationException("가져오기휴대폰 번호공가능대기");
    }

    @Override
    public LoginDto getLoginInfoByTempToken(String tempToken) {
        // Enterprise SSO temp-token lookup is not available without a configured provider.
        throw new UnsupportedOperationException("가져오기로그인정보공가능대기");
    }

    @Override
    public AppResponse<List<Tenant>> getTenantList(String tempToken, HttpServletRequest request) {
        // Enterprise SSO tenant lookup is not available without a configured provider.
        // 1. 에서시인증중가져오기사용자 정보
        // 2. 호출SSO API가져오기테넌트목록
        log.warn("SSO방식가져오기테넌트목록공가능대기, 시인증: {}", tempToken);
        return AppResponse.error(ErrorCodeEnum.E_SERVICE, "SSO방식가져오기테넌트목록공가능대기");
    }

    @Override
    public String register(RegisterDto registerDto, HttpServletRequest request) {
        // SSO통신일반지원하지 않음회원가입, 시스템일관리관리계정
        throw new UnsupportedOperationException("SSO방식지원하지 않음회원가입");
    }

    @Override
    public User setPasswordAndLogin(String tempToken, String password, String tenantId, HttpServletRequest request) {
        // SSO지원하지 않음비밀번호
        throw new UnsupportedOperationException("SSO방식지원하지 않음비밀번호");
    }

    @Override
    public boolean setPassword(String tempToken, String password, String tenantId, HttpServletRequest request) {
        // SSO지원하지 않음비밀번호
        throw new UnsupportedOperationException("SSO방식지원하지 않음비밀번호");
    }

    @Override
    public boolean queryUserExist(String loginName) {
        throw new UnsupportedOperationException("SSO 모드는 로컬 사용자 조회를 지원하지 않습니다");
    }

    @Override
    public AppResponse<String> logout(HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("SSO방식지원하지 않음출력");
    }

    @Override
    public AppResponse<Boolean> refreshToken(HttpServletRequest request, String accessToken) {
        return AppResponse.error(ErrorCodeEnum.E_SERVICE_NOT_SUPPORT, "SSO 모드는 토큰 새로고침을 지원하지 않습니다");
    }

    @Override
    public String getVerificationCode(String phone, String scene) {
        // SSO지원하지 않음인증 코드 로그인
        throw new UnsupportedOperationException("SSO방식지원하지 않음인증 코드 로그인");
    }

    @Override
    public String getVerificationCode(String phone) {
        return getVerificationCode(phone, "login");
    }

    @Override
    public AppResponse<Boolean> checkSession(HttpServletRequest request, HttpServletResponse response) {
        // SSO방식통신일반시스템일관리관리빈까지, 직선연결반환성공
        // 결과가필요, 가능으로매개유형
        return AppResponse.success(true);
    }

    @Override
    public boolean checkLoginStatus(HttpServletRequest request) {
        return false;
    }

    @Override
    public AppResponse<String> changePassword(ChangePasswordDto changePasswordDto) {
        // SSO방식지원하지 않음수정비밀번호
        throw new UnsupportedOperationException("SSO방식지원하지 않음수정비밀번호");
    }

    @Override
    public AppResponse<String> addUser(AddUserDto user, HttpServletRequest request) {
        return AppResponse.error(ErrorCodeEnum.E_SERVICE_NOT_SUPPORT, "SSO 모드는 직접 사용자 생성을 지원하지 않습니다");
    }
}
