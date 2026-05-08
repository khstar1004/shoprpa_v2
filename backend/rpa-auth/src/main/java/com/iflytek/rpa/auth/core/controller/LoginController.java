package com.iflytek.rpa.auth.core.controller;

import com.iflytek.rpa.auth.core.entity.*;
import com.iflytek.rpa.auth.dataPreheater.entity.InitDataEvent;
import com.iflytek.rpa.auth.idp.AuthenticationService;
import com.iflytek.rpa.auth.utils.AppResponse;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 로그아웃닫기
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class LoginController {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    /**
     * 조회session여부경과
     * 시검증빈여부까지, 결과가빈까지이면강함제어출력로그인
     * @param request
     * @param response
     * @return
     */
    @GetMapping("/check-session")
    public AppResponse<Boolean> checkSession(HttpServletRequest request, HttpServletResponse response) {
        return authenticationService.checkSession(request, response);
    }

    /**
     * 조회로그인상태
     *
     * @param request
     * @return
     */
    @GetMapping("/login-status")
    public AppResponse<Boolean> loginStatus(HttpServletRequest request) {
        try {
            boolean isLoggedIn = authenticationService.checkLoginStatus(request);
            log.debug("조회로그인상태: {}", isLoggedIn);
            return AppResponse.success(isLoggedIn);
        } catch (Exception e) {
            log.error("조회로그인상태예외", e);
            return AppResponse.success(false);
        }
    }

    /**
     * 가져오기token
     *
     * @param request
     * @return
     */
    @GetMapping("/token")
    public AppResponse<String> getToken(HttpServletRequest request) {
        return AppResponse.error(
                com.iflytek.rpa.auth.utils.ErrorCodeEnum.E_SERVICE_NOT_SUPPORT,
                "현재 인증 모드는 별도 토큰 발급을 지원하지 않습니다");
        //        return UapManagementClientUtil.getToken(request);
    }

    /**
     * 출력로그인
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @PostMapping(value = "/logout")
    public AppResponse<String> logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return authenticationService.logout(request, response);
    }

    /**
     * 일: 인증
     * 인증사용자(휴대폰 번호+비밀번호 또는 휴대폰 번호+인증 코드)
     * 반환시인증, 사용후가져오기테넌트목록
     * platform필드에서인증통신경과후저장까지저장중, 사용후가져오기테넌트목록시필터링
     *
     * @param loginDto 로그인요청 매개변수(패키지platform필드: client-클라이언트, admin-실행운영후, invite-초대연결)
     * @return 시인증
     */
    @PostMapping("/pre-authenticate")
    public AppResponse<String> preAuthenticate(@RequestBody @Validated LoginDto loginDto, HttpServletRequest request) {
        try {
            log.info("인증요청 , 휴대폰 번호: {}", loginDto.getPhone());
            String tempToken = authenticationService.preAuthenticate(loginDto, request);
            log.info("인증 성공, 휴대폰 번호: {}", loginDto.getPhone());
            return AppResponse.success(tempToken);
        } catch (com.iflytek.rpa.auth.blacklist.exception.ShouldBeBlackException e) {
            // 예외위출력, 전체영역예외관리기기관리
            throw e;
        } catch (Exception e) {
            log.error("인증 실패", e);
            return AppResponse.error(com.iflytek.rpa.auth.utils.ErrorCodeEnum.E_SERVICE, e.getMessage());
        }
    }

    /**
     * 이: 가져오기테넌트목록
     * 사용시인증가져오기사용자의테넌트목록
     * 시미완료생성 session
     * 근거인증시입력의platform필드필터링테넌트목록: 
     * - platform로client시, 반환전체테넌트목록
     * - platform로admin시, 반환개사람테넌트의목록(필터링개사람테넌트)
     * - platform로invite시, 반환전체테넌트목록(아니요필터링)
     *
     * @param tempToken 시인증
     * @return 테넌트목록
     */
    @GetMapping("/tenant/list")
    public AppResponse<List<Tenant>> getTenantList(
            @RequestParam(required = false) String tempToken, HttpServletRequest request) {
        try {
            log.info("가져오기테넌트목록, 시인증: {}", tempToken);
            return authenticationService.getTenantList(tempToken, request);
        } catch (Exception e) {
            log.error("가져오기테넌트목록실패", e);
            return AppResponse.error(com.iflytek.rpa.auth.utils.ErrorCodeEnum.E_SERVICE, "가져오기테넌트목록실패: " + e.getMessage());
        }
    }

    /**
     * 삼: 정상방식로그인
     * 사용자선택테넌트후, 사용시인증및테넌트ID완료로그인
     * 시생성 session
     *
     * @param tempToken 시인증
     * @param tenantId  선택의테넌트ID
     * @param request   HTTP요청 
     * @return 로그인성공반환사용자 정보
     */
    @PostMapping("/login")
    public AppResponse<User> login(
            @RequestParam @NotBlank(message = "시인증비워 둘 수 없습니다") String tempToken,
            @RequestParam @NotBlank(message = "테넌트 ID는 비워 둘 수 없습니다") String tenantId,
            HttpServletRequest request) {
        try {
            log.info("정상방식로그인요청 , 시인증: {}, 테넌트ID: {}", tempToken, tenantId);
            User user = authenticationService.loginWithTenant(tempToken, tenantId, request);
            log.info("정상방식로그인성공, 사용자ID: {}, 테넌트ID: {}", user.getId(), tenantId);
            // 팀마켓분유형 팀마켓
            eventPublisher.publishEvent(new InitDataEvent(this, tenantId));
            return AppResponse.success(user);
        } catch (Exception e) {
            log.error("정상방식로그인실패", e);
            return AppResponse.error(com.iflytek.rpa.auth.utils.ErrorCodeEnum.E_SERVICE, e.getMessage());
        }
    }

    /**
     * 전송짧음정보인증 코드
     * 사용비밀로그인및회원가입
     *
     * @param phone 휴대폰 번호
     * @return 전송결과
     */
    @PostMapping("/verification-code/send")
    public AppResponse<String> sendVerificationCode(
            @RequestParam @NotBlank(message = "휴대폰 번호는 비워 둘 수 없습니다") String phone, @RequestParam(required = false) String scene) {
        try {
            log.info("인증 코드 전송, 휴대폰 번호: {}", phone);

            // 사용연결방법법, 유형관리
            String result = authenticationService.getVerificationCode(phone, scene);
            log.info("인증 코드 전송 성공, 휴대폰 번호: {}", phone);
            return AppResponse.success(result);
        } catch (UnsupportedOperationException e) {
            log.warn("현재모듈방식지원하지 않음인증 코드 로그인, 휴대폰 번호: {}", phone);
            return AppResponse.error("현재모듈방식지원하지 않음인증 코드 로그인");
        } catch (Exception e) {
            log.error("인증 코드 전송 실패, 휴대폰 번호: {}", phone, e);
            return AppResponse.error("인증 코드 전송 실패: " + e.getMessage());
        }
    }

    /**
     * 사용자회원가입(일)
     * 입력휴대폰 번호, 인증 코드, 사용자명
     * 에서ShopRPA 계정및UAP생성사용자(사용비밀번호)
     * 반환시인증사용후비밀번호
     *
     * @param registerDto 회원가입요청 매개변수
     * @param request     HTTP요청 
     * @return 시인증
     */
    @PostMapping("/register")
    public AppResponse<String> register(@RequestBody @Validated RegisterDto registerDto, HttpServletRequest request) {
        try {
            log.info("사용자회원가입요청 , 휴대폰 번호: {}", registerDto.getPhone());

            // 호출회원가입서비스, 반환시인증
            String tempToken = authenticationService.register(registerDto, request);
            log.info("사용자회원가입성공, 휴대폰 번호: {}, 시인증완료완료", registerDto.getPhone());

            return AppResponse.success(tempToken);
        } catch (Exception e) {
            log.error("사용자회원가입실패", e);
            return AppResponse.error(com.iflytek.rpa.auth.utils.ErrorCodeEnum.E_SERVICE, "회원가입실패: " + e.getMessage());
        }
    }

    /**
     * 비밀번호
     * 사용자비밀번호후, 업데이트ShopRPA 계정및UAP비밀번호
     *
     * @param setPasswordDto 비밀번호요청 매개변수
     * @param request        HTTP요청 
     * @return 여부성공
     */
    @PostMapping("/password/set")
    public AppResponse<Boolean> setPasswordAndLogin(
            @RequestBody @Validated SetPasswordDto setPasswordDto, HttpServletRequest request) {
        try {
            log.info("비밀번호로그인요청 , 시인증: {}", setPasswordDto.getTempToken());

            // 인증비밀번호여부일
            if (!setPasswordDto.getPassword().equals(setPasswordDto.getConfirmPassword())) {
                return AppResponse.error(com.iflytek.rpa.auth.utils.ErrorCodeEnum.E_PARAM, "입력한 비밀번호가 올바르지 않습니다");
            }

            // 비밀번호로그인
            boolean res = authenticationService.setPassword(
                    setPasswordDto.getTempToken(), setPasswordDto.getPassword(), setPasswordDto.getTenantId(), request);

            log.info("비밀번호성공");
            return AppResponse.success(res);

        } catch (Exception e) {
            log.error("비밀번호로그인실패", e);
            return AppResponse.error(com.iflytek.rpa.auth.utils.ErrorCodeEnum.E_SERVICE, e.getMessage());
        }
    }

    /**
     * 조회사용자여부완료회원가입
     *
     * @param phone 휴대폰 번호또는로그인이름
     * @return 여부완료회원가입
     */
    @GetMapping("/user/exist")
    public AppResponse<Boolean> checkUserExist(@RequestParam @NotBlank(message = "휴대폰 번호는 비워 둘 수 없습니다") String phone) {
        try {
            boolean exist = authenticationService.queryUserExist(phone);
            return AppResponse.success(exist);
        } catch (Exception e) {
            log.error("조회사용자여부저장에서실패, 휴대폰 번호: {}", phone, e);
            return AppResponse.error(com.iflytek.rpa.auth.utils.ErrorCodeEnum.E_SERVICE, "조회실패: " + e.getMessage());
        }
    }

    /**
     * 삭제ShopRPA 계정
     *
     * @param phone 휴대폰 번호
     * @return 삭제결과
     */
    @PostMapping("/iflytek-account/delete")
    public AppResponse<String> deleteIflytekAccount(@RequestParam @NotBlank(message = "휴대폰 번호는 비워 둘 수 없습니다") String phone) {
        return AppResponse.error("현재모듈방식지원하지 않음삭제사용자");
    }

    /**
     * 새로고침Token
     * 사용 refreshToken 새로고침 accessToken
     *
     * @param request HTTP요청 
     * @return 새로고침결과
     */
    @PostMapping("/refresh-token")
    public AppResponse<Boolean> refreshToken(
            @RequestParam("accessToken") String accessToken, HttpServletRequest request) {
        try {
            log.info("새로고침Token요청 ");
            AppResponse<Boolean> response = authenticationService.refreshToken(request, accessToken);
            if (response.ok()) {
                log.info("새로고침Token성공");
            } else {
                log.warn("새로고침Token실패: {}", response.getMessage());
            }
            return response;
        } catch (Exception e) {
            log.error("새로고침Token예외", e);
            return AppResponse.error(com.iflytek.rpa.auth.utils.ErrorCodeEnum.E_SERVICE, "새로고침Token예외: " + e.getMessage());
        }
    }

    /**
     * 수정비밀번호
     * 인증비밀번호후, 업데이트로새비밀번호
     *
     * @param changePasswordDto 수정비밀번호요청 매개변수(패키지계정, 휴대폰 번호, 비밀번호, 새비밀번호, 비밀번호)
     * @return 수정결과
     */
    @PostMapping("/password/change")
    public AppResponse<String> changePassword(@RequestBody @Validated ChangePasswordDto changePasswordDto) {
        try {
            log.info("수정비밀번호요청 , 계정: {}, 휴대폰 번호: {}", changePasswordDto.getLoginName(), changePasswordDto.getPhone());

            AppResponse<String> response = authenticationService.changePassword(changePasswordDto);
            if (response.ok()) {
                log.info("수정비밀번호성공, 계정: {}, 시인증: {}", changePasswordDto.getLoginName(), response.getData());
            } else {
                log.warn("수정비밀번호실패, 계정: {}, 오류: {}", changePasswordDto.getLoginName(), response.getMessage());
            }
            return response;
        } catch (UnsupportedOperationException e) {
            log.warn("현재모듈방식지원하지 않음수정비밀번호, 계정: {}", changePasswordDto.getLoginName());
            return AppResponse.error(com.iflytek.rpa.auth.utils.ErrorCodeEnum.E_SERVICE, "현재모듈방식지원하지 않음수정비밀번호");
        } catch (Exception e) {
            log.error("수정비밀번호예외, 계정: {}", changePasswordDto.getLoginName(), e);
            return AppResponse.error(com.iflytek.rpa.auth.utils.ErrorCodeEnum.E_SERVICE, "수정비밀번호실패: " + e.getMessage());
        }
    }
}
