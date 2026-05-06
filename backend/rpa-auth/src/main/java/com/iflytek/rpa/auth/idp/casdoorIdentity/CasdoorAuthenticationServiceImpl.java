package com.iflytek.rpa.auth.idp.casdoorIdentity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.rpa.auth.core.entity.*;
import com.iflytek.rpa.auth.core.entity.enums.LoginTypeEnum;
import com.iflytek.rpa.auth.core.service.TenantService;
import com.iflytek.rpa.auth.exception.ServiceException;
import com.iflytek.rpa.auth.idp.AuthenticationService;
import com.iflytek.rpa.auth.sp.casdoor.entity.CasdoorLoginDto;
import com.iflytek.rpa.auth.sp.casdoor.entity.CasdoorLoginResult;
import com.iflytek.rpa.auth.sp.casdoor.entity.CasdoorSignupDto;
import com.iflytek.rpa.auth.sp.casdoor.mapper.CasdoorUserMapper;
import com.iflytek.rpa.auth.sp.casdoor.service.extend.CasdoorLoginExtendService;
import com.iflytek.rpa.auth.sp.casdoor.service.extend.CasdoorUserExtendService;
import com.iflytek.rpa.auth.utils.AppResponse;
import com.iflytek.rpa.auth.utils.RedisUtils;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "casdoor")
public class CasdoorAuthenticationServiceImpl implements AuthenticationService {

    private static final String TEMP_TOKEN_PREFIX = "auth:temp_token:";
    private static final int TEMP_TOKEN_EXPIRE_SECONDS = 600;

    @Value("${casdoor.endpoint}")
    private String endpoint;

    @Value("${casdoor.client-id}")
    private String clientId;

    @Value("${casdoor.client-secret}")
    private String clientSecret;

    @Value("${casdoor.organization-name}")
    private String organizationName;

    @Value("${casdoor.application-name}")
    private String applicationName;

    private final ObjectMapper objectMapper;
    private final CasdoorUserExtendService casdoorUserExtendService;
    private final CasdoorLoginExtendService casdoorLoginExtendService;
    private final CasdoorUserMapper casdoorUserMapper;
    private final TenantService tenantService;

    @Override
    public String preAuthenticate(LoginDto loginDto, HttpServletRequest request) {
        if (loginDto == null) {
            throw new ServiceException("로그인매개변수비워 둘 수 없습니다");
        }

        String phone = loginDto.getPhone();
        String loginName = loginDto.getLoginName();

        if (!StringUtils.hasText(loginName) && !StringUtils.hasText(phone)) {
            throw new ServiceException("사용자명또는휴대폰 번호는 비워 둘 수 없습니다");
        }
        if (!StringUtils.hasText(loginDto.getPassword())) {
            throw new ServiceException("비밀번호는 비워 둘 수 없습니다");
        }

        try {
            log.info("Casdoor 인증열기 , 사용자명: {}, 휴대폰 번호: {}", loginName, phone);

            org.casbin.casdoor.entity.User casdoorUser;
            if (StringUtils.hasText(phone)) {
                casdoorUser = casdoorUserExtendService.getUserByPhone(phone);
            } else {
                casdoorUser = casdoorUserExtendService.getUser(loginName);
            }

            if (casdoorUser == null || !StringUtils.hasText(casdoorUser.name)) {
                log.warn("Casdoor 인증 실패: 계정찾을 수 없습니다, 사용자명: {}, 휴대폰 번호: {}", loginName, phone);
                throw new ServiceException("계정찾을 수 없습니다, 요청 회원가입");
            }

            // 결과가완료휴대폰 번호, 사용casdoor사용자의name사용자명
            if (!StringUtils.hasText(loginName)) {
                loginName = casdoorUser.name;
                loginDto.setLoginName(loginName);
            }
            // 휴대폰 번호정보 후프로세스사용
            if (!StringUtils.hasText(phone) && StringUtils.hasText(casdoorUser.phone)) {
                phone = casdoorUser.phone;
                loginDto.setPhone(phone);
            }

            casdoorUser.password = loginDto.getPassword();
            boolean passwordValid = casdoorUserExtendService.checkUserPassword(casdoorUser);
            if (!passwordValid) {
                log.warn("Casdoor 인증 실패: 비밀번호오류, 사용자명: {}, 휴대폰 번호: {}", loginName, phone);
                throw new ServiceException("계정또는비밀번호오류");
            }

            String tempToken = UUID.randomUUID().toString().replace("-", "");
            String cacheKey = TEMP_TOKEN_PREFIX + tempToken;

            // 저장사용자로그인정보, 후정상방식로그인시가져오기출력
            RedisUtils.set(cacheKey, objectMapper.writeValueAsString(loginDto), TEMP_TOKEN_EXPIRE_SECONDS);

            log.info("Casdoor 인증 성공, 사용자명: {}, 휴대폰 번호: {}, 시인증완료완료", loginName, phone);
            return tempToken;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Casdoor 인증예외, 매개변수: {}", loginDto, e);
            throw new ServiceException("Casdoor 인증예외: " + e.getMessage());
        }
    }

    @Override
    public User loginWithTenant(String tempToken, String tenantId, HttpServletRequest servletRequest) {
        if (!StringUtils.hasText(tempToken)) {
            throw new ServiceException("시인증비워 둘 수 없습니다");
        }
        if (!StringUtils.hasText(tenantId)) {
            throw new ServiceException("테넌트 ID는 비워 둘 수 없습니다");
        }

        try {
            log.info("열기 Casdoor정상방식로그인, 시인증: {}, 테넌트ID: {}", tempToken, tenantId);

            // 1. 에서저장중가져오기사용자로그인정보
            LoginDto loginDto = getLoginInfoByTempToken(tempToken);
            if (loginDto == null) {
                throw new ServiceException("시인증완료경과또는없음");
            }

            // 2. 생성Casdoor로그인요청 
            CasdoorLoginDto casdoorLoginDto = new CasdoorLoginDto();
            casdoorLoginDto.setApplication(applicationName);
            casdoorLoginDto.setOrganization(organizationName);
            casdoorLoginDto.setUsername(loginDto.getLoginName());
            casdoorLoginDto.setPassword(loginDto.getPassword());
            casdoorLoginDto.setType("login");
            casdoorLoginDto.setSigninMethod("Password");

            // 3. 호출Casdoor로그인연결, 가져오기사용자ID및session cookie
            CasdoorLoginResult loginResult = casdoorLoginExtendService.login(casdoorLoginDto);
            if (loginResult == null || !StringUtils.hasText(loginResult.getUserId())) {
                throw new ServiceException("Casdoor로그인실패: 가져올 수 없는 사용자ID");
            }

            String userIdForCasdoor = loginResult.getUserId();
            String casdoorSessionId = loginResult.getSession();
            log.info("Casdoor로그인성공, 사용자ID: {}, Session ID: {}", userIdForCasdoor, casdoorSessionId);

            // 4. 통신경과사용자이름가져오기사용자정보
            String[] split = userIdForCasdoor.split("/");
            String name = split.length > 1 ? split[1] : "";
            org.casbin.casdoor.entity.User casdoorUser = casdoorUserExtendService.getUser(name);
            if (casdoorUser == null) {
                throw new ServiceException("가져오기사용자 정보실패: 사용자를 찾을 수 없습니다");
            }

            // 5. 복사사용Casdoor반환의session, 를사용자 정보 까지session중
            HttpSession session = servletRequest.getSession(true);
            session.setAttribute("user", casdoorUser);
            session.setAttribute("tenantId", tenantId);
            // 저장Casdoor의session ID, 으로후및Casdoor API시사용
            if (StringUtils.hasText(casdoorSessionId)) {
                session.setAttribute("casdoor_session_id", casdoorSessionId);
                log.info("Casdoor Session ID완료저장까지사용session중");
            }
            log.info("사용자 정보완료까지session, 사용자ID: {}, 테넌트ID: {}", userIdForCasdoor, tenantId);

            // 6. 삭제시인증
            String cacheKey = TEMP_TOKEN_PREFIX + tempToken;
            RedisUtils.del(cacheKey);

            // 7. 변환로통신사용User객체반환
            User commonUser = casdoorUserMapper.toCommonUser(casdoorUser);
            log.info("Casdoor정상방식로그인성공, 사용자ID: {}, 테넌트ID: {}", userIdForCasdoor, tenantId);
            return commonUser;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Casdoor정상방식로그인예외, 시인증: {}, 테넌트ID: {}", tempToken, tenantId, e);
            throw new ServiceException("Casdoor정상방식로그인예외: " + e.getMessage());
        }
    }

    @Override
    public User login(LoginDto loginDto, HttpServletRequest servletRequest) {
        return null;
    }

    @Override
    public String getPhoneByTempToken(String tempToken) {
        return "";
    }

    @Override
    public LoginDto getLoginInfoByTempToken(String tempToken) {
        if (!StringUtils.hasText(tempToken)) {
            throw new ServiceException("시인증비워 둘 수 없습니다");
        }
        try {
            String cacheKey = TEMP_TOKEN_PREFIX + tempToken;
            Object cachedUserInfo = RedisUtils.get(cacheKey);
            if (cachedUserInfo == null) {
                throw new ServiceException("시인증완료경과또는없음");
            }
            LoginDto loginDto = objectMapper.readValue(cachedUserInfo.toString(), LoginDto.class);
            return loginDto;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("가져오기로그인정보예외, 시인증: {}", tempToken, e);
            throw new ServiceException("가져오기로그인정보예외: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<List<Tenant>> getTenantList(String tempToken, HttpServletRequest request) {
        try {
            log.info("가져오기테넌트목록, 시인증: {}", tempToken);

            // 에서시인증중가져오기LoginDto
            //            LoginDto loginDto = getLoginInfoByTempToken(tempToken);

            return tenantService.getTenantList(organizationName, request);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("가져오기테넌트목록실패, 시인증: {}", tempToken, e);
            throw new ServiceException("가져오기테넌트목록실패: " + e.getMessage());
        }
    }

    @Override
    public String register(RegisterDto registerDto, HttpServletRequest request) {
        if (registerDto == null) {
            throw new ServiceException("회원가입매개변수비워 둘 수 없습니다");
        }

        if (!StringUtils.hasText(registerDto.getPassword())) {
            throw new ServiceException("비밀번호는 비워 둘 수 없습니다");
        }

        // 인증비밀번호여부일
        if (!registerDto.getPassword().equals(registerDto.getConfirmPassword())) {
            throw new ServiceException("입력한 비밀번호가 올바르지 않습니다");
        }

        try {
            log.info("열기 Casdoor회원가입, 휴대폰 번호: {}, 사용자명: {}", registerDto.getPhone(), registerDto.getLoginName());

            // 1. 생성Casdoor회원가입요청 
            CasdoorSignupDto signupDto = new CasdoorSignupDto();
            signupDto.setApplication(applicationName);
            signupDto.setOrganization(organizationName);
            // 결과가있음로그인이름, 사용휴대폰 번호로로그인이름
            String username = StringUtils.hasText(registerDto.getLoginName())
                    ? registerDto.getLoginName()
                    : registerDto.getPhone();
            signupDto.setUsername(username);
            signupDto.setName(username); // name필드사용사용자명
            signupDto.setPassword(registerDto.getPassword());
            if (!StringUtils.isEmpty(registerDto.getPhone())) {
                signupDto.setPhone(registerDto.getPhone());
                // 휴대폰 번호로CN
                signupDto.setCountryCode("CN");
            }

            // 2. 호출Casdoor회원가입연결
            CasdoorLoginResult signupResult = casdoorLoginExtendService.signup(signupDto);
            if (signupResult == null || !StringUtils.hasText(signupResult.getUserId())) {
                throw new ServiceException("Casdoor회원가입실패: 가져올 수 없는 사용자ID");
            }

            // 3. 완료시인증저장회원가입정보
            String tempToken = UUID.randomUUID().toString().replace("-", "");
            String cacheKey = TEMP_TOKEN_PREFIX + tempToken;

            // 에서회원가입정보중가져오기로그인정보, 후를로그인정보및테넌트ID저장, 사용후입력.(열기 버전테넌트있음일개)
            LoginDto loginDto = new LoginDto();
            loginDto.setLoginName(registerDto.getLoginName());
            loginDto.setPassword(registerDto.getPassword());
            loginDto.setLoginType(LoginTypeEnum.PASSWORD);
            loginDto.setTenantId(organizationName);
            loginDto.setScene("login");
            loginDto.setPlatform("client");

            RedisUtils.set(cacheKey, objectMapper.writeValueAsString(loginDto), TEMP_TOKEN_EXPIRE_SECONDS);

            String userId = signupResult.getUserId();
            log.info("Casdoor회원가입성공, 사용자ID: {}, 휴대폰 번호: {}, 테넌트ID: {}", userId, registerDto.getPhone(), organizationName);
            return tempToken;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Casdoor회원가입예외, 휴대폰 번호: {}", registerDto.getPhone(), e);
            throw new ServiceException("Casdoor회원가입예외: " + e.getMessage());
        }
    }

    @Override
    public User setPasswordAndLogin(String tempToken, String password, String tenantId, HttpServletRequest request) {
        return null;
    }

    @Override
    public boolean queryUserExist(String loginName) {
        if (!StringUtils.hasText(loginName)) {
            return false;
        }

        try {
            log.debug("조회사용자여부저장에서, 로그인이름: {}", loginName);

            // 호출getUser조회사용자여부저장에서
            org.casbin.casdoor.entity.User user = casdoorUserExtendService.getUser(loginName);
            boolean exists = user != null && StringUtils.hasText(user.name);

            log.debug("사용자저장된 조회결과, 로그인이름: {}, 저장에서: {}", loginName, exists);
            return exists;

        } catch (Exception e) {
            log.warn("조회사용자여부저장에서예외, 로그인이름: {}, 예외: {}", loginName, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean setPassword(String tempToken, String password, String tenantId, HttpServletRequest request) {
        return false;
    }

    @Override
    public AppResponse<String> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            log.info("열기 Casdoor로그아웃");

            // 1. 에서session중가져오기Casdoor session ID
            HttpSession session = request.getSession(false);
            String casdoorSessionId = null;
            if (session != null) {
                casdoorSessionId = (String) session.getAttribute("casdoor_session_id");
                if (StringUtils.hasText(casdoorSessionId)) {
                    log.info("에서session중가져오기까지Casdoor Session ID");
                }
            }
            //            //1.1 가져오기사용자의access token
            //            String accessToken = null;
            //            if (session != null) {
            //                org.casbin.casdoor.entity.User user = (org.casbin.casdoor.entity.User)
            // session.getAttribute("user");
            //                if (user != null && StringUtils.hasText(user.name)) {
            //                    accessToken = TokenManager.getAccessToken(user.name);
            //                    if (StringUtils.hasText(accessToken)) {
            //                        log.info("에서TokenManager가져오기까지사용자access token, username: {}", user.name);
            //                        // 지우기Redis중의token
            //                        TokenManager.clearTokens(user.name);
            //                    } else {
            //                        log.warn("가져오기사용자access token비어 있습니다, 가능token완료경과또는찾을 수 없습니다, username: {}", user.name);
            //                    }
            //                }
            //            }

            // 2. 호출Casdoor로그아웃연결
            if (StringUtils.hasText(casdoorSessionId)) {
                try {
                    casdoorLoginExtendService.logout(casdoorSessionId);
                    log.info("Casdoor로그아웃연결호출성공");
                } catch (Exception e) {
                    log.warn("호출Casdoor로그아웃연결실패, 계속지우기본session: {}", e.getMessage());
                }
            } else {
                log.warn("찾을 수 없는 Casdoor Session ID, 건너뛰기Casdoor로그아웃연결호출");
            }

            // 3. 지우기사용의session
            if (session != null) {
                session.invalidate();
                log.info("사용session완료지우기");
            }

            log.info("Casdoor로그아웃성공");
            return AppResponse.success("로그아웃성공");

        } catch (Exception e) {
            log.error("Casdoor로그아웃예외", e);
            // 출력오류 시도지우기session
            try {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
            } catch (Exception ex) {
                log.error("지우기session실패", ex);
            }
            return AppResponse.error("로그아웃예외: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<Boolean> refreshToken(HttpServletRequest request, String accessToken) {
        return null;
    }

    @Override
    public String getVerificationCode(String phone, String scene) {
        return "";
    }

    @Override
    public String getVerificationCode(String phone) {
        return getVerificationCode(phone, "login");
    }

    @Override
    public AppResponse<Boolean> checkSession(HttpServletRequest request, HttpServletResponse response) {
        // 에서session중가져오기사용자 정보
        HttpSession session = request.getSession(false);
        if (Objects.isNull(session)) {
            return AppResponse.success(false);
        }

        org.casbin.casdoor.entity.User user = (org.casbin.casdoor.entity.User) session.getAttribute("user");
        if (Objects.isNull(user)) {
            return AppResponse.success(false);
        }

        return AppResponse.success(true);
    }

    @Override
    public boolean checkLoginStatus(HttpServletRequest request) {
        try {
            // 에서session중가져오기사용자 정보
            HttpSession session = request.getSession(false);
            if (session == null) {
                log.debug("조회로그인상태: session찾을 수 없습니다");
                return false;
            }

            org.casbin.casdoor.entity.User user = (org.casbin.casdoor.entity.User) session.getAttribute("user");
            boolean isLoggedIn = user != null && StringUtils.hasText(user.name);

            log.debug("조회로그인상태: {}", isLoggedIn);
            return isLoggedIn;

        } catch (Exception e) {
            log.warn("조회로그인상태예외: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public AppResponse<String> changePassword(ChangePasswordDto changePasswordDto) {
        return null;
    }

    @Override
    public AppResponse<String> addUser(AddUserDto user, HttpServletRequest request) {
        return null;
    }
}