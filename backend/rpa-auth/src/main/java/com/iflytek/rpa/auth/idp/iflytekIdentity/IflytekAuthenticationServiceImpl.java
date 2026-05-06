package com.iflytek.rpa.auth.idp.iflytekIdentity;

import static com.iflytek.rpa.auth.sp.uap.constants.UAPConstant.DEFAULT_INITIAL_PASSWORD;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.acount.sdk.CAccountClient;
import com.iflytek.acount.sdk.CAccountResponse;
import com.iflytek.rpa.auth.blacklist.exception.ShouldBeBlackException;
import com.iflytek.rpa.auth.blacklist.service.PasswordErrorService;
import com.iflytek.rpa.auth.core.entity.*;
import com.iflytek.rpa.auth.core.entity.ChangePasswordDto;
import com.iflytek.rpa.auth.core.entity.LoginDto;
import com.iflytek.rpa.auth.core.entity.RegisterDto;
import com.iflytek.rpa.auth.core.entity.Tenant;
import com.iflytek.rpa.auth.core.entity.User;
import com.iflytek.rpa.auth.core.entity.enums.LoginTypeEnum;
import com.iflytek.rpa.auth.core.service.TenantService;
import com.iflytek.rpa.auth.exception.ServiceException;
import com.iflytek.rpa.auth.idp.AuthenticationService;
import com.iflytek.rpa.auth.idp.iflytekIdentity.dto.*;
import com.iflytek.rpa.auth.idp.iflytekIdentity.enums.IflytekLoginModeEnum;
import com.iflytek.rpa.auth.sp.uap.constants.RedisKeyConstant;
import com.iflytek.rpa.auth.sp.uap.constants.UAPConstant;
import com.iflytek.rpa.auth.sp.uap.dao.TenantDao;
import com.iflytek.rpa.auth.sp.uap.dao.UserDao;
import com.iflytek.rpa.auth.sp.uap.mapper.UserMapper;
import com.iflytek.rpa.auth.sp.uap.service.impl.UserServiceImpl;
import com.iflytek.rpa.auth.utils.AppResponse;
import com.iflytek.rpa.auth.utils.ErrorCodeEnum;
import com.iflytek.rpa.auth.utils.MenuPermissionValidator;
import com.iflytek.rpa.auth.utils.RedisUtils;
import com.iflytek.sec.uap.base.util.ClientConfigUtil;
import com.iflytek.sec.uap.client.api.ClientAuthenticationAPI;
import com.iflytek.sec.uap.client.api.UapUserInfoAPI;
import com.iflytek.sec.uap.client.core.dto.ResponseDto;
import com.iflytek.sec.uap.client.core.dto.authentication.LoginTokenResponseDto;
import com.iflytek.sec.uap.client.core.dto.pwd.UpdatePwdDto;
import com.iflytek.sec.uap.client.core.dto.tenant.UapTenant;
import com.iflytek.sec.uap.client.core.dto.user.UapUser;
import com.iflytek.sec.uap.client.util.CommonValidateUtil;
import com.iflytek.sec.uap.client.util.Oauth2Util;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "saas", matchIfMissing = true)
public class IflytekAuthenticationServiceImpl implements AuthenticationService {

    private final UserServiceImpl userService;

    private final UserMapper userMapper;

    private final UserDao userDao;

    private final PasswordErrorService passwordErrorService;

    @Value("${iflytek.account.host}")
    private String accountHost;

    @Value("${iflytek.account.appid}")
    private String appid;

    @Value("${iflytek.account.accessKey}")
    private String accessKey;

    @Value("${iflytek.account.accessSecret}")
    private String accessSecret;

    @Value("${rpa.auth.local-debug:false}")
    private boolean localDebug;

    @Value("${rpa.auth.local-ip:172.31.114.36}")
    private String localDebugIp;

    // 서비스서버IP주소, 지원에서변수또는매칭파일가져오기(K8s사용)
    @Value("${rpa.auth.server-ip:#{null}}")
    private String serverIp;

    @Value("${uap.database.name:uap_db}")
    private String databaseName;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private TenantService tenantService;

    private static final int TIME_OUT = 10000;
    private static final boolean USE_AES_ENCRYPT = true;
    private static final String CHECK_LOGIN_ID_PATH = "/register/svr/checkLoginID";
    private static final String SEND_MSG_CODE_PATH = "/login/svr/sendMsgCode";
    private static final String REGISTER_SUBMIT_PATH = "/register/svr/submit";
    private static final String LOGIN_PATH = "/login/svr/aggrLogin";
    private static final String VERIFY_CODE_PATH = "/login/svr/checkCode";
    private static final String UPDATE_PASSWORD_PATH = "/userinfo/svr/update/pwd";
    private static final String DELETE_USER_PATH = "/logout/svr/delete";
    private static final String SYNC_USER_INFO_PATH = "/general/svr/syncUserInfo";
    private static final int DEFAULT_SMS_EXPIRE_SECONDS = 600;
    private static final String DEFAULT_COUNTRY_CODE = "86";
    private static final int DEFAULT_LOGIN_TYPE = 1;
    private static final String DEFAULT_PASSWORD_TYPE = "md5";

    // 시인증저장전및경과시간(10분)
    private static final String TEMP_TOKEN_PREFIX = "auth:temp_token:";
    private static final int TEMP_TOKEN_EXPIRE_SECONDS = 600;

    // 인증 코드저장전및경과시간(10분, 및짧음정보인증 코드경과시간일)
    private static final String VERIFY_CODE_PREFIX = "auth:verify_code:";
    private final ObjectMapper objectMapper;
    private final IflytekAccountClientFactory accountClientFactory;

    // Shoprpa계정클라이언트(단일복사사용, 재복사생성)
    private CAccountClient accountClient;

    @Override
    public String preAuthenticate(LoginDto loginDto, HttpServletRequest request) {
        if (loginDto == null) {
            throw new ServiceException("로그인매개변수비워 둘 수 없습니다");
        }
        try {
            log.info("열기 인증, 휴대폰 번호: {}", loginDto.getPhone());

            // 1. 호출Shoprpa계정인증사용자
            CAccountClient client = getAccountClient();
            String traceId = generateTraceId();
            IflytekLoginModeEnum loginMode = resolveLoginMode(loginDto);
            String scene = resolveScene(loginDto.getScene(), AuthenticationService.SCENE_LOGIN);

            if (loginMode == IflytekLoginModeEnum.FREE) {
                if (!verifyCode(loginDto.getPhone(), loginDto.getCaptcha(), scene)) {
                    throw new ServiceException("인증 코드오류또는완료실패");
                }
            }

            byte[] requestBody = buildLoginRequest(loginDto, traceId, loginMode);
            byte[] responseBytes = executePost(client, LOGIN_PATH, requestBody, "인증");
            IflytekAccountResponse<IflytekLoginData> responseDto = parseResponse(responseBytes, IflytekLoginData.class);
            String respCode = responseDto.getCode();

            if ("000000".equals(respCode)) {
                IflytekLoginData data = responseDto.getData();
                if (data == null || !StringUtils.hasText(data.getUserid())) {
                    log.error("인증 실패, 반환되지 않았습니다있음사용자 정보");
                    throw new ServiceException("인증 실패: 반환되지 않았습니다사용자 정보");
                }

                // 2. 근거휴대폰 번호인증사용자여부에서UAP저장에서, 결과가찾을 수 없습니다이면회원가입
                ensureUapUserExistsAndRegister(loginDto, data.getUserid(), request);

                // 3. 완료시인증저장사용자 정보
                String tempToken = UUID.randomUUID().toString().replace("-", "");
                String cacheKey = TEMP_TOKEN_PREFIX + tempToken;

                // 저장사용자휴대폰 번호및필요정보(JSON형식)
                String userInfo = objectMapper.writeValueAsString(loginDto);
                RedisUtils.set(cacheKey, userInfo, TEMP_TOKEN_EXPIRE_SECONDS);

                log.info("인증 성공, 휴대폰 번호: {}, 시인증완료완료", loginDto.getPhone());
                return tempToken;

            } else if ("720101".equals(respCode)) {
                log.warn("인증 실패: 계정찾을 수 없습니다, 휴대폰 번호 {}", loginDto.getPhone());
                throw new ServiceException("계정찾을 수 없습니다, 요청 회원가입");
            } else if ("720102".equals(respCode)) {
                log.warn("인증 실패: 비밀번호오류, 휴대폰 번호 {}", loginDto.getPhone());

                // 기록비밀번호오류, 결과가까지값출력 ShouldBeBlackException
                try {
                    String userId = getUserIdByPhone(loginDto.getPhone());
                    if (StringUtils.hasText(userId)) {
                        // 결과가출력 ShouldBeBlackException, 직선연결업로드, 아니요 catch
                        passwordErrorService.recordPasswordError(userId, loginDto.getPhone());
                    }
                } catch (ShouldBeBlackException e) {
                    // 예외직선연결위출력, 전체영역예외관리기기관리
                    throw e;
                } catch (Exception e) {
                    // 예외(예가져오기userId실패)아니요프로세스, 기록로그가능
                    log.error("기록비밀번호오류실패", e);
                }

                // 결과가있음출력 ShouldBeBlackException, 설명찾을 수 없는 값, 반환통신오류
                throw new ServiceException("계정또는비밀번호오류");
            }

            log.error("인증 실패, 오류코드: {}, 오류정보: {}", respCode, responseDto.getDesc());
            throw new ServiceException("인증 실패: " + responseDto.getDesc());

        } catch (ShouldBeBlackException e) {
            // 예외직선연결위출력, 전체영역예외관리기기관리
            throw e;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("인증예외, 매개변수: {}", loginDto, e);
            throw new ServiceException("인증예외: " + e.getMessage());
        }
    }

    /**
     * 삭제Shoprpa계정
     *
     * @param phone 휴대폰 번호
     */
    public void deleteUser(String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new ServiceException("휴대폰 번호는 비워 둘 수 없습니다");
        }

        try {
            log.info("열기 삭제Shoprpa계정, 휴대폰 번호: {}", phone);

            // 1. 근거휴대폰 번호가져오기Shoprpa계정의 userid(에서 UAP 사용자의 third_ext_info 필드가져오기)
            String iflytekUserId = getIflytekUserIdByPhone(phone);
            if (!StringUtils.hasText(iflytekUserId)) {
                throw new ServiceException("찾을 수 없는 해당휴대폰 번호의Shoprpa계정ID");
            }

            // 2. 호출Shoprpa계정삭제연결
            CAccountClient client = getAccountClient();
            String traceId = generateTraceId();
            byte[] requestBody = buildDeleteUserRequest(iflytekUserId, traceId);
            byte[] responseBytes = executePost(client, DELETE_USER_PATH, requestBody, "삭제사용자");
            IflytekAccountResponse<Void> responseDto = parseResponse(responseBytes, Void.class);
            String code = responseDto.getCode();

            if (isSuccessCode(code)) {
                log.info("삭제Shoprpa계정성공, 휴대폰 번호: {}, userid: {}", phone, iflytekUserId);
                return;
            }

            if ("750101".equals(code)) {
                throw new ServiceException("사용자를 찾을 수 없습니다");
            }

            throw new ServiceException("삭제사용자실패: " + responseDto.getDesc());

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("삭제Shoprpa계정예외, 휴대폰 번호: {}", phone, e);
            throw new ServiceException("삭제Shoprpa계정예외: " + e.getMessage());
        }
    }

    /**
     * 근거휴대폰 번호가져오기Shoprpa계정의 userid(에서 UAP 사용자의 third_ext_info 필드가져오기)
     *
     * @param phone 휴대폰 번호
     * @return Shoprpa계정의 userid
     */
    private String getIflytekUserIdByPhone(String phone) {
        try {
            // 직선연결조회데이터베이스가져오기 third_ext_info
            String thirdExtInfo = userDao.queryThirdExtInfoByPhone(phone, databaseName);

            if (!StringUtils.hasText(thirdExtInfo)) {
                log.warn("사용자지정되지 않았습니다Shoprpa계정ID, 휴대폰 번호: {}", phone);
                return null;
            }

            return thirdExtInfo;

        } catch (Exception e) {
            log.error("가져오기Shoprpa계정ID실패, 휴대폰 번호: {}", phone, e);
            return null;
        }
    }

    /**
     * 근거휴대폰 번호가져오기사용자ID
     *
     * @param phone 휴대폰 번호
     * @return 사용자ID
     */
    private String getUserIdByPhone(String phone) {
        try {
            String userId = userDao.getUserIdByPhone(phone, databaseName);
            if (!StringUtils.hasText(userId)) {
                log.warn("찾을 수 없는 사용자, 휴대폰 번호: {}", phone);
                return null;
            }
            return userId;
        } catch (Exception e) {
            log.error("가져오기사용자ID실패, 휴대폰 번호: {}", phone, e);
            return null;
        }
    }

    /**
     * 저장량사용자데이터
     *
     * @param userid        사용자ID
     * @param password      비밀번호(가능비어 있습니다)
     * @param loginAccounts 로그인계정목록
     * @param userInfo      사용자정보
     */
    public void syncUserInfo(
            String userid,
            String password,
            List<IflytekSyncUserInfoAccount> loginAccounts,
            IflytekSyncUserInfoUserInfo userInfo) {
        if (!StringUtils.hasText(userid)) {
            throw new ServiceException("사용자 ID는 비워 둘 수 없습니다");
        }
        if (loginAccounts == null || loginAccounts.isEmpty()) {
            throw new ServiceException("로그인계정목록은 비워 둘 수 없습니다");
        }

        try {
            log.info("열기 사용자 정보, 사용자ID: {}", userid);

            CAccountClient client = getAccountClient();
            String traceId = generateTraceId();
            byte[] requestBody = buildSyncUserInfoRequest(userid, password, loginAccounts, userInfo, traceId);
            byte[] responseBytes = executePost(client, SYNC_USER_INFO_PATH, requestBody, "사용자 정보");
            IflytekAccountResponse<Void> responseDto = parseResponse(responseBytes, Void.class);
            String code = responseDto.getCode();

            if ("000000".equals(code)) {
                log.info("사용자 정보성공, 사용자ID: {}", userid);
                return;
            }

            if ("740103".equals(code)) {
                log.warn("사용자 정보실패: 사용자완료저장에서, 사용자ID {}", userid);
                throw new ServiceException("사용자완료저장에서");
            }

            if ("740101".equals(code)) {
                log.warn("사용자 정보실패: 로그인방식재복사, 사용자ID {}", userid);
                throw new ServiceException("로그인방식재복사");
            }

            log.error("사용자 정보실패, 오류코드: {}, 오류정보: {}", code, responseDto.getDesc());
            throw new ServiceException("사용자 정보실패: " + responseDto.getDesc());

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("사용자 정보예외, 사용자ID: {}", userid, e);
            throw new ServiceException("사용자 정보예외: " + e.getMessage());
        }
    }

    /**
     * 수정비밀번호
     *
     * @param changePasswordDto 수정비밀번호요청 매개변수
     */
    public void updatePassword(ChangePasswordDto changePasswordDto) {
        if (changePasswordDto == null) {
            throw new ServiceException("수정비밀번호매개변수비워 둘 수 없습니다");
        }
        String phone = changePasswordDto.getPhone();
        String oldPassword = changePasswordDto.getOldPassword();
        String newPassword = changePasswordDto.getNewPassword();
        if (!StringUtils.hasText(phone)) {
            throw new ServiceException("휴대폰 번호는 비워 둘 수 없습니다");
        }
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new ServiceException("비밀번호는 비워 둘 수 없습니다");
        }
        if (oldPassword.equals(newPassword)) {
            throw new ServiceException("새비밀번호할 수 없음및기존비밀번호일");
        }
        try {
            CAccountClient client = getAccountClient();
            String traceId = generateTraceId();
            String oldPwdMd5 = toMd5Hex(oldPassword);
            String newPwdMd5 = toMd5Hex(newPassword);
            byte[] requestBody = buildUpdatePasswordRequest(phone, traceId, oldPwdMd5, newPwdMd5);
            byte[] responseBytes = executePost(client, UPDATE_PASSWORD_PATH, requestBody, "수정비밀번호");
            IflytekAccountResponse<Void> responseDto = parseResponse(responseBytes, Void.class);
            String code = responseDto.getCode();
            if (isSuccessCode(code)) {
                log.info("수정비밀번호성공, 휴대폰 번호: {}", phone);
                return;
            }
            if ("0100100".equals(code)) {
                throw new ServiceException("수정비밀번호실패: 매개변수오류");
            }
            if ("0402200".equals(code)) {
                throw new ServiceException("계정찾을 수 없습니다");
            }
            throw new ServiceException("수정비밀번호실패: " + responseDto.getDesc());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("수정비밀번호예외, 휴대폰 번호: {}", changePasswordDto.getPhone(), e);
            throw new ServiceException("수정비밀번호예외: " + e.getMessage());
        }
    }

    /**
     * 관리관리단말근거로그인이름업데이트사용자비밀번호
     *
     * @param loginName   로그인이름
     * @param oldPassword 비밀번호
     * @param newPassword 새비밀번호
     */
    public void updateUserPassword(String loginName, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(loginName)) {
            throw new ServiceException("사용자명은 비워 둘 수 없습니다");
        }
        String phone = userDao.queryPhoneByLoginName(loginName, databaseName);
        if (!StringUtils.hasText(phone)) {
            throw new ServiceException("찾을 수 없는 해당사용자또는휴대폰 번호가 비어 있습니다");
        }

        // 업데이트Shoprpa계정비밀번호
        updateIflytekPassword(phone, newPassword);

        // 업데이트 UAP 비밀번호
        UpdatePwdDto updatePwdDto = new UpdatePwdDto();
        updatePwdDto.setLoginName(loginName);
        updatePwdDto.setOldPwd(Base64Utils.encodeToString(oldPassword.getBytes(StandardCharsets.UTF_8)));
        updatePwdDto.setNewPwd(Base64Utils.encodeToString(newPassword.getBytes(StandardCharsets.UTF_8)));

        ResponseDto<String> response = ClientAuthenticationAPI.updateUserPwd(updatePwdDto);
        if (!response.isFlag()) {
            throw new ServiceException("업데이트UAP비밀번호실패: " + response.getMessage());
        }

        // 업데이트 ext_info 로 null
        try {
            userDao.updateExtInfo(phone, null, databaseName);
            log.info("완료업데이트사용자 ext_info 로 null, 로그인이름: {}", loginName);
        } catch (Exception e) {
            log.error("업데이트사용자 ext_info 실패, 로그인이름: {}", loginName, e);
            // 아니요출력예외, 원인로비밀번호업데이트완료완료, 예정보업데이트실패
        }

        log.info("관리관리원업데이트사용자비밀번호성공, 로그인이름: {}", loginName);
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
            log.info("열기 정상방식로그인, 시인증: {}, 테넌트ID: {}", tempToken, tenantId);

            // 1. 에서저장중가져오기사용자 정보
            String cacheKey = TEMP_TOKEN_PREFIX + tempToken;

            LoginDto loginDto = getLoginInfoByTempToken(tempToken);
            loginDto.setTenantId(tenantId);

            // 에서저장중가져오기platform, 결과가비어 있습니다이면사용client
            String platform = loginDto.getPlatform();
            if (!StringUtils.hasText(platform)) {
                platform = "client";
            }
            log.info("에서저장가져오기평면: {}", platform);

            // 2. 까지UAP생성session(사용자완료에서인증회원가입)
            UapUser uapUser = syncAndLoginUap(loginDto, servletRequest);
            // 업데이트테넌트사용자의후로그인시간
            String tenantUserId = tenantDao.getTenantUserId(databaseName, uapUser.getId(), tenantId);
            tenantDao.updateLoginTime(databaseName, tenantUserId);

            // 3. 를platform저장까지session
            if (StringUtils.hasText(platform)) {
                servletRequest.getSession().setAttribute(UAPConstant.SESSION_KEY_PLATFORM, platform);
                log.debug("완료저장로그인평면까지session, 평면: {}", platform);
            }

            // 4. 로그인성공, 지우기비밀번호오류계획데이터
            try {
                passwordErrorService.clearPasswordError(uapUser.getId());
            } catch (Exception e) {
                log.error("지우기비밀번호오류계획데이터실패", e);
            }

            // 5. 관리단일로그인: 있음클라이언트로그인실행단일로그인
            if (UAPConstant.PLATFORM_CLIENT.equals(platform)) {
                handleSingleSignOn(uapUser.getId(), servletRequest);
            } else {
                log.debug("클라이언트로그인(platform: {}), 건너뛰기단일로그인관리, 사용자ID: {}", platform, uapUser.getId());
            }

            // 6. 삭제시인증
            RedisUtils.del(cacheKey);

            log.info("정상방식로그인성공, 사용자ID: {}, 테넌트ID: {}", uapUser.getId(), tenantId);
            return userMapper.fromUapUser(uapUser);

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("정상방식로그인예외, 시인증: {}", tempToken, e);
            throw new ServiceException("정상방식로그인예외: " + e.getMessage());
        }
    }

    private LoginDto getLoginDtoByTempToken(String tempToken) {
        // 공유연결방법법
        return getLoginInfoByTempToken(tempToken);
    }

    @Override
    public LoginDto getLoginInfoByTempToken(String tempToken) {
        if (!StringUtils.hasText(tempToken)) {
            return null;
        }

        try {
            String cacheKey = TEMP_TOKEN_PREFIX + tempToken;
            Object cachedUserInfo = RedisUtils.get(cacheKey);

            if (cachedUserInfo == null) {
                throw new ServiceException("시인증완료경과또는없음");
            }

            LoginDto loginDto = objectMapper.readValue(cachedUserInfo.toString(), LoginDto.class);
            if (loginDto.getPhone() == null) {
                Map<String, Object> dataMap = objectMapper.readValue(
                        cachedUserInfo.toString(),
                        objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));

                RegisterDto registerDto = objectMapper.convertValue(dataMap.get("registerDto"), RegisterDto.class);
                loginDto.setPhone(registerDto.getPhone());
            }

            return loginDto;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("가져오기로그인정보예외, 시인증: {}", tempToken, e);
            throw new ServiceException("가져오기로그인정보예외: " + e.getMessage());
        }
    }

    @Override
    public String getPhoneByTempToken(String tempToken) {
        if (!StringUtils.hasText(tempToken)) {
            return null;
        }

        try {
            String cacheKey = TEMP_TOKEN_PREFIX + tempToken;
            Object cachedUserInfo = RedisUtils.get(cacheKey);

            if (cachedUserInfo == null) {
                throw new ServiceException("시인증완료경과또는없음");
            }

            LoginDto loginDto = objectMapper.readValue(cachedUserInfo.toString(), LoginDto.class);
            if (loginDto.getPhone() == null) {
                Map<String, Object> dataMap = objectMapper.readValue(
                        cachedUserInfo.toString(),
                        objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));

                RegisterDto registerDto = objectMapper.convertValue(dataMap.get("registerDto"), RegisterDto.class);
                loginDto.setPhone(registerDto.getPhone());
            }

            return loginDto.getPhone();

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("가져오기휴대폰 번호예외, 시인증: {}", tempToken, e);
            throw new ServiceException("가져오기휴대폰 번호예외: " + e.getMessage());
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

        } catch (ServiceException e) {
            log.error("가져오기테넌트목록실패, 시인증: {}", tempToken, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기테넌트목록실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("가져오기테넌트목록예외, 시인증: {}", tempToken, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기테넌트목록실패: " + e.getMessage());
        }
    }

    @Override
    @Deprecated
    public User login(LoginDto loginDto, HttpServletRequest servletRequest) {
        if (loginDto == null) {
            throw new ServiceException("로그인매개변수비워 둘 수 없습니다");
        }
        try {
            CAccountClient client = getAccountClient();
            String traceId = generateTraceId();
            IflytekLoginModeEnum loginMode = resolveLoginMode(loginDto);
            if (loginMode == IflytekLoginModeEnum.FREE) {
                String scene = resolveScene(loginDto.getScene(), AuthenticationService.SCENE_LOGIN);
                if (!verifyCode(loginDto.getPhone(), loginDto.getCaptcha(), scene)) {
                    throw new ServiceException("인증 코드오류또는완료실패");
                }
                // ensurePhoneRegistered(loginDto.getPhone());
            }
            byte[] requestBody = buildLoginRequest(loginDto, traceId, loginMode);
            byte[] responseBytes = executePost(client, LOGIN_PATH, requestBody, "계정로그인");
            IflytekAccountResponse<IflytekLoginData> responseDto = parseResponse(responseBytes, IflytekLoginData.class);
            String respCode = responseDto.getCode();
            if ("000000".equals(respCode)) {
                IflytekLoginData data = responseDto.getData();
                if (data == null || !StringUtils.hasText(data.getUserid())) {
                    log.error("계정로그인실패, 반환되지 않았습니다있음사용자 정보");
                    throw new ServiceException("계정로그인실패: 반환되지 않았습니다사용자 정보");
                }

                UapUser uapUser = syncAndLoginUap(loginDto, servletRequest);

                return userMapper.fromUapUser(uapUser);
            } else if ("720101".equals(respCode)) {
                log.warn("계정로그인실패: 계정찾을 수 없습니다, 휴대폰 번호 {}", loginDto.getPhone());
                throw new ServiceException("계정찾을 수 없습니다, 요청 회원가입");
            } else if ("720102".equals(respCode)) {
                log.warn("계정로그인실패: 비밀번호오류, 휴대폰 번호 {}", loginDto.getPhone());
                throw new ServiceException("계정또는비밀번호오류");
            }
            log.error("계정로그인실패, 오류코드: {}, 오류정보: {}", respCode, responseDto.getDesc());
            throw new ServiceException("계정로그인실패: " + responseDto.getDesc());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("계정로그인예외, 매개변수: {}", loginDto, e);
            throw new ServiceException("계정로그인예외: " + e.getMessage());
        }
    }

    private UapUser syncAndLoginUap(LoginDto loginDto, HttpServletRequest servletRequest) {
        ensureUapTenant(loginDto, servletRequest);
        // 인증사용자여부있음해당테넌트
        validateTenantPermission(loginDto);
        return loginUapByPhone(loginDto, servletRequest);
    }

    /**
     * 인증사용자여부있음해당테넌트권한
     * 근거휴대폰 번호조회로그인계정, 근거로그인계정조회테넌트목록, 인증현재로그인의테넌트id여부에서목록
     *
     * @param loginDto 로그인정보
     * @throws ServiceException 결과가인증 실패이면출력예외
     */
    private void validateTenantPermission(LoginDto loginDto) {
        String phone = loginDto.getPhone();
        if (!StringUtils.hasText(phone)) {
            throw new ServiceException("휴대폰 번호는 비워 둘 수 없습니다");
        }
        if (!StringUtils.hasText(loginDto.getTenantId())) {
            throw new ServiceException("테넌트 ID는 비워 둘 수 없습니다");
        }

        // 근거휴대폰 번호조회로그인계정
        String loginName = userDao.queryLoginNameByPhone(phone, databaseName);
        if (!StringUtils.hasText(loginName)) {
            log.warn("근거휴대폰 번호찾을 수 없는 사용자로그인이름, 휴대폰 번호: {}", phone);
            loginName = phone;
        }

        // 근거로그인계정조회테넌트목록
        List<UapTenant> tenantList = ClientAuthenticationAPI.getTenantListInAppByLoginName(loginName);
        if (CollectionUtils.isEmpty(tenantList)) {
            log.warn("근거로그인이름찾을 수 없는 테넌트 정보, 로그인이름: {}", loginName);
            throw new ServiceException("현재빈권한검증실패, 요청다시 로그인");
        }

        // 인증현재로그인의테넌트id여부에서목록
        boolean hasPermission =
                tenantList.stream().anyMatch(tenant -> loginDto.getTenantId().equals(tenant.getId()));

        if (!hasPermission) {
            log.warn("사용자있음해당테넌트권한, 로그인이름: {}, 테넌트ID: {}", loginName, loginDto.getTenantId());
            throw new ServiceException("현재빈권한검증실패, 요청다시 로그인");
        }
    }

    /**
     * 에서인증조회사용자여부에서UAP저장에서, 결과가찾을 수 없습니다이면회원가입
     *
     * @param loginDto      로그인정보
     * @param iflytekUserId Shoprpa계정의userId
     * @param request       HTTP요청 (사용회원가입UAP사용자)
     */
    private void ensureUapUserExistsAndRegister(LoginDto loginDto, String iflytekUserId, HttpServletRequest request) {
        String phone = loginDto.getPhone();
        if (!StringUtils.hasText(phone)) {
            return;
        }

        // 근거휴대폰 번호조회사용자로그인이름, 사용자여부저장에서
        String loginName = userDao.queryLoginNameByPhone(phone, databaseName);
        if (!StringUtils.hasText(loginName)) {
            // 사용자를 찾을 수 없습니다, 회원가입까지UAP
            log.info("UAP에 사용자가 없어 회원가입을 시작합니다, 휴대폰 번호: {}, Shoprpa계정userId: {}", phone, iflytekUserId);

            RegisterDto registerDto = RegisterDto.builder()
                    .phone(phone)
                    .loginName(phone) // 사용휴대폰 번호로로그인이름
                    .password(null) // 인증아니요필요비밀번호
                    .build();

            AppResponse<String> registerResponse = userService.register(registerDto, request);
            if (registerResponse == null || !registerResponse.ok()) {
                String message = registerResponse == null ? "회원가입실패: UAP반환되지 않았습니다" : registerResponse.getMessage();
                log.error("회원가입UAP사용자실패, 휴대폰 번호: {}, 오류: {}", phone, message);
                throw new ServiceException("회원가입UAP사용자실패: " + message);
            }

            // 를Shoprpa계정의userId저장까지UAP사용자의third_ext_info필드
            if (StringUtils.hasText(iflytekUserId)) {
                try {
                    userDao.updateThirdExtInfo(phone, iflytekUserId, databaseName);
                    log.info("완료저장Shoprpa계정userId까지UAP사용자필드, 로그인이름: {}, userId: {}", phone, iflytekUserId);
                } catch (Exception e) {
                    log.error("저장Shoprpa계정userId까지UAP사용자필드실패, 로그인이름: {}, userId: {}", phone, iflytekUserId, e);
                    // 아니요출력예외, 원인로회원가입완료완료, 예정보저장실패
                }
            }

            log.info("UAP사용자회원가입성공, 휴대폰 번호: {}, 테넌트ID: {}", phone, registerResponse.getData());
        } else {
            log.debug("사용자완료저장된 UAP, 로그인이름: {}, 휴대폰 번호: {}", loginName, phone);
        }
    }

    private void ensureUapTenant(LoginDto loginDto, HttpServletRequest servletRequest) {
        if (StringUtils.hasText(loginDto.getTenantId())) {
            return;
        }
        RegisterDto registerDto = RegisterDto.builder()
                .phone(loginDto.getPhone())
                .password(loginDto.getPassword())
                .build();
        AppResponse<String> registerResponse = userService.register(registerDto, servletRequest);
        if (registerResponse == null || !registerResponse.ok()) {
            String message = registerResponse == null ? "회원가입실패: UAP반환되지 않았습니다" : registerResponse.getMessage();
            throw new ServiceException(message);
        }
        loginDto.setTenantId(registerResponse.getData());
    }

    private UapUser loginUapByPhone(LoginDto loginDto, HttpServletRequest servletRequest) {
        AppResponse<UapUser> loginResponse =
                userService.loginNoPasswordByPhone(loginDto.getPhone(), loginDto.getTenantId(), servletRequest);
        if (loginResponse == null || !loginResponse.ok() || loginResponse.getData() == null) {
            log.error("계정로그인실패, UAP반환되지 않았습니다있음사용자 정보");
            throw new ServiceException("계정로그인실패: UAP반환되지 않았습니다사용자 정보");
        }
        return loginResponse.getData();
    }

    @Override
    public String register(RegisterDto registerDto, HttpServletRequest request) {
        if (registerDto == null) {
            throw new ServiceException("회원가입매개변수비워 둘 수 없습니다");
        }

        if (!StringUtils.hasText(registerDto.getPhone())) {
            throw new ServiceException("휴대폰 번호는 비워 둘 수 없습니다");
        }

        // 인증 코드검증
        if (StringUtils.hasText(registerDto.getCaptcha())) {
            if (!verifyCode(registerDto.getPhone(), registerDto.getCaptcha(), AuthenticationService.SCENE_REGISTER)) {
                throw new ServiceException("인증 코드오류또는완료실패");
            }
        } else {
            throw new ServiceException("인증 코드는 비워 둘 수 없습니다");
        }

        try {
            log.info("열기 회원가입, 휴대폰 번호: {}", registerDto.getPhone());

            // 1. 에서Shoprpa계정회원가입(사용빈비밀번호)
            CAccountClient client = getAccountClient();
            String traceId = generateTraceId();
            byte[] requestBody = buildRegisterRequest(registerDto, traceId);
            byte[] responseBytes = executePost(client, REGISTER_SUBMIT_PATH, requestBody, "제출회원가입");
            IflytekAccountResponse<IflytekRegisterData> responseDto =
                    parseResponse(responseBytes, IflytekRegisterData.class);
            String respCode = responseDto.getCode();

            if ("000000".equals(respCode) || "00000".equals(respCode)) {
                if (responseDto.getData() == null
                        || !StringUtils.hasText(responseDto.getData().getUserid())) {
                    log.warn("회원가입성공반환되지 않았습니다userid");
                    throw new ServiceException("회원가입실패: 반환되지 않았습니다사용자ID");
                }

                String iflytekUserId = responseDto.getData().getUserid();
                String phone = registerDto.getPhone();
                String tenantId;

                // 2. 조회UAP중여부완료저장에서해당사용자
                String loginName = userDao.queryLoginNameByPhone(phone, databaseName);
                if (StringUtils.hasText(loginName)) {
                    // 사용자완료저장에서, 조회테넌트목록선택일개
                    log.info("사용자완료저장된 UAP, 조회테넌트목록, 로그인이름: {}", loginName);
                    List<UapTenant> tenantList = ClientAuthenticationAPI.getTenantListInAppByLoginName(loginName);
                    if (CollectionUtils.isEmpty(tenantList)) {
                        log.warn("사용자완료저장된 있음테넌트 정보, 로그인이름: {}, 를실행회원가입", loginName);
                        // 있음테넌트, 회원가입생성새테넌트
                        tenantId = registerUapUser(registerDto, request);
                    } else {
                        // 선택일개테넌트
                        tenantId = tenantList.get(0).getId();
                        log.info("사용자완료저장에서, 선택일개테넌트, 로그인이름: {}, 테넌트ID: {}", loginName, tenantId);
                    }
                } else {
                    // 사용자를 찾을 수 없습니다, 실행회원가입
                    log.info("UAP에 사용자가 없어 회원가입을 시작합니다, 휴대폰 번호: {}", phone);
                    tenantId = registerUapUser(registerDto, request);
                }

                // 2.1 를Shoprpa계정의 userId 저장까지 UAP 사용자의 third_ext_info 필드
                String finalLoginName = StringUtils.hasText(loginName) ? loginName : phone;
                try {
                    userDao.updateThirdExtInfo(finalLoginName, iflytekUserId, databaseName);
                    log.info("완료저장Shoprpa계정 userId 까지 UAP 사용자필드, 로그인이름: {}, userId: {}", finalLoginName, iflytekUserId);
                } catch (Exception e) {
                    log.error("저장Shoprpa계정 userId 까지 UAP 사용자필드실패, 로그인이름: {}, userId: {}", finalLoginName, iflytekUserId, e);
                    // 아니요출력예외, 원인로회원가입완료완료, 예정보저장실패
                }

                // 3. 완료시인증저장회원가입정보
                String tempToken = UUID.randomUUID().toString().replace("-", "");
                String cacheKey = TEMP_TOKEN_PREFIX + tempToken;

                // 를회원가입정보및테넌트ID저장(사용후비밀번호)
                registerDto.setPassword(null); // 빈비밀번호필드, 저장필요정보

                // 저장테넌트ID및회원가입정보
                String cacheData = objectMapper.writeValueAsString(new HashMap<String, Object>() {
                    {
                        put("registerDto", registerDto);
                        put("tenantId", tenantId);
                    }
                });

                RedisUtils.set(cacheKey, cacheData, TEMP_TOKEN_EXPIRE_SECONDS);

                log.info("회원가입성공, 휴대폰 번호: {}, 시인증완료완료, 테넌트ID: {}", registerDto.getPhone(), tenantId);
                return tempToken;

            } else if ("710201".equals(respCode)) {
                log.warn("회원가입 제출 실패: 휴대폰 번호 형식이 올바르지 않습니다, 휴대폰 번호 {}", registerDto.getPhone());
                throw new ServiceException("휴대폰 번호 형식이 올바르지 않습니다");
            } else if ("710203".equals(respCode)) {
                log.warn("제출회원가입실패: 계정완료회원가입, 휴대폰 번호 {}", registerDto.getPhone());
                throw new ServiceException("계정완료회원가입");
            }

            log.error("제출회원가입실패, 오류코드: {}, 오류정보: {}", respCode, responseDto.getDesc());
            throw new ServiceException("제출회원가입실패: " + responseDto.getDesc());

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("제출회원가입예외, 회원가입매개변수: {}", registerDto, e);
            throw new ServiceException("제출회원가입예외: " + e.getMessage());
        }
    }

    public String registerWithoutCaptcha(RegisterDto registerDto, HttpServletRequest request) {
        if (registerDto == null) {
            throw new ServiceException("회원가입매개변수비워 둘 수 없습니다");
        }

        if (!StringUtils.hasText(registerDto.getPhone())) {
            throw new ServiceException("휴대폰 번호는 비워 둘 수 없습니다");
        }

        try {
            log.info("열기 회원가입, 휴대폰 번호: {}", registerDto.getPhone());

            // 1. 에서Shoprpa계정회원가입(사용빈비밀번호)
            CAccountClient client = getAccountClient();
            String traceId = generateTraceId();
            byte[] requestBody = buildRegisterRequest(registerDto, traceId);
            byte[] responseBytes = executePost(client, REGISTER_SUBMIT_PATH, requestBody, "제출회원가입");
            IflytekAccountResponse<IflytekRegisterData> responseDto =
                    parseResponse(responseBytes, IflytekRegisterData.class);
            String respCode = responseDto.getCode();

            if ("000000".equals(respCode) || "00000".equals(respCode)) {
                if (responseDto.getData() == null
                        || !StringUtils.hasText(responseDto.getData().getUserid())) {
                    log.warn("회원가입성공반환되지 않았습니다userid");
                    throw new ServiceException("회원가입실패: 반환되지 않았습니다사용자ID");
                }

                String iflytekUserId = responseDto.getData().getUserid();
                String phone = registerDto.getPhone();
                String tenantId;

                // 2. 조회UAP중여부완료저장에서해당사용자
                String loginName = userDao.queryLoginNameByPhone(phone, databaseName);
                if (StringUtils.hasText(loginName)) {
                    // 사용자완료저장에서, 조회테넌트목록선택일개
                    log.info("사용자완료저장된 UAP, 조회테넌트목록, 로그인이름: {}", loginName);
                    List<UapTenant> tenantList = ClientAuthenticationAPI.getTenantListInAppByLoginName(loginName);
                    if (CollectionUtils.isEmpty(tenantList)) {
                        log.warn("사용자완료저장된 있음테넌트 정보, 로그인이름: {}, 를실행회원가입", loginName);
                        // 있음테넌트, 회원가입생성새테넌트
                        tenantId = registerUapUser(registerDto, request);
                    } else {
                        // 선택일개테넌트
                        tenantId = tenantList.get(0).getId();
                        log.info("사용자완료저장에서, 선택일개테넌트, 로그인이름: {}, 테넌트ID: {}", loginName, tenantId);
                    }
                } else {
                    // 사용자를 찾을 수 없습니다, 실행회원가입
                    log.info("UAP에 사용자가 없어 회원가입을 시작합니다, 휴대폰 번호: {}", phone);
                    tenantId = registerUapUser(registerDto, request);
                }

                // 2.1 를Shoprpa계정의 userId 저장까지 UAP 사용자의 third_ext_info 필드
                String finalLoginName = StringUtils.hasText(loginName) ? loginName : phone;
                try {
                    userDao.updateThirdExtInfo(finalLoginName, iflytekUserId, databaseName);
                    log.info("완료저장Shoprpa계정 userId 까지 UAP 사용자필드, 로그인이름: {}, userId: {}", finalLoginName, iflytekUserId);
                } catch (Exception e) {
                    log.error("저장Shoprpa계정 userId 까지 UAP 사용자필드실패, 로그인이름: {}, userId: {}", finalLoginName, iflytekUserId, e);
                    // 아니요출력예외, 원인로회원가입완료완료, 예정보저장실패
                }

                // 3. 완료시인증저장회원가입정보
                String tempToken = UUID.randomUUID().toString().replace("-", "");
                String cacheKey = TEMP_TOKEN_PREFIX + tempToken;

                // 를회원가입정보및테넌트ID저장(사용후비밀번호)
                registerDto.setPassword(null); // 빈비밀번호필드, 저장필요정보

                // 저장테넌트ID및회원가입정보
                String cacheData = objectMapper.writeValueAsString(new HashMap<String, Object>() {
                    {
                        put("registerDto", registerDto);
                        put("tenantId", tenantId);
                    }
                });

                RedisUtils.set(cacheKey, cacheData, TEMP_TOKEN_EXPIRE_SECONDS);

                log.info("회원가입성공, 휴대폰 번호: {}, 시인증완료완료, 테넌트ID: {}", registerDto.getPhone(), tenantId);
                return tempToken;

            } else if ("710201".equals(respCode)) {
                log.warn("회원가입 제출 실패: 휴대폰 번호 형식이 올바르지 않습니다, 휴대폰 번호 {}", registerDto.getPhone());
                throw new ServiceException("휴대폰 번호 형식이 올바르지 않습니다");
            } else if ("710203".equals(respCode)) {
                log.warn("제출회원가입실패: 계정완료회원가입, 휴대폰 번호 {}", registerDto.getPhone());
                throw new ServiceException("계정완료회원가입");
            }

            log.error("제출회원가입실패, 오류코드: {}, 오류정보: {}", respCode, responseDto.getDesc());
            throw new ServiceException("제출회원가입실패: " + responseDto.getDesc());

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("제출회원가입예외, 회원가입매개변수: {}", registerDto, e);
            throw new ServiceException("제출회원가입예외: " + e.getMessage());
        }
    }

    /**
     * 에서UAP회원가입사용자(사용비밀번호)
     *
     * @param registerDto 회원가입정보
     * @param request     HTTP요청 
     * @return 테넌트ID
     */
    private String registerUapUser(RegisterDto registerDto, HttpServletRequest request) {
        AppResponse<String> register = userService.register(registerDto, request);
        if (!register.ok()) {
            throw new ServiceException(register.getMessage());
        }
        return register.getData();
    }

    @Override
    public User setPasswordAndLogin(String tempToken, String password, String tenantId, HttpServletRequest request) {
        if (!StringUtils.hasText(tempToken)) {
            throw new ServiceException("시인증비워 둘 수 없습니다");
        }
        if (!StringUtils.hasText(password)) {
            throw new ServiceException("비밀번호는 비워 둘 수 없습니다");
        }

        try {
            log.info("열기 비밀번호로그인, 시인증: {}", tempToken);

            // 1. 에서저장중가져오기회원가입정보
            String cacheKey = TEMP_TOKEN_PREFIX + tempToken;
            Object cachedData = RedisUtils.get(cacheKey);

            if (cachedData == null) {
                throw new ServiceException("시인증완료경과또는없음, 요청다시 회원가입");
            }

            // 2. 파싱저장의데이터
            Map<String, Object> dataMap = objectMapper.readValue(
                    cachedData.toString(),
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));

            RegisterDto registerDto = objectMapper.convertValue(dataMap.get("registerDto"), RegisterDto.class);
            String cachedTenantId = (String) dataMap.get("tenantId");

            String phone = registerDto.getPhone();
            String loginName = phone;

            log.info("비밀번호, 휴대폰 번호: {}, 로그인이름: {}", phone, loginName);

            // 3. 업데이트Shoprpa계정비밀번호
            updateIflytekPassword(loginName, password);

            // 4. 업데이트UAP비밀번호
            updateUapPassword(loginName, password);

            // 5. 지정테넌트ID
            if (!StringUtils.hasText(tenantId)) {
                tenantId = cachedTenantId;
            }

            if (!StringUtils.hasText(tenantId)) {
                throw new ServiceException("찾을 수 없는 사용자테넌트, 시스템 관리자에게 문의하세요");
            }

            // 6. 로그인(사용새비밀번호)
            LoginDto loginDto = new LoginDto();
            loginDto.setPhone(phone);
            loginDto.setLoginName(loginName);
            loginDto.setPassword(password);
            loginDto.setTenantId(tenantId);
            loginDto.setLoginType(LoginTypeEnum.PASSWORD);

            UapUser uapUser = syncAndLoginUap(loginDto, request);

            // 7. 삭제시인증
            RedisUtils.del(cacheKey);

            log.info("비밀번호로그인성공, 사용자ID: {}, 테넌트ID: {}", uapUser.getId(), tenantId);
            return userMapper.fromUapUser(uapUser);

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("비밀번호로그인예외, 시인증: {}", tempToken, e);
            throw new ServiceException("비밀번호로그인예외: " + e.getMessage());
        }
    }

    @Override
    public boolean setPassword(String tempToken, String password, String tenantId, HttpServletRequest request) {
        if (!StringUtils.hasText(tempToken)) {
            throw new ServiceException("시인증비워 둘 수 없습니다");
        }
        if (!StringUtils.hasText(password)) {
            throw new ServiceException("비밀번호는 비워 둘 수 없습니다");
        }

        try {
            log.info("열기 비밀번호, 시인증: {}", tempToken);

            // 1. 에서저장중가져오기회원가입정보
            String cacheKey = TEMP_TOKEN_PREFIX + tempToken;
            Object cachedData = RedisUtils.get(cacheKey);

            if (cachedData == null) {
                throw new ServiceException("시인증완료경과또는없음, 요청다시 회원가입");
            }

            // 2. 파싱저장의데이터
            Map<String, Object> dataMap = objectMapper.readValue(
                    cachedData.toString(),
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));

            RegisterDto registerDto = objectMapper.convertValue(dataMap.get("registerDto"), RegisterDto.class);
            String phone;
            if (registerDto == null) {
                phone = (String) dataMap.get("phone");
            } else {
                phone = registerDto.getPhone();
            }
            String loginName = phone;

            log.info("비밀번호, 휴대폰 번호: {}, 로그인이름: {}", phone, loginName);

            // 3. 업데이트Shoprpa계정비밀번호
            updateIflytekPassword(loginName, password);

            // 4. 업데이트UAP비밀번호, login연결중사용의UAP비밀로그인, 으로일
            // 해당방법법내부모듈필요사용비밀번호수정, 원인로비밀번호아니요정상업데이트실패
            //            updateUapPassword(loginName, password);

            // 결과가 ext_info 로 1(사용자), 업데이트로 0
            String extInfo = userDao.queryExtInfoByPhone(phone, databaseName);
            if ("1".equals(extInfo)) {
                userDao.updateExtInfo(loginName, "0", databaseName);
            }

            log.info("비밀번호성공");
            return true;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("비밀번호, 시인증: {}", tempToken, e);
            throw new ServiceException("비밀번호예외: " + e.getMessage());
        }
    }

    /**
     * 업데이트Shoprpa계정비밀번호(회원가입후비밀번호)
     * 회원가입시사용의예빈비밀번호, 으로비밀번호사용빈문자열
     */
    private void updateIflytekPassword(String loginName, String newPassword) {
        try {
            log.info("업데이트Shoprpa계정비밀번호, 로그인이름: {}", loginName);

            // 회원가입시결과가있음비밀번호, 사용의예빈문자열
            // 으로비밀번호사용빈문자열
            String oldPassword = "";
            String oldPwdMd5 = oldPassword;
            String newPwdMd5 = toMd5Hex(newPassword);

            CAccountClient client = getAccountClient();
            String traceId = generateTraceId();
            byte[] requestBody = buildUpdatePasswordRequest(loginName, traceId, oldPwdMd5, newPwdMd5);
            byte[] responseBytes = executePost(client, UPDATE_PASSWORD_PATH, requestBody, "업데이트Shoprpa계정비밀번호");
            IflytekAccountResponse<Void> responseDto = parseResponse(responseBytes, Void.class);
            String code = responseDto.getCode();

            if (isSuccessCode(code)) {
                log.info("업데이트Shoprpa계정비밀번호성공, 로그인이름: {}", loginName);
                return;
            }

            if ("0100100".equals(code)) {
                throw new ServiceException("업데이트비밀번호실패: 매개변수오류");
            }
            if ("0402200".equals(code)) {
                throw new ServiceException("계정찾을 수 없습니다");
            }

            throw new ServiceException("업데이트비밀번호실패: " + responseDto.getDesc());

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("업데이트Shoprpa계정비밀번호실패, 로그인이름: {}", loginName, e);
            throw new ServiceException("업데이트Shoprpa계정비밀번호실패: " + e.getMessage());
        }
    }

    /**
     * 업데이트UAP비밀번호
     */
    private void updateUapPassword(String loginName, String newPassword) {
        try {
            log.info("업데이트UAP비밀번호, 로그인이름: {}", loginName);

            // 호출UserService의비밀번호업데이트
            // 사용비밀번호로비밀번호
            userService.updatePasswordAfterRegister(loginName, newPassword);

        } catch (Exception e) {
            log.error("업데이트UAP비밀번호실패", e);
            throw new ServiceException("업데이트UAP비밀번호실패: " + e.getMessage());
        }
    }

    /**
     * 가져오기인증 코드
     *
     * @param phone 로그인이름
     * @return 인증 코드
     */
    @Override
    public String getVerificationCode(String phone, String scene) {
        try {
            CAccountClient client = getAccountClient();
            String traceId = generateTraceId();
            byte[] requestBody = buildSendMsgCodeRequest(phone, traceId, DEFAULT_SMS_EXPIRE_SECONDS);
            byte[] responseBytes = executePost(client, SEND_MSG_CODE_PATH, requestBody, "가져오기인증 코드");
            IflytekAccountResponse<Void> responseDto = parseResponse(responseBytes, Void.class);
            if ("000000".equals(responseDto.getCode())) {
                // 를인증 코드key저장까지Redis, 확인가능사용일(아니요저장인증 코드값, 원인로불가가져오기)
                String cacheKey = buildVerifyCodeKey(phone, scene);
                RedisUtils.set(cacheKey, "1", DEFAULT_SMS_EXPIRE_SECONDS);
                log.info("인증 코드완료전송, key완료저장까지Redis, 휴대폰 번호: {}, : {}", phone, cacheKey);
                return responseDto.getDesc();
            }
            log.error("가져오기인증 코드실패, 오류코드: {}, 오류정보: {}", responseDto.getCode(), responseDto.getDesc());
            throw new ServiceException("가져오기인증 코드실패: " + responseDto.getDesc());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("가져오기인증 코드예외, 로그인이름: {}", phone, e);
            throw new ServiceException("가져오기인증 코드예외: " + e.getMessage());
        }
    }

    @Override
    public String getVerificationCode(String phone) {
        return getVerificationCode(phone, AuthenticationService.SCENE_LOGIN);
    }

    /**
     * 인증인증 코드
     *
     * @param phone 로그인휴대폰 번호
     * @param code  인증 코드
     * @return 검증여부성공
     */
    public boolean verifyCode(String phone, String code, String scene) {
        if (!StringUtils.hasText(phone)) {
            throw new ServiceException("휴대폰 번호는 비워 둘 수 없습니다");
        }
        if (!StringUtils.hasText(code)) {
            throw new ServiceException("인증 코드는 비워 둘 수 없습니다");
        }
        try {
            // 에서Redis중조회인증 코드key여부저장에서(확인가능사용일)
            String cacheKey = buildVerifyCodeKey(phone, scene);
            if (!RedisUtils.hasKey(cacheKey)) {
                log.warn("인증 코드찾을 수 없습니다또는완료사용, 휴대폰 번호: {}, : {}", phone, scene);
                throw new ServiceException("인증 코드찾을 수 없습니다또는완료사용");
            }

            // 인증 코드key저장에서, 호출아래서비스인증
            CAccountClient client = getAccountClient();
            String traceId = generateTraceId();
            byte[] requestBody = buildVerifyCodeRequest(phone, code, traceId);
            byte[] responseBytes = executePost(client, VERIFY_CODE_PATH, requestBody, "인증인증 코드");
            IflytekAccountResponse<Void> responseDto = parseResponse(responseBytes, Void.class);

            if ("000000".equals(responseDto.getCode())) {
                // 인증 성공, 삭제Redis중의인증 코드key(확인가능사용일)
                RedisUtils.del(cacheKey);
                log.info("인증 코드인증 성공삭제됨, 휴대폰 번호: {}, : {}", phone, scene);
                return true;
            }

            log.warn("인증인증 코드실패, 오류코드: {}, 오류정보: {}", responseDto.getCode(), responseDto.getDesc());
            return false;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("인증인증 코드예외, 휴대폰 번호: {}", phone, e);
            throw new ServiceException("인증인증 코드예외: " + e.getMessage());
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

    private void ensurePhoneRegistered(String phone, HttpServletRequest request) {
        if (!StringUtils.hasText(phone)) {
            throw new ServiceException("휴대폰 번호는 비워 둘 수 없습니다");
        }
        if (queryUserExist(phone)) {
            return;
        }
        RegisterDto autoRegisterDto = RegisterDto.builder().phone(phone).build();
        String userId = register(autoRegisterDto, request);
        if (!StringUtils.hasText(userId)) {
            throw new ServiceException("회원가입실패");
        }
        log.info("휴대폰 번호 {} 미완료회원가입, 완료회원가입, 사용자ID {}", phone, userId);
    }

    /**
     * 조회사용자 정보
     *
     * @param loginName 로그인이름
     * @return 사용자 정보
     */
    @Override
    public boolean queryUserExist(String loginName) {
        try {
            CAccountClient client = getAccountClient();
            String traceId = generateTraceId();
            byte[] requestBody = buildCheckLoginIdRequest(loginName, traceId);
            byte[] responseBytes = executePost(client, CHECK_LOGIN_ID_PATH, requestBody, "조회사용자 정보");
            IflytekAccountResponse<IflytekCheckLoginIdData> responseDto =
                    parseResponse(responseBytes, IflytekCheckLoginIdData.class);
            if ("000000".equals(responseDto.getCode())) {
                return responseDto.getData() != null && responseDto.getData().isExist();
            }
            log.error("조회사용자 정보실패, 오류코드: {}, 오류정보: {}", responseDto.getCode(), responseDto.getDesc());
            return false;
        } catch (Exception e) {
            log.error("조회사용자 정보예외, 로그인이름: {}", loginName, e);
            throw new ServiceException("조회사용자 정보예외: " + e.getMessage());
        }
    }

    /**
     * 생성checkLoginID요청 
     *
     * @param loginName 로그인이름(휴대폰 번호)
     * @param traceId   ID
     * @return 요청 JSON문자열
     */
    private byte[] buildCheckLoginIdRequest(String loginName, String traceId) {
        try {
            IflytekAccountRequest<IflytekCheckLoginIdParam> request = new IflytekAccountRequest<>(
                    new IflytekAccountBase(appid, traceId),
                    new IflytekCheckLoginIdParam(loginName, DEFAULT_COUNTRY_CODE, DEFAULT_LOGIN_TYPE));
            return objectMapper.writeValueAsBytes(request);
        } catch (Exception e) {
            log.error("생성요청 실패", e);
            throw new ServiceException("생성요청 실패: " + e.getMessage());
        }
    }

    private byte[] buildSendMsgCodeRequest(String phone, String traceId, int expireSeconds) {
        try {
            IflytekAccountRequest<IflytekSendMsgCodeParam> request = new IflytekAccountRequest<>(
                    new IflytekAccountBase(appid, traceId),
                    new IflytekSendMsgCodeParam(DEFAULT_COUNTRY_CODE, phone, expireSeconds));
            return objectMapper.writeValueAsBytes(request);
        } catch (Exception e) {
            log.error("생성인증 코드요청 실패", e);
            throw new ServiceException("생성인증 코드요청 실패: " + e.getMessage());
        }
    }

    private byte[] buildVerifyCodeRequest(String phone, String code, String traceId) {
        try {
            IflytekAccountRequest<IflytekVerifyCodeParam> request = new IflytekAccountRequest<>(
                    new IflytekAccountBase(appid, traceId), new IflytekVerifyCodeParam(phone, code));
            return objectMapper.writeValueAsBytes(request);
        } catch (Exception e) {
            log.error("생성인증 코드검증요청 실패", e);
            throw new ServiceException("생성인증 코드검증요청 실패: " + e.getMessage());
        }
    }

    private <T> IflytekAccountResponse<T> parseResponse(byte[] responseBytes, Class<T> dataClass) {
        try {
            return objectMapper.readValue(
                    responseBytes,
                    objectMapper.getTypeFactory().constructParametricType(IflytekAccountResponse.class, dataClass));
        } catch (Exception e) {
            log.error("파싱실패", e);
            throw new ServiceException("파싱실패: " + e.getMessage());
        }
    }

    private byte[] buildRegisterRequest(RegisterDto registerDto, String traceId) {
        try {
            String loginId = registerDto.getPhone();
            if (!StringUtils.hasText(loginId)) {
                throw new ServiceException("회원가입계정비워 둘 수 없습니다");
            }
            String password = StringUtils.isEmpty(registerDto.getPassword()) ? "" : toMd5Hex(registerDto.getPassword());
            IflytekRegisterParam param = new IflytekRegisterParam(
                    loginId, DEFAULT_LOGIN_TYPE, DEFAULT_COUNTRY_CODE, password, DEFAULT_PASSWORD_TYPE);
            IflytekAccountRequest<IflytekRegisterParam> request =
                    new IflytekAccountRequest<>(new IflytekAccountBase(appid, traceId), param);
            return objectMapper.writeValueAsBytes(request);
        } catch (Exception e) {
            log.error("생성회원가입요청 실패", e);
            throw new ServiceException("생성회원가입요청 실패: " + e.getMessage());
        }
    }

    private byte[] buildLoginRequest(LoginDto loginDto, String traceId, IflytekLoginModeEnum loginMode) {
        try {
            String loginId = resolveLoginId(loginDto.getLoginName(), loginDto.getPhone());
            boolean usePassword = loginMode == IflytekLoginModeEnum.PASSWORD;
            if (usePassword && !StringUtils.hasText(loginDto.getPassword())) {
                throw new ServiceException("로그인비밀번호는 비워 둘 수 없습니다");
            }
            String lgType = loginMode.getValue();
            String password = usePassword ? toMd5Hex(loginDto.getPassword()) : "";
            IflytekLoginParam param =
                    new IflytekLoginParam(loginId, DEFAULT_COUNTRY_CODE, lgType, password, DEFAULT_PASSWORD_TYPE);
            IflytekAccountRequest<IflytekLoginParam> request =
                    new IflytekAccountRequest<>(new IflytekAccountBase(appid, traceId), param);
            return objectMapper.writeValueAsBytes(request);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("생성로그인요청 실패", e);
            throw new ServiceException("생성로그인요청 실패: " + e.getMessage());
        }
    }

    private String resolveLoginId(String loginName, String phone) {
        //        if (StringUtils.hasText(loginName)) {
        //            return loginName;
        //        }
        if (StringUtils.hasText(phone)) {
            return phone;
        }
        throw new ServiceException("휴대폰 번호는 비워 둘 수 없습니다");
    }

    private IflytekLoginModeEnum resolveLoginMode(LoginDto loginDto) {
        if (loginDto.getLoginType() == LoginTypeEnum.PASSWORD) {
            return IflytekLoginModeEnum.PASSWORD;
        } else {
            return IflytekLoginModeEnum.FREE;
        }
    }

    private String toMd5Hex(String raw) {
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] buildUpdatePasswordRequest(String loginId, String traceId, String oldPwdMd5, String newPwdMd5) {
        try {
            IflytekAccountRequest<IflytekUpdatePasswordParam> request = new IflytekAccountRequest<>(
                    new IflytekAccountBase(appid, traceId),
                    new IflytekUpdatePasswordParam(
                            loginId, DEFAULT_COUNTRY_CODE, "phone", oldPwdMd5, newPwdMd5, DEFAULT_PASSWORD_TYPE));
            return objectMapper.writeValueAsBytes(request);
        } catch (Exception e) {
            log.error("생성수정비밀번호요청 실패", e);
            throw new ServiceException("생성수정비밀번호요청 실패: " + e.getMessage());
        }
    }

    private boolean isSuccessCode(String code) {
        return "000000".equals(code) || "00000".equals(code);
    }

    /**
     * 생성삭제사용자요청 
     *
     * @param userid  Shoprpa계정의 userid
     * @param traceId ID
     * @return 요청 문자배열
     */
    private byte[] buildDeleteUserRequest(String userid, String traceId) {
        try {
            IflytekAccountRequest<IflytekDeleteUserParam> request = new IflytekAccountRequest<>(
                    new IflytekAccountBase(appid, traceId), new IflytekDeleteUserParam(userid));
            return objectMapper.writeValueAsBytes(request);
        } catch (Exception e) {
            log.error("생성삭제사용자요청 실패", e);
            throw new ServiceException("생성삭제사용자요청 실패: " + e.getMessage());
        }
    }

    /**
     * 생성사용자 정보요청 
     *
     * @param userid        사용자ID
     * @param password      비밀번호(가능비어 있습니다)
     * @param loginAccounts 로그인계정목록
     * @param userInfo      사용자정보
     * @param traceId       ID
     * @return 요청 문자배열
     */
    private byte[] buildSyncUserInfoRequest(
            String userid,
            String password,
            List<IflytekSyncUserInfoAccount> loginAccounts,
            IflytekSyncUserInfoUserInfo userInfo,
            String traceId) {
        try {
            // 결과가비밀번호아니요비어 있습니다, 변환로MD5
            String passwordMd5 = StringUtils.hasText(password) ? toMd5Hex(password) : "";

            // 생성로그인정보
            IflytekSyncUserInfoLogin login = new IflytekSyncUserInfoLogin(loginAccounts);

            // 생성요청 매개변수
            IflytekSyncUserInfoParam param = new IflytekSyncUserInfoParam(userid, passwordMd5, login, userInfo);

            // 생성요청 
            IflytekAccountRequest<IflytekSyncUserInfoParam> request =
                    new IflytekAccountRequest<>(new IflytekAccountBase(appid, traceId), param);

            return objectMapper.writeValueAsBytes(request);
        } catch (Exception e) {
            log.error("생성사용자 정보요청 실패", e);
            throw new ServiceException("생성사용자 정보요청 실패: " + e.getMessage());
        }
    }

    /**
     * Shoprpa계정클라이언트
     * 사용 @PostConstruct 비고해제에서 Bean 후실행
     * 에서매요청 시재복사생성 client 
     */
    @javax.annotation.PostConstruct
    private void initAccountClient() {
        log.info("Shoprpa계정클라이언트, accountHost: {}", accountHost);
        this.accountClient =
                accountClientFactory.create(accountHost, TIME_OUT, accessKey, accessSecret, USE_AES_ENCRYPT);
    }

    /**
     * 가져오기Shoprpa계정클라이언트(단일복사사용)
     */
    private CAccountClient getAccountClient() {
        return accountClient;
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 가져오기현재서비스서버IP주소
     * 단계: 
     * 1. 본디버그방식 -> 사용매칭의본IP
     * 2. 매칭의서비스서버IP(rpa.auth.server-ip)-> 사용K8s대기내용기기
     * 3. 가져오기본기기IP -> 사용물품관리기기/기기모듈
     * 4. IP -> 방법
     */
    private String getServerIp() {
        // 1. 본디버그방식, 사용매칭의본IP
        if (localDebug) {
            log.debug("본디버그방식, 사용매칭의IP: {}", localDebugIp);
            return localDebugIp;
        }

        // 2. 결과가매칭완료서비스서버IP, 직선연결사용(사용K8s)
        if (serverIp != null && !serverIp.trim().isEmpty()) {
            log.debug("사용매칭의서비스서버IP: {}", serverIp);
            return serverIp;
        }

        // 3. 시도가져오기본기기IP(사용물품관리기기/기기모듈)
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                // 건너뛰기돌아가기연결및사용할 수 없습니다의연결
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    // 가져오기IPv4주소, 아니요예돌아가기주소
                    if (!inetAddress.isLoopbackAddress()
                            && inetAddress.getHostAddress().indexOf(':') == -1) {
                        String ip = inetAddress.getHostAddress();
                        log.debug("가져오기까지IP: {}", ip);
                        return ip;
                    }
                }
            }
        } catch (SocketException e) {
            log.error("가져오기IP실패", e);
        }

        // 4. 결과가가져오기실패, 반환값
        log.warn("불가가져오기서비스서버IP, 사용값: {}", localDebugIp);
        return localDebugIp;
    }

    private byte[] executePost(CAccountClient client, String path, byte[] requestBody, String actionDesc) {
        try {
            String requestString = new String(requestBody, StandardCharsets.UTF_8);
            log.info("{}, 요청 경로: {}, 요청 : {}", actionDesc, path, requestString);
            Map<String, String> headers = new HashMap<>();
            // 근거가져오기IP주소: 본디버그사용매칭의IP, 서비스서버모듈사용IP
            String serverIp = getServerIp();
            headers.put("X-Forwarded-For", serverIp);
            log.debug("사용IP주소: {}", serverIp);
            CAccountResponse response = client.post(path, headers, requestBody);
            if (response.getHttpStatus() != 200) {
                log.error("{}실패, HTTP상태코드: {}, 오류정보: {}", actionDesc, response.getHttpStatus(), response.getErrorMessage());
                throw new ServiceException(actionDesc + "실패: " + response.getErrorMessage());
            }
            String responseData = new String(response.getData(), StandardCharsets.UTF_8);
            log.info("{}성공, 데이터: {}", actionDesc, responseData);
            return response.getData();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("{}예외", actionDesc, e);
            throw new ServiceException(actionDesc + "예외: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<String> logout(HttpServletRequest request, HttpServletResponse response) {
        return userService.logout(request, response);
    }

    /**
     * 새로고침token
     *
     * @param request     HTTP요청 
     * @param accessToken
     * @return
     */
    @Override
    public AppResponse<Boolean> refreshToken(HttpServletRequest request, String accessToken) {
        try {
            String refreshToken = Oauth2Util.getRefreshTokenFromRequest(request);
            if (org.apache.commons.lang3.StringUtils.isBlank(refreshToken)) {
                return AppResponse.success(false);
            }
            boolean flag = true;
            ResponseDto<LoginTokenResponseDto> responseDto =
                    ClientAuthenticationAPI.refreshToken(accessToken, refreshToken, null);
            if (responseDto.isFlag()) {
                String userFlag = null;
                if (ClientConfigUtil.instance().isUseSession()) {
                    userFlag = request.getSession().getId();
                } else {
                    userFlag = com.iflytek.sec.uap.client.util.ClientRequestUtil.getFlagFromRequest(request);
                }
                com.iflytek.sec.uap.client.util.CacheUtil.refreshAuthenticationToken(userFlag, responseDto.getData());
            } else {
                flag = false;
            }
            return AppResponse.success((flag));
        } catch (Exception e) {
            log.error("새로고침access token실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "새로고침access token실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<Boolean> checkSession(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 1. 검증session여부있음(UAP의AuthenticationFilter완료검증완료)

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
        // SaaS방식시지원하지 않음수정비밀번호
        throw new UnsupportedOperationException("SaaS방식시지원하지 않음수정비밀번호");
    }

    /**
     * 관리단일로그인: 지우기session, 저장새sessionId
     *
     * @param userId  사용자ID
     * @param request HTTP요청 
     */
    private void handleSingleSignOn(String userId, HttpServletRequest request) {
        try {
            if (StringUtils.isEmpty(userId)) {
                return;
            }

            // 가져오기현재sessionId
            String currentSessionId = request.getSession().getId();
            if (StringUtils.isEmpty(currentSessionId)) {
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
            // 비고: 아니요TTL, 에서session경과시실패, 또는일개길이의TTL
            RedisUtils.set(redisKey, currentSessionId, 2592000); // 30

            log.debug("단일로그인session완료업데이트, 사용자ID: {}, sessionId: {}", userId, currentSessionId);

        } catch (Exception e) {
            log.error("관리단일로그인실패, 사용자ID: {}", userId, e);
            // 아니요출력예외, 로그인프로세스
        }
    }

    /**
     * 제어-직선연결추가사용자
     */
    @Override
    public AppResponse<String> addUser(AddUserDto userDto, HttpServletRequest request) {
        String phone = userDto.getPhone();
        userDto.setPassword(DEFAULT_INITIAL_PASSWORD);
        userDto.setConfirmPassword(DEFAULT_INITIAL_PASSWORD);
        if (queryUserExist(phone)) {
            String loginName = userDao.queryLoginNameByPhone(phone, databaseName);
            if (StringUtils.hasText(loginName)) {
                userService.addUser(userDto, request);
            } else {
                userService.doBindTenantRoleDept(userDto, request);
            }
        } else {
            String name = userDto.getName();
            RegisterDto registerDto = RegisterDto.builder().build();
            BeanUtils.copyProperties(userDto, registerDto);
            registerDto.setLoginName(name);
            registerWithoutCaptcha(registerDto, request);
            userService.doBindTenantRoleDept(userDto, request);
        }
        return AppResponse.success("추가성공");
    }
}