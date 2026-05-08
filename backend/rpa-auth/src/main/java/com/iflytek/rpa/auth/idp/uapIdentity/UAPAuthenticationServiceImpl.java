package com.iflytek.rpa.auth.idp.uapIdentity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.rpa.auth.core.entity.*;
import com.iflytek.rpa.auth.core.service.TenantService;
import com.iflytek.rpa.auth.idp.AuthenticationService;
import com.iflytek.rpa.auth.sp.uap.constants.RedisKeyConstant;
import com.iflytek.rpa.auth.sp.uap.constants.UAPConstant;
import com.iflytek.rpa.auth.sp.uap.dao.UserDao;
import com.iflytek.rpa.auth.sp.uap.mapper.TenantMapper;
import com.iflytek.rpa.auth.sp.uap.mapper.UserMapper;
import com.iflytek.rpa.auth.sp.uap.service.impl.UserServiceImpl;
import com.iflytek.rpa.auth.utils.AppResponse;
import com.iflytek.rpa.auth.utils.ErrorCodeEnum;
import com.iflytek.rpa.auth.utils.MenuPermissionValidator;
import com.iflytek.rpa.auth.utils.RedisUtils;
import com.iflytek.rpa.auth.utils.SmsUtils;
import com.iflytek.sec.uap.base.util.ClientConfigUtil;
import com.iflytek.sec.uap.client.api.ClientAuthenticationAPI;
import com.iflytek.sec.uap.client.api.UapUserInfoAPI;
import com.iflytek.sec.uap.client.core.dto.ResponseDto;
import com.iflytek.sec.uap.client.core.dto.authentication.LoginResultDto;
import com.iflytek.sec.uap.client.core.dto.authentication.UapLoginByPasswordDto;
import com.iflytek.sec.uap.client.core.dto.pwd.UpdatePwdDto;
import com.iflytek.sec.uap.client.core.dto.tenant.UapTenant;
import com.iflytek.sec.uap.client.core.dto.user.UapUser;
import com.iflytek.sec.uap.client.util.CommonValidateUtil;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * UAP인증 서비스
 * 사용있음모듈, 있음의SSO시, 사용내부모듈UAP행인증
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "private-uap")
public class UAPAuthenticationServiceImpl implements AuthenticationService {

    // 시인증저장전및경과시간(10분)
    private static final String TEMP_TOKEN_PREFIX = "auth:temp_token:";
    private static final int TEMP_TOKEN_EXPIRE_SECONDS = 600;

    // 인증 코드저장전및경과시간(10분)
    private static final String VERIFY_CODE_PREFIX = "auth:verify_code:";
    private static final int VERIFY_CODE_EXPIRE_SECONDS = 600;
    private final UserServiceImpl userServiceImpl;
    private final ObjectMapper objectMapper;
    private final TenantMapper tenantMapper;
    private final UserMapper userMapper;
    private final SmsUtils smsUtils;
    private final TenantService tenantService;

    private final UserDao userDao;

    @Value("${uap.database.name:uap_db}")
    private String databaseName;

    /**
     * 생성UAP로그인요청 매개변수
     */
    private UapLoginByPasswordDto buildUapLoginByPasswordDto(LoginDto loginDto, String tenantId) {
        UapLoginByPasswordDto uapLoginByPasswordDto = new UapLoginByPasswordDto();
        uapLoginByPasswordDto.setAppCode(ClientConfigUtil.instance().getAppCode());
        uapLoginByPasswordDto.setService(ClientConfigUtil.instance().getRestServerUrl());
        uapLoginByPasswordDto.setRedirect(ClientConfigUtil.instance().getCasClientContext());
        uapLoginByPasswordDto.setLoginName(loginDto.getLoginName());
        uapLoginByPasswordDto.setPassword(loginDto.getPassword());
        uapLoginByPasswordDto.setReferer(ClientConfigUtil.instance().getRestServerUrl());
        if (StringUtils.hasText(tenantId)) {
            uapLoginByPasswordDto.setTenantId(tenantId);
        }
        return uapLoginByPasswordDto;
    }

    /**
     * 에서시인증중가져오기LoginDto
     */
    private LoginDto getLoginDtoByTempToken(String tempToken) {
        if (!StringUtils.hasText(tempToken)) {
            return null;
        }

        try {
            String cacheKey = TEMP_TOKEN_PREFIX + tempToken;
            Object cachedData = RedisUtils.get(cacheKey);

            if (cachedData == null) {
                log.warn("시인증완료경과또는없음, 시인증: {}", tempToken);
                return null;
            }

            Map<String, Object> dataMap = objectMapper.readValue(
                    cachedData.toString(),
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));

            if (dataMap.containsKey("loginDto")) {
                LoginDto loginDto = objectMapper.convertValue(dataMap.get("loginDto"), LoginDto.class);
                if (loginDto != null) {
                    return loginDto;
                }
            }

            log.warn("시인증중찾을 수 없는 로그인정보, 시인증: {}", tempToken);
            return null;

        } catch (Exception e) {
            log.error("가져오기로그인정보예외, 시인증: {}", tempToken, e);
            return null;
        }
    }

    @Override
    public String preAuthenticate(LoginDto loginDto, HttpServletRequest request) {
        try {
            // 예비밀번호로그인예인증 코드 로그인
            boolean isCodeLogin =
                    StringUtils.hasText(loginDto.getCaptcha()) && StringUtils.hasText(loginDto.getPhone());
            String phone = loginDto.getPhone();
            String loginName = StringUtils.hasText(loginDto.getLoginName()) ? loginDto.getLoginName() : phone;

            // 결과가프론트엔드사용휴대폰 번호로로그인이름, 이면근거휴대폰 번호조회의로그인이름
            if (StringUtils.hasText(phone) && phone.equals(loginName)) {
                String loginNameByPhone = userDao.queryLoginNameByPhone(phone, databaseName);
                if (StringUtils.hasText(loginNameByPhone)) {
                    loginName = loginNameByPhone;
                }
            }
            // 를종료의로그인이름돌아가기, 확인후프로세스사용일의로그인이름
            loginDto.setLoginName(loginName);
            String scene = resolveScene(loginDto.getScene(), AuthenticationService.SCENE_LOGIN);

            if (isCodeLogin) {
                log.info("UAP 인증 시작(인증 코드 로그인), 휴대폰 번호: {}", loginDto.getPhone());

                // 인증 코드 로그인: 인증 코드를 검증합니다.
                if (!verifyCode(loginDto.getPhone(), loginDto.getCaptcha(), scene)) {
                    throw new RuntimeException("인증 코드가 올바르지 않거나 만료되었습니다");
                }

                // 인증 코드 검증 후 휴대폰 번호로 로그인 이름을 보완합니다.
                if (!StringUtils.hasText(loginDto.getLoginName())) {
                    loginDto.setLoginName(loginDto.getPhone());
                }

                log.info("인증 코드 검증 완료, 휴대폰 번호: {}, 로그인 이름: {}", loginDto.getPhone(), loginDto.getLoginName());
            } else {
                log.info("UAP 인증 시작(비밀번호 로그인), 로그인 이름: {}", loginName);

                // 비밀번호 로그인: 계정 비밀번호를 검증합니다.
                if (!StringUtils.hasText(loginDto.getPassword())) {
                    throw new RuntimeException("비밀번호는 비워 둘 수 없습니다");
                }

                // 1. 테넌트 목록을 조회하고 필요하면 tenantId를 지정합니다.
                List<UapTenant> tenantList = ClientAuthenticationAPI.getTenantListInAppByLoginName(loginName);
                String tenantId = null;

                if (CollectionUtils.isEmpty(tenantList)) {
                    // 테넌트 목록이 비어 있으면 계정 상태가 올바르지 않습니다.
                    log.error("계정 오류: 로그인 이름으로 테넌트 정보를 찾을 수 없습니다. 로그인 이름: {}", loginName);
                    throw new RuntimeException("계정 상태가 올바르지 않습니다. 시스템 관리자에게 문의하세요");
                } else if (tenantList.size() == 1) {
                    // 테넌트가 하나이면 해당 테넌트 ID를 사용합니다.
                    tenantId = tenantList.get(0).getId();
                    log.info("단일 테넌트 조회 완료, 로그인 이름: {}, 테넌트 ID: {}", loginName, tenantId);
                } else {
                    // 테넌트가 여러 개이면 tenantId 없이 로그인 요청을 진행합니다.
                    log.info("여러 테넌트 조회 완료, 로그인 이름: {}, 테넌트 수: {}", loginName, tenantList.size());
                }

                // 2. UAP 로그인 요청 매개변수를 생성합니다.
                UapLoginByPasswordDto uapLoginByPasswordDto = buildUapLoginByPasswordDto(loginDto, tenantId);

                // 3. UAP 로그인 API를 호출해 계정 비밀번호를 검증합니다.
                ResponseDto<LoginResultDto> uapLoginByPasswordResponse =
                        ClientAuthenticationAPI.loginUapByPassword(uapLoginByPasswordDto);
                log.info(
                        "UAP 로그인: flag={}, message={}",
                        uapLoginByPasswordResponse != null ? uapLoginByPasswordResponse.isFlag() : false,
                        uapLoginByPasswordResponse != null ? uapLoginByPasswordResponse.getMessage() : "비어 있습니다");

                // 4. 로그인 결과를 확인합니다.
                if (uapLoginByPasswordResponse == null || !uapLoginByPasswordResponse.isFlag()) {
                    String errorMsg =
                            uapLoginByPasswordResponse != null ? uapLoginByPasswordResponse.getMessage() : "UAP 로그인 응답이 비어 있습니다";
                    log.error("UAP 로그인 실패: {}", errorMsg);
                    throw new RuntimeException("로그인 실패: " + errorMsg);
                }

                log.info("계정 비밀번호 검증 완료, 로그인 이름: {}", loginName);
            }

            // 4. 임시 인증 토큰을 생성합니다.
            String tempToken = UUID.randomUUID().toString().replace("-", "");
            String cacheKey = TEMP_TOKEN_PREFIX + tempToken;

            // 5. 저장할 데이터를 구성합니다.
            Map<String, Object> cacheData = new HashMap<>();
            cacheData.put("loginDto", loginDto); // 로그인 정보를 저장합니다. 인증 코드 로그인에서는 비밀번호가 비어 있습니다.

            // 6. 데이터를 JSON으로 직렬화한 뒤 Redis에 저장합니다.
            String cacheDataJson = objectMapper.writeValueAsString(cacheData);
            RedisUtils.set(cacheKey, cacheDataJson, TEMP_TOKEN_EXPIRE_SECONDS);

            log.info("일인증 성공, 로그인이름: {}, 시인증완료완료, 경과시간: {}초", loginName, TEMP_TOKEN_EXPIRE_SECONDS);

            return tempToken;

        } catch (Exception e) {
            log.error("일인증예외, 로그인이름: {}", loginDto != null ? loginDto.getLoginName() : "지원하지 않는", e);
            throw new RuntimeException("UAP인증 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public User loginWithTenant(String tempToken, String tenantId, HttpServletRequest servletRequest) {
        try {
            log.info("삼: UAP정상방식로그인, 시인증: {}, 테넌트ID: {}", tempToken, tenantId);

            // 1. 인증매개변수
            if (!StringUtils.hasText(tempToken)) {
                throw new RuntimeException("시인증비워 둘 수 없습니다");
            }
            if (!StringUtils.hasText(tenantId)) {
                throw new RuntimeException("테넌트 ID는 비워 둘 수 없습니다");
            }

            // 2. 에서시인증중가져오기로그인정보
            LoginDto loginDto = getLoginDtoByTempToken(tempToken);
            if (loginDto == null) {
                throw new RuntimeException("시인증완료경과또는없음");
            }
            loginDto.setTenantId(tenantId);

            // 에서저장중가져오기platform, 결과가비어 있습니다이면사용client
            String platform = loginDto.getPlatform();
            if (!StringUtils.hasText(platform)) {
                platform = (String) servletRequest.getSession().getAttribute(UAPConstant.SESSION_KEY_PLATFORM);
                if (!StringUtils.hasText(platform)) {
                    platform = "client";
                }
            }
            log.info("에서저장가져오기로그인정보, 로그인이름: {}, 휴대폰 번호: {}, 평면: {}", loginDto.getLoginName(), loginDto.getPhone(), platform);

            // 3. 예비밀번호로그인예인증 코드 로그인(없음비밀번호로그인)
            UapUser uapUser;
            if (StringUtils.hasText(loginDto.getPassword())) {
                // 비밀번호로그인
                uapUser = userServiceImpl.loginUapByPasswordWithTenant(
                        loginDto.getLoginName(), loginDto.getPassword(), tenantId, servletRequest);
            } else if (StringUtils.hasText(loginDto.getPhone())) {
                // 인증 코드 로그인(없음비밀번호로그인)
                log.info("사용인증 코드 로그인(없음비밀번호), 휴대폰 번호: {}", loginDto.getPhone());
                AppResponse<UapUser> loginResponse =
                        userServiceImpl.loginNoPasswordByPhone(loginDto.getPhone(), tenantId, servletRequest);

                if (loginResponse == null || !loginResponse.ok() || loginResponse.getData() == null) {
                    String errorMsg = loginResponse != null ? loginResponse.getMessage() : "없음비밀번호로그인비어 있습니다";
                    log.error("없음비밀번호로그인실패: {}", errorMsg);
                    throw new RuntimeException("UAP로그인실패: " + errorMsg);
                }

                uapUser = loginResponse.getData();
            } else {
                throw new RuntimeException("로그인 정보가 올바르지 않습니다. 비밀번호 또는 휴대폰 번호를 확인하세요");
            }

            if (uapUser == null) {
                throw new RuntimeException("UAP로그인실패: 반환되지 않았습니다사용자 정보");
            }

            // 4. 를platform저장까지session
            if (StringUtils.hasText(platform)) {
                servletRequest.getSession().setAttribute(UAPConstant.SESSION_KEY_PLATFORM, platform);
                log.debug("완료저장로그인평면까지session, 평면: {}", platform);
            }

            // 5. 관리단일로그인: 있음클라이언트로그인실행단일로그인
            if (UAPConstant.PLATFORM_CLIENT.equals(platform)) {
                handleSingleSignOn(uapUser.getId(), servletRequest);
            } else {
                log.debug("클라이언트로그인(platform: {}), 건너뛰기단일로그인관리, 사용자ID: {}", platform, uapUser.getId());
            }

            // 6. 삭제시인증
            String cacheKey = TEMP_TOKEN_PREFIX + tempToken;
            RedisUtils.del(cacheKey);
            log.info("삭제됨시인증, cacheKey: {}", cacheKey);

            // 6. 변환로서비스반환
            User user = userMapper.fromUapUser(uapUser);

            log.info("삼정상방식로그인성공, 사용자ID: {}, 로그인이름: {}, 테넌트ID: {}", uapUser.getId(), uapUser.getLoginName(), tenantId);

            return user;

        } catch (Exception e) {
            log.error("삼정상방식로그인예외, 시인증: {}, 테넌트ID: {}", tempToken, tenantId, e);
            throw new RuntimeException("UAP정상방식로그인실패: " + e.getMessage(), e);
        }
    }

    @Override
    @Deprecated
    public User login(LoginDto loginDto, HttpServletRequest servletRequest) {
        throw new UnsupportedOperationException("기존 로그인 경로는 지원하지 않습니다. pre-authenticate와 login을 사용하세요");
    }

    @Override
    public LoginDto getLoginInfoByTempToken(String tempToken) {
        LoginDto loginDto = getLoginDtoByTempToken(tempToken);
        if (loginDto == null) {
            throw new RuntimeException("시인증완료경과또는없음");
        }
        return loginDto;
    }

    @Override
    public String getPhoneByTempToken(String tempToken) {
        try {
            LoginDto loginDto = getLoginDtoByTempToken(tempToken);
            if (loginDto == null) {
                throw new RuntimeException("시인증완료경과또는없음");
            }
            if (StringUtils.hasText(loginDto.getPhone())) {
                return loginDto.getPhone();
            }
            throw new RuntimeException("시인증중찾을 수 없는 휴대폰 번호정보");
        } catch (Exception e) {
            log.error("가져오기휴대폰 번호예외, 시인증: {}", tempToken, e);
            throw new RuntimeException("가져오기휴대폰 번호예외: " + e.getMessage(), e);
        }
    }

    @Override
    public AppResponse<List<Tenant>> getTenantList(String tempToken, HttpServletRequest request) {
        try {
            log.info("가져오기테넌트목록, 시인증: {}", tempToken);

            String phone = null;
            String platform = null;

            // 에서시인증중가져오기LoginDto, 패키지platform정보
            LoginDto loginDto = getLoginDtoByTempToken(tempToken);

            if (loginDto != null) {
                phone = loginDto.getPhone();
                platform = loginDto.getPlatform();
            }

            // 결과가platform비어 있습니다, 사용client
            if (!StringUtils.hasText(platform)) {
                platform = (String) request.getSession().getAttribute(UAPConstant.SESSION_KEY_PLATFORM);
                if (!StringUtils.hasText(platform)) {
                    platform = "client";
                }
            }

            log.info("에서시인증가져오기휴대폰 번호: {}, 평면: {}", phone, platform);

            // 호출테넌트서비스가져오기테넌트목록
            AppResponse<List<Tenant>> response = tenantService.getTenantList(phone, request);

            if (!response.ok() || response.getData() == null) {
                return response;
            }

            List<Tenant> tenantList = response.getData();

            // 근거platform필터링테넌트목록
            // 결과가platform예admin, 필터링개사람테넌트(tenantCode으로PERSONAL_TENANT_CODE열기 의)
            if (UAPConstant.PLATFORM_ADMIN.equals(platform)) {
                tenantList = tenantList.stream()
                        .filter(tenant -> {
                            if (tenant == null || tenant.getTenantCode() == null) {
                                return false;
                            }
                            // 필터링개사람테넌트
                            return !tenant.getTenantCode().startsWith(UAPConstant.PERSONAL_TENANT_CODE);
                        })
                        .collect(java.util.stream.Collectors.toList());
                log.info("평면로admin, 완료필터링개사람테넌트, 테넌트수: {}", tenantList.size());
            }
            // 결과가platform예client또는invite, 반환전체테넌트목록(아니요필요필터링)

            return AppResponse.success(tenantList);

        } catch (Exception e) {
            log.error("가져오기테넌트목록실패, 시인증: {}", tempToken, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기테넌트목록실패: " + e.getMessage());
        }
    }

    @Override
    public String register(RegisterDto registerDto, HttpServletRequest request) {
        // UAP private mode manages registration outside this login adapter.
        throw new UnsupportedOperationException("UAP회원가입공가능대기");
    }

    @Override
    public User setPasswordAndLogin(String tempToken, String password, String tenantId, HttpServletRequest request) {
        // UAP private mode does not create a password during this flow.
        throw new UnsupportedOperationException("UAP비밀번호공가능대기");
    }

    @Override
    public boolean setPassword(String tempToken, String password, String tenantId, HttpServletRequest request) {
        // SSO지원하지 않음비밀번호
        throw new UnsupportedOperationException("UAP비밀번호공가능대기");
    }

    @Override
    public boolean queryUserExist(String loginName) {
        String loginNameByPhone = userDao.queryLoginNameByPhone(loginName, databaseName);
        return StringUtils.hasText(loginNameByPhone);
    }

    @Override
    public AppResponse<String> logout(HttpServletRequest request, HttpServletResponse response) {
        return userServiceImpl.logout(request, response);
    }

    @Override
    public AppResponse<Boolean> refreshToken(HttpServletRequest request, String accessToken) {
        if (checkLoginStatus(request)) {
            return AppResponse.success(true);
        }
        return AppResponse.error(ErrorCodeEnum.E_NOT_LOGIN, "로그인되지 않았습니다");
    }

    /**
     * 가져오기인증 코드
     * 완료6위치인증 코드, 저장까지Redis, 전송짧음정보
     *
     * @param phone 휴대폰 번호
     * @return 전송결과
     */
    @Override
    public String getVerificationCode(String phone, String scene) {
        try {
            if (!StringUtils.hasText(phone)) {
                throw new RuntimeException("휴대폰 번호는 비워 둘 수 없습니다");
            }

            log.info("열기완료인증 코드, 휴대폰 번호: {}", phone);

            // 1. 완료6위치기기인증 코드
            String code = String.format("%06d", (int) (Math.random() * 1000000));

            // 2. 저장까지Redis(직선연결저장인증 코드문자열)
            String cacheKey = buildVerifyCodeKey(phone, scene);
            RedisUtils.set(cacheKey, code, VERIFY_CODE_EXPIRE_SECONDS);

            log.info("인증 코드완료완료저장까지Redis, 휴대폰 번호: {}, 인증 코드: {}, : {}, 경과시간: {}초", phone, code, scene, VERIFY_CODE_EXPIRE_SECONDS);

            // 4. 생성짧음정보 매개변수
            Map<String, Object> tpMap = new HashMap<>();
            tpMap.put("code", code);
            tpMap.put("time", VERIFY_CODE_EXPIRE_SECONDS);

            // 5. 전송짧음정보
            AppResponse<?> smsResponse = smsUtils.sendSms(phone, smsUtils.tid, tpMap);
            if (smsResponse == null || !smsResponse.ok()) {
                log.error("인증 코드 전송짧음정보실패, 휴대폰 번호: {}, : {}", phone, smsResponse);
                throw new RuntimeException("인증 코드 전송짧음정보실패");
            }

            log.info("인증 코드 전송 성공, 휴대폰 번호: {}", phone);
            return "인증 코드완료전송";

        } catch (Exception e) {
            log.error("가져오기인증 코드예외, 휴대폰 번호: {}", phone, e);
            throw new RuntimeException("가져오기인증 코드예외: " + e.getMessage(), e);
        }
    }

    @Override
    public String getVerificationCode(String phone) {
        return getVerificationCode(phone, AuthenticationService.SCENE_LOGIN);
    }

    /**
     * 인증인증 코드
     *
     * @param phone 휴대폰 번호
     * @param code  인증 코드
     * @return 검증여부성공
     */
    public boolean verifyCode(String phone, String code, String scene) {
        if (!StringUtils.hasText(phone)) {
            throw new RuntimeException("휴대폰 번호는 비워 둘 수 없습니다");
        }
        if (!StringUtils.hasText(code)) {
            throw new RuntimeException("인증 코드는 비워 둘 수 없습니다");
        }

        try {
            String cacheKey = buildVerifyCodeKey(phone, scene);

            // 1. 조회Redis중여부저장에서인증 코드
            if (!RedisUtils.hasKey(cacheKey)) {
                log.warn("인증 코드찾을 수 없습니다또는완료경과, 휴대폰 번호: {}, : {}", phone, scene);
                return false;
            }

            // 2. 에서Redis중가져오기인증 코드
            Object storedCode = RedisUtils.get(cacheKey);
            if (storedCode == null) {
                log.warn("인증 코드데이터예외, 휴대폰 번호: {}, : {}", phone, scene);
                return false;
            }

            // 3. 인증 코드
            if (!code.equals(storedCode.toString())) {
                log.warn("인증 코드오류, 휴대폰 번호: {}, : {}", phone, scene);
                return false;
            }

            // 4. 인증 성공, 삭제Redis중의인증 코드(확인가능사용일)
            RedisUtils.del(cacheKey);
            log.info("인증 코드인증 성공삭제됨, 휴대폰 번호: {}, : {}", phone, scene);
            return true;

        } catch (Exception e) {
            log.error("인증인증 코드예외, 휴대폰 번호: {}", phone, e);
            throw new RuntimeException("인증인증 코드예외: " + e.getMessage(), e);
        }
    }

    private String buildVerifyCodeKey(String phone, String scene) {
        String normalizedScene = resolveScene(scene, AuthenticationService.SCENE_LOGIN);
        return VERIFY_CODE_PREFIX + normalizedScene + ":" + phone;
    }

    private String resolveScene(String scene, String defaultScene) {
        if (!StringUtils.hasText(scene)) {
            return defaultScene;
        }
        String normalized = scene.trim().toLowerCase();
        if (!AuthenticationService.SCENE_LOGIN.equals(normalized)
                && !AuthenticationService.SCENE_REGISTER.equals(normalized)
                && !AuthenticationService.SCENE_SET_PASSWORD.equals(normalized)) {
            return defaultScene;
        }
        return normalized;
    }

    @Override
    public AppResponse<Boolean> checkSession(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 1. 검증session여부있음(UAP의AuthenticationFilter완료검증완료)
            // 필요검증빈여부까지

            // 2. 에서session중가져오기platform, 있음클라이언트로그인검증단일로그인
            String platform = (String) request.getSession().getAttribute(UAPConstant.SESSION_KEY_PLATFORM);
            boolean isClient = UAPConstant.PLATFORM_CLIENT.equals(platform);

            if (isClient) {
                // 검증단일로그인: 조회현재sessionId여부및Redis중저장의일
                UapUser loginUser = UapUserInfoAPI.getLoginUser(request);
                if (loginUser != null && StringUtils.hasText(loginUser.getId())) {
                    String userId = loginUser.getId();
                    String currentSessionId = request.getSession().getId();

                    if (StringUtils.hasText(currentSessionId)) {
                        String redisKey = RedisKeyConstant.REDIS_KEY_USER_SESSION_PREFIX + userId;
                        Object storedSessionIdObj = RedisUtils.get(redisKey);

                        if (storedSessionIdObj != null && StringUtils.hasText(storedSessionIdObj.toString())) {
                            String storedSessionId = storedSessionIdObj.toString();

                            // 결과가현재sessionId및Redis중저장의아니요일, 설명에서방법로그인완료
                            if (!storedSessionId.equals(currentSessionId)) {
                                log.warn(
                                        "감지까지계정에서방법로그인, 현재sessionId: {}, 저장의sessionId: {}, 사용자ID: {}",
                                        currentSessionId,
                                        storedSessionId,
                                        userId);

                                // 지우기현재session, 강함제어출력로그인
                                try {
                                    logout(request, response);
                                } catch (Exception e) {
                                    log.error("지우기session실패", e);
                                }

                                return AppResponse.error(ErrorCodeEnum.E_NOT_LOGIN, "계정완료에서방법로그인, 현재완료실패");
                            }
                        }
                    }
                }
            } else {
                log.debug("클라이언트로그인(platform: {}), 건너뛰기단일로그인검증", platform);
            }

            // 3. 검증빈여부까지
            boolean spaceExpired = tenantService.checkSpaceExpired(request);
            if (spaceExpired) {
                String tenantId = UapUserInfoAPI.getTenantId(request);
                log.warn("빈완료까지, 테넌트ID: {}, 강함제어출력로그인", tenantId);
                // 지우기session, 강함제어출력로그인
                try {
                    logout(request, response);
                } catch (Exception e) {
                    log.error("지우기session실패", e);
                }
                return AppResponse.error(ErrorCodeEnum.E_SPACE_EXPIRED, "빈완료까지, 요청다시 로그인");
            }

            // 4. 결과가예admin평면, 검증메뉴권한
            boolean isAdmin = UAPConstant.PLATFORM_ADMIN.equals(platform);
            if (isAdmin) {
                AppResponse<Boolean> menuPermissionResult = MenuPermissionValidator.checkMenuPermission(request);
                if (!menuPermissionResult.ok()) {
                    // 메뉴권한인증 실패, 반환오류 
                    return menuPermissionResult;
                }
            }

            // 5. session있음, 단일로그인검증통신경과, 빈찾을 수 없는 메뉴권한인증통신경과, 반환성공
            return AppResponse.success(true);
        } catch (Exception e) {
            log.error("조회session실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회session실패: " + e.getMessage());
        }
    }

    @Override
    public boolean checkLoginStatus(HttpServletRequest request) {
        try {
            // 사용UAP의API조회로그인상태
            UapUser loginUser = UapUserInfoAPI.getLoginUser(request);
            boolean tokenCheckResult = CommonValidateUtil.casCheckToken(request);
            boolean isLoggedIn = loginUser != null && tokenCheckResult;

            if (isLoggedIn) {
                log.debug("조회로그인상태: 완료로그인, 사용자명: {}", loginUser.getLoginName());
            } else {
                log.debug("조회로그인상태: 로그인되지 않았습니다");
            }

            return isLoggedIn;
        } catch (Exception e) {
            log.warn("조회로그인상태예외: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public AppResponse<String> changePassword(ChangePasswordDto changePasswordDto) {
        try {
            log.info("수정비밀번호요청 , 계정: {}, 휴대폰 번호: {}", changePasswordDto.getLoginName(), changePasswordDto.getPhone());

            String phone = changePasswordDto.getPhone();
            String loginName = changePasswordDto.getLoginName();

            // 결과가loginName비어 있습니다또는loginName대기phone, 이면근거phone조회의loginName
            if (!StringUtils.hasText(loginName) || (StringUtils.hasText(phone) && phone.equals(loginName))) {
                if (StringUtils.hasText(phone)) {
                    String loginNameByPhone = userDao.queryLoginNameByPhone(phone, databaseName);
                    if (StringUtils.hasText(loginNameByPhone)) {
                        loginName = loginNameByPhone;
                        log.info("근거휴대폰 번호조회까지로그인이름: {}, 휴대폰 번호: {}", loginName, phone);
                    } else {
                        log.warn("근거휴대폰 번호찾을 수 없는 로그인이름, 사용휴대폰 번호로로그인이름, 휴대폰 번호: {}", phone);
                        loginName = phone;
                    }
                } else {
                    throw new RuntimeException("로그인이름및휴대폰 번호할 수 없음시비어 있습니다");
                }
            }

            // 직선연결호출UAP업데이트비밀번호연결, UAP검증비밀번호여부정상
            UpdatePwdDto updatePwdDto = new UpdatePwdDto();
            updatePwdDto.setLoginName(loginName);
            updatePwdDto.setOldPwd(Base64Utils.encodeToString(
                    changePasswordDto.getOldPassword().getBytes(StandardCharsets.UTF_8)));
            updatePwdDto.setNewPwd(Base64Utils.encodeToString(
                    changePasswordDto.getNewPassword().getBytes(StandardCharsets.UTF_8)));

            ResponseDto<String> updatePwdResponse = ClientAuthenticationAPI.updateUserPwd(updatePwdDto);
            if (updatePwdResponse == null || !updatePwdResponse.isFlag()) {
                String errorMsg = updatePwdResponse != null ? updatePwdResponse.getMessage() : "업데이트비밀번호비어 있습니다";
                log.error("업데이트UAP비밀번호실패: {}", errorMsg);
                throw new RuntimeException("업데이트비밀번호실패: " + errorMsg);
            }

            log.info("수정비밀번호성공, 계정: {}, 열기완료시인증", loginName);

            // 완료시인증, 저장로그인정보(패키지새비밀번호)
            String tempToken = UUID.randomUUID().toString().replace("-", "");
            String cacheKey = TEMP_TOKEN_PREFIX + tempToken;

            // 생성LoginDto, 패키지로그인이름, 휴대폰 번호및새비밀번호
            LoginDto loginDto = new LoginDto();
            loginDto.setLoginName(loginName);
            loginDto.setPhone(phone);
            loginDto.setPassword(changePasswordDto.getNewPassword());
            // platform가능으로에서session중가져오기, 결과가있음이면로client
            loginDto.setPlatform("client");

            // 생성저장데이터
            Map<String, Object> cacheData = new HashMap<>();
            cacheData.put("loginDto", loginDto);

            // 를데이터순서열로JSON저장까지Redis
            String cacheDataJson = objectMapper.writeValueAsString(cacheData);
            RedisUtils.set(cacheKey, cacheDataJson, TEMP_TOKEN_EXPIRE_SECONDS);

            log.info("시인증완료완료, 계정: {}, 경과시간: {}초", loginName, TEMP_TOKEN_EXPIRE_SECONDS);
            return AppResponse.success(tempToken);

        } catch (Exception e) {
            log.error("수정비밀번호예외, 계정: {}", changePasswordDto != null ? changePasswordDto.getLoginName() : "지원하지 않는", e);
            throw new RuntimeException("수정비밀번호실패: " + e.getMessage(), e);
        }
    }

    /**
     * 관리단일로그인: 지우기session, 저장새sessionId
     *
     * @param userId 사용자ID
     * @param request HTTP요청 
     */
    private void handleSingleSignOn(String userId, HttpServletRequest request) {
        try {
            if (!StringUtils.hasText(userId)) {
                return;
            }

            // 가져오기현재sessionId
            String currentSessionId = request.getSession().getId();
            if (!StringUtils.hasText(currentSessionId)) {
                log.warn("불가가져오기sessionId, 사용자ID: {}", userId);
                return;
            }

            // Redis key: user:session:{userId}
            String redisKey = RedisKeyConstant.REDIS_KEY_USER_SESSION_PREFIX + userId;

            // 조회여부완료있음의sessionId
            Object oldSessionIdObj = RedisUtils.get(redisKey);
            if (oldSessionIdObj != null && StringUtils.hasText(oldSessionIdObj.toString())) {
                String oldSessionId = oldSessionIdObj.toString();

                // 결과가sessionId및현재sessionId아니요, 지우기session
                if (!oldSessionId.equals(currentSessionId)) {
                    log.info(
                            "감지까지사용자에서방법로그인, 지우기session, 사용자ID: {}, sessionId: {}, 새sessionId: {}",
                            userId,
                            oldSessionId,
                            currentSessionId);

                    // 지우기session(Spring Session에서Redis중의key형식: uap:session:sessions:{sessionId})
                    String oldSessionRedisKey = "uap:session:sessions:" + oldSessionId;
                    RedisUtils.del(oldSessionRedisKey);

                    // 지우기session의경과key(Spring Session저장경과시간)
                    String oldSessionExpiresKey = "uap:session:sessions:expires:" + oldSessionId;
                    RedisUtils.del(oldSessionExpiresKey);
                }
            }

            // 저장새의sessionId(TTL로30, 및session경과시간보관일또는길이)
            RedisUtils.set(redisKey, currentSessionId, 2592000); // 30

            log.debug("단일로그인session완료업데이트, 사용자ID: {}, sessionId: {}", userId, currentSessionId);

        } catch (Exception e) {
            log.error("관리단일로그인실패, 사용자ID: {}", userId, e);
            // 아니요출력예외, 로그인프로세스
        }
    }

    @Override
    public AppResponse<String> addUser(AddUserDto user, HttpServletRequest request) {
        if (user == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "사용자 매개변수는 비워 둘 수 없습니다");
        }
        if (StringUtils.hasText(user.getPassword())
                && StringUtils.hasText(user.getConfirmPassword())
                && !user.getPassword().equals(user.getConfirmPassword())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "입력한 비밀번호가 올바르지 않습니다");
        }
        try {
            return userServiceImpl.addUser(user, request);
        } catch (Exception e) {
            log.error("UAP 사용자 생성 실패, 휴대폰 번호: {}", user.getPhone(), e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "사용자 생성 실패: " + e.getMessage());
        }
    }
}
