package com.iflytek.rpa.auth.sp.casdoor.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iflytek.rpa.auth.core.entity.*;
import com.iflytek.rpa.auth.core.service.UserService;
import com.iflytek.rpa.auth.sp.casdoor.dao.CasdoorGroupDao;
import com.iflytek.rpa.auth.sp.casdoor.dao.CasdoorUserDao;
import com.iflytek.rpa.auth.sp.casdoor.dao.MarketUserDao;
import com.iflytek.rpa.auth.sp.casdoor.entity.CasdoorLoginResult;
import com.iflytek.rpa.auth.sp.casdoor.entity.CasdoorSignupDto;
import com.iflytek.rpa.auth.sp.casdoor.mapper.CasdoorOrganizationMapper;
import com.iflytek.rpa.auth.sp.casdoor.mapper.CasdoorUserMapper;
import com.iflytek.rpa.auth.sp.casdoor.service.extend.CasdoorAuthExtendService;
import com.iflytek.rpa.auth.sp.casdoor.service.extend.CasdoorGroupExtendService;
import com.iflytek.rpa.auth.sp.casdoor.service.extend.CasdoorLoginExtendService;
import com.iflytek.rpa.auth.sp.casdoor.service.extend.CasdoorUserExtendService;
import com.iflytek.rpa.auth.sp.casdoor.utils.SessionUserUtils;
import com.iflytek.rpa.auth.sp.casdoor.utils.TokenManager;
import com.iflytek.rpa.auth.utils.AppResponse;
import com.iflytek.rpa.auth.utils.ErrorCodeEnum;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.casbin.casdoor.entity.Group;
import org.casbin.casdoor.entity.User;
import org.casbin.casdoor.service.RoleService;
import org.casbin.casdoor.util.http.CasdoorResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service("casdoorUserService")
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "casdoor", matchIfMissing = true)
public class CasdoorUserServiceImpl implements UserService {

    @Autowired
    private org.casbin.casdoor.service.UserService userService;

    @Autowired
    private CasdoorUserExtendService userExtendService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private CasdoorUserMapper userMapper;

    @Autowired
    private com.iflytek.rpa.auth.sp.casdoor.mapper.CasdoorPermissionMapper permissionMapper;

    @Autowired
    private CasdoorOrganizationMapper organizationMapper;

    @Value("${casdoor.database.name:casdoor}")
    private String databaseName;

    @Autowired
    private CasdoorUserDao casdoorUserDao;

    @Autowired
    private MarketUserDao marketUserDao;

    @Autowired
    private CasdoorGroupDao casdoorGroupDao;

    @Autowired
    private CasdoorAuthExtendService casdoorAuthExtendService;

    @Autowired
    private CasdoorLoginExtendService casdoorLoginExtendService;

    @Value("${casdoor.organization-name}")
    private String organizationName;

    @Value("${casdoor.application-name}")
    private String applicationName;

    @Value("${casdoor.external-endpoint:}")
    private String externalEndPoint;

    @Value("${casdoor.redirect-url:}")
    private String redirectUrl;

    @Value("${casdoor.certificate:}")
    private String certificate;

    @Autowired
    private CasdoorGroupExtendService casdoorGroupExtendService;

    @Autowired
    private com.iflytek.rpa.auth.sp.casdoor.service.extend.CasdoorAccountExtendService casdoorAccountExtendService;

    /**
     * 회원가입
     * @param registerDto 회원가입정보
     * @param request HTTP요청 
     * @return 회원가입결과
     */
    @Override
    public AppResponse<String> register(RegisterDto registerDto, HttpServletRequest request) throws IOException {
        try {
            log.debug("열기 사용자회원가입");

            // 매개변수검증
            if (registerDto == null) {
                log.warn("사용자회원가입실패: 회원가입매개변수가 비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "회원가입매개변수비워 둘 수 없습니다");
            }

            if (StringUtils.isBlank(registerDto.getPassword())) {
                log.warn("사용자회원가입실패: 비밀번호비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "비밀번호는 비워 둘 수 없습니다");
            }

            // 인증비밀번호여부일
            if (!StringUtils.equals(registerDto.getPassword(), registerDto.getConfirmPassword())) {
                log.warn("사용자회원가입실패: 입력한 비밀번호가 올바르지 않습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM, "입력한 비밀번호가 올바르지 않습니다");
            }

            String phone = registerDto.getPhone().trim();
            String loginName = registerDto.getLoginName();
            log.debug("사용자회원가입매개변수, 휴대폰 번호: {}, 로그인이름: {}", phone, loginName);

            // 결과가있음로그인이름, 사용휴대폰 번호로로그인이름
            String username = StringUtils.isNotBlank(loginName) ? loginName.trim() : phone;
            log.debug("지정사용자명로: {}", username);

            // 생성Casdoor회원가입요청 
            CasdoorSignupDto casdoorSignupDto = new CasdoorSignupDto();
            casdoorSignupDto.setApplication(applicationName);
            casdoorSignupDto.setOrganization(organizationName);
            casdoorSignupDto.setUsername(username);
            casdoorSignupDto.setName(username);
            casdoorSignupDto.setPassword(registerDto.getPassword());

            // 호출Casdoor회원가입연결
            log.debug("호출Casdoor API회원가입사용자, username: {}", username);
            CasdoorLoginResult signupResult = casdoorLoginExtendService.signup(casdoorSignupDto);

            if (signupResult == null) {
                log.error("사용자회원가입실패: Casdoor API반환비어 있습니다, username: {}", username);
                return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "사용자회원가입실패: API반환비어 있습니다");
            }

            if (StringUtils.isBlank(signupResult.getUserId())) {
                log.error("사용자회원가입실패: 가져올 수 없는 사용자ID, username: {}", username);
                return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "사용자회원가입실패: 가져올 수 없는 사용자ID");
            }

            String userId = signupResult.getUserId();
            log.debug("사용자회원가입성공, userId: {}, username: {}, phone: {}", userId, username, phone);
            return AppResponse.success(userId);
        } catch (IOException e) {
            log.error("사용자회원가입실패(IO예외)", e);
            return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "사용자회원가입실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("사용자회원가입예외", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "사용자회원가입예외: " + e.getMessage());
        }
    }

    /**
     * 이름검색모든요소또는모듈
     * @param name 검색닫기 문자
     * @param request HTTP요청 
     * @return 검색결과
     */
    @Override
    public AppResponse<GetDeptOrUserDto> searchDeptOrUser(String name, HttpServletRequest request) {
        try {
            log.debug("열기 이름검색요소또는모듈, name: {}", name);

            // 매개변수검증
            if (name == null || name.trim().isEmpty()) {
                log.warn("이름검색요소또는모듈실패: 검색닫기 문자비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "검색어는 비워 둘 수 없습니다");
            }

            // 가져오기현재테넌트ID(owner), 사용제한지정조회
            String owner = getCurrentTenantOwner(request);

            // 조회사용자(제한지정에서현재테넌트아래, 결과가owner비어 있습니다이면조회전체)
            List<User> users = casdoorUserDao.searchUserByName(name, owner, databaseName);
            if (users == null) {
                users = Collections.emptyList();
            }
            log.debug("조회까지 {} 개사용자, name: {}", users.size(), name);

            // 변환로통신사용사용자객체목록, 필터링변환실패의객체
            List<com.iflytek.rpa.auth.core.entity.User> commonUserList = users.stream()
                    .filter(user -> user != null)
                    .map(user -> {
                        try {
                            return userMapper.toCommonUser(user);
                        } catch (Exception e) {
                            log.warn("사용자 정보변환실패, userId: {}, name: {}", user != null ? user.id : "null", name, e);
                            return null;
                        }
                    })
                    .filter(user -> user != null)
                    .collect(Collectors.toList());
            log.debug("성공변환 {} 개사용자, name: {}", commonUserList.size(), name);

            // 조회모듈(에서casdoor중group, 제한지정에서현재테넌트아래, 결과가owner비어 있습니다이면조회전체)
            List<Group> groups = casdoorGroupDao.searchDeptByName(name, owner, databaseName);
            if (groups == null) {
                groups = Collections.emptyList();
            }
            log.debug("조회까지 {} 개모듈, name: {}", groups.size(), name);

            // 변환로통신사용모듈객체목록, 필터링변환실패의객체
            List<Org> commonOrgList = groups.stream()
                    .filter(group -> group != null)
                    .map(group -> {
                        try {
                            return organizationMapper.toCommonOrg(group);
                        } catch (Exception e) {
                            log.warn("모듈정보변환실패, groupId: {}, name: {}", group != null ? group.name : "null", name, e);
                            return null;
                        }
                    })
                    .filter(org -> org != null)
                    .collect(Collectors.toList());
            log.debug("성공변환 {} 개모듈, name: {}", commonOrgList.size(), name);

            // 그룹설치까지DTO
            GetDeptOrUserDto getDeptOrUserDto = new GetDeptOrUserDto();
            getDeptOrUserDto.setUserList(commonUserList);
            getDeptOrUserDto.setDeptList(commonOrgList);

            log.debug("이름검색요소또는모듈성공, 사용자데이터: {}, 모듈데이터: {}, name: {}", commonUserList.size(), commonOrgList.size(), name);
            return AppResponse.success(getDeptOrUserDto);
        } catch (Exception e) {
            log.error("이름검색요소또는모듈예외, name: {}", name, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "이름검색요소또는모듈실패: " + e.getMessage());
        }
    }

    /**
     * 요소
     * @param updateUapUserDto 업데이트원정보
     * @param request HTTP요청 
     * @return 결과
     */
    @Override
    public AppResponse<String> editUser(UpdateUapUserDto updateUapUserDto, HttpServletRequest request)
            throws IOException {
        try {
            log.debug(
                    "열기 요소, userId: {}",
                    updateUapUserDto.getUser() != null
                            ? updateUapUserDto.getUser().getId()
                            : "null");

            // 매개변수검증
            if (updateUapUserDto == null || updateUapUserDto.getUser() == null) {
                log.warn("요소실패: 매개변수가 비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "업데이트사용자 정보비워 둘 수 없습니다");
            }

            UpdateUserDto updateUserDto = updateUapUserDto.getUser();
            if (updateUserDto.getId() == null || updateUserDto.getId().trim().isEmpty()) {
                log.warn("요소실패: 사용자ID비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "사용자 ID는 비워 둘 수 없습니다");
            }

            // 조회있음사용자 정보, 보관일값
            User existingUser = null;
            try {
                existingUser = userExtendService.getUserById(updateUserDto.getId());
            } catch (Exception e) {
                log.warn("조회있음사용자 정보실패, userId: {}", updateUserDto.getId(), e);
            }

            // 정보변환까지Casdoor User, 비고: 정보목록전지원하지 않음변환
            User userToUpdate = convertUpdateUserDtoToCasdoorUser(updateUserDto, existingUser, request);

            // 업데이트사용자
            log.debug("호출Casdoor API업데이트사용자, userId: {}", userToUpdate.id);
            CasdoorResponse<String, Object> updateUserResponse = userExtendService.updateUser(userToUpdate);

            if (updateUserResponse == null) {
                log.error("업데이트사용자실패: Casdoor API반환비어 있습니다, userId: {}", userToUpdate.id);
                return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "업데이트사용자실패: API반환비어 있습니다");
            }

            if (updateUserResponse.getStatus() != null && !"ok".equals(updateUserResponse.getStatus())) {
                log.error(
                        "업데이트사용자실패: Casdoor API반환오류, userId: {}, status: {}, msg: {}",
                        userToUpdate.id,
                        updateUserResponse.getStatus(),
                        updateUserResponse.getMsg());
                return AppResponse.error(
                        ErrorCodeEnum.E_API_EXCEPTION,
                        "업데이트사용자실패: " + (updateUserResponse.getMsg() != null ? updateUserResponse.getMsg() : "지원하지 않는오류"));
            }

            log.debug("업데이트사용자성공, userId: {}", userToUpdate.id);
            return AppResponse.success("업데이트사용자성공");
        } catch (IOException e) {
            log.error("요소실패", e);
            return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "요소실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("요소예외", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "요소예외: " + e.getMessage());
        }
    }

    /**
     * 를UpdateUserDto변환로Casdoor User(업데이트사용자서비스사용)
     * 비고: 정보목록전지원하지 않음변환
     *
     * @param updateUserDto 업데이트사용자DTO
     * @param existingUser 있음사용자 정보(사용보관미완료업데이트의필드)
     * @param request HTTP요청 (사용에서session가져오기테넌트ID)
     * @return Casdoor User
     */
    private User convertUpdateUserDtoToCasdoorUser(
            UpdateUserDto updateUserDto, User existingUser, HttpServletRequest request) {
        User userToUpdate = new User();

        // 본필드
        userToUpdate.id = updateUserDto.getId();
        userToUpdate.name = updateUserDto.getLoginName() != null
                ? updateUserDto.getLoginName()
                : (existingUser != null ? existingUser.name : "");
        userToUpdate.displayName = updateUserDto.getName() != null
                ? updateUserDto.getName()
                : (existingUser != null ? existingUser.displayName : "");
        userToUpdate.phone = updateUserDto.getPhone() != null
                ? updateUserDto.getPhone()
                : (existingUser != null ? existingUser.phone : "");
        userToUpdate.email = updateUserDto.getEmail() != null
                ? updateUserDto.getEmail()
                : (existingUser != null ? existingUser.email : null);

        // 사용자유형변환: userType -> isAdmin, isGlobalAdmin
        // SUPER_ADMIN(1), SYSTEM_ADMIN(2), NORMAL_USER(-1), RESOURCE_POOL_USER(3), TENANT_SUPER_ADMIN(0)
        if (updateUserDto.getUserType() != null) {
            Integer userType = updateUserDto.getUserType();
            // SUPER_ADMIN(1) 또는 TENANT_SUPER_ADMIN(0) 또는 SYSTEM_ADMIN(2) 로관리관리원
            userToUpdate.isAdmin = (userType == 1 || userType == 0 || userType == 2);
            // SUPER_ADMIN(1) 로전체영역관리관리원
            userToUpdate.isGlobalAdmin = (userType == 1);
        } else if (existingUser != null) {
            userToUpdate.isAdmin = existingUser.isAdmin;
            userToUpdate.isGlobalAdmin = existingUser.isGlobalAdmin;
        }

        // 상태필드변환: status (0중지사용 -> isForbidden=true, 1사용 -> isForbidden=false)
        if (updateUserDto.getStatus() != null) {
            userToUpdate.isForbidden = (updateUserDto.getStatus() == 0);
        } else if (existingUser != null) {
            userToUpdate.isForbidden = existingUser.isForbidden;
        }

        // owner필드보관있음값(owner예테넌트ID, Casdoor의Organization, 할 수 없음에서orgId가져오기)
        // 비고: orgId의예Casdoor의Group, 아니요예Organization, 으로할 수 없음를orgId값owner
        if (existingUser != null && existingUser.owner != null) {
            userToUpdate.owner = existingUser.owner;
        } else {
            // 결과가있음있음사용자 정보, 시도에서현재로그인사용자가져오기테넌트ID(owner)
            String owner = SessionUserUtils.getTenantOwnerFromSession(request);
            if (owner != null && !owner.trim().isEmpty()) {
                userToUpdate.owner = owner;
                log.debug("에서현재로그인사용자가져오기테넌트ID(owner): {}", owner);
            } else {
                log.warn("업데이트사용자시owner비어 있습니다, 불가에서현재로그인사용자가져오기테넌트ID, 가능가져오기 업데이트실패");
            }
        }

        // 주소필드변환: String -> String[]
        if (updateUserDto.getAddress() != null
                && !updateUserDto.getAddress().trim().isEmpty()) {
            String address = updateUserDto.getAddress().trim();
            if (address.contains(",")) {
                userToUpdate.address = Arrays.stream(address.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toArray(String[]::new);
            } else {
                userToUpdate.address = new String[] {address};
            }
            userToUpdate.location = address;
        } else if (existingUser != null && existingUser.address != null) {
            userToUpdate.address = existingUser.address;
            userToUpdate.location = existingUser.location;
        }

        // 비고필드까지bio
        if (updateUserDto.getRemark() != null
                && !updateUserDto.getRemark().trim().isEmpty()) {
            userToUpdate.bio = updateUserDto.getRemark();
        } else if (existingUser != null && existingUser.bio != null) {
            userToUpdate.bio = existingUser.bio;
        }

        // 인증
        if (updateUserDto.getIdNumber() != null
                && !updateUserDto.getIdNumber().trim().isEmpty()) {
            userToUpdate.idCard = updateUserDto.getIdNumber();
        } else if (existingUser != null && existingUser.idCard != null) {
            userToUpdate.idCard = existingUser.idCard;
        }

        // 일필드변환: Date -> String (yyyy-MM-dd)
        if (updateUserDto.getBirthday() != null) {
            userToUpdate.birthday = formatDate(updateUserDto.getBirthday());
        } else if (existingUser != null && existingUser.birthday != null) {
            userToUpdate.birthday = existingUser.birthday;
        }

        // 보관있음사용자의시간및필드
        if (existingUser != null) {
            userToUpdate.createdTime = existingUser.createdTime;
            userToUpdate.updatedTime = formatDateTime(new Date()); // 수정 시간로현재시간
            userToUpdate.type = existingUser.type != null ? existingUser.type : "normal-user";
            userToUpdate.password = existingUser.password != null ? existingUser.password : "";
            userToUpdate.passwordSalt = existingUser.passwordSalt != null ? existingUser.passwordSalt : "";
            userToUpdate.isDeleted = existingUser.isDeleted;
            userToUpdate.emailVerified = existingUser.emailVerified;
            userToUpdate.properties = existingUser.properties;
            userToUpdate.roles = existingUser.roles;
            userToUpdate.permissions = existingUser.permissions;
        } else {
            // 결과가조회아니요까지있음사용자, 값
            userToUpdate.type = "normal-user";
            userToUpdate.password = "";
            userToUpdate.passwordSalt = "";
            userToUpdate.isDeleted = false;
            userToUpdate.emailVerified = false;
            userToUpdate.createdTime = formatDateTime(new Date());
            userToUpdate.updatedTime = formatDateTime(new Date());
        }

        return userToUpdate;
    }

    /**
     * 에서현재로그인사용자가져오기테넌트ID(Casdoor중의 owner)
     *
     * @param request HTTP요청 
     * @return 테넌트ID(owner), 가져오기실패시반환null
     */
    private String getCurrentTenantOwner(HttpServletRequest request) {
        return SessionUserUtils.getTenantOwnerFromSession(request);
    }

    /**
     * 형식Date객체로날짜문자열 (yyyy-MM-dd)
     */
    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }

    /**
     * 형식Date객체로날짜시간문자열 (yyyy-MM-dd HH:mm:ss)
     */
    private String formatDateTime(Date date) {
        if (date == null) {
            return "";
        }
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(date);
    }

    /**
     * 추가요소
     * @param createUapUserDto 추가원정보
     * @param request HTTP요청 
     * @return 추가결과
     */
    @Override
    public AppResponse<String> addUser(CreateUapUserDto createUapUserDto, HttpServletRequest request)
            throws IOException {
        try {
            log.debug("열기 추가요소");

            // 매개변수검증
            if (createUapUserDto == null || createUapUserDto.getUser() == null) {
                log.warn("추가요소실패: 매개변수가 비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "추가사용자 정보비워 둘 수 없습니다");
            }

            CreateUserDto createUserDto = createUapUserDto.getUser();

            // 로그인이름
            if (createUserDto.getLoginName() == null
                    || createUserDto.getLoginName().trim().isEmpty()) {
                log.warn("추가요소실패: 로그인이름비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "로그인이름비워 둘 수 없습니다");
            }

            // 검증사용자여부완료저장에서(근거로그인이름)
            try {
                User existingUserByName = userExtendService.getUser(createUserDto.getLoginName());
                if (existingUserByName != null) {
                    log.warn("추가요소실패: 로그인이름완료저장에서, loginName: {}", createUserDto.getLoginName());
                    return AppResponse.error(ErrorCodeEnum.E_SERVICE, "로그인이름완료저장에서");
                }
            } catch (Exception e) {
                // 사용자를 찾을 수 없습니다예정상일반의, 계속실행
                log.debug("사용자를 찾을 수 없습니다(로그인이름), loginName: {}", createUserDto.getLoginName());
            }

            // 가져오기현재테넌트ID(owner), 비고: orgId의예Casdoor의Group, 아니요예Organization(테넌트)
            // ownerCasdoor의Organization name, 할 수 없음사용orgId
            String owner = getCurrentTenantOwner(request);

            if (owner == null || owner.trim().isEmpty()) {
                log.warn("추가요소실패: 테넌트ID비어 있습니다, 필요사용자완료로그인가능가져오기현재테넌트ID");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "테넌트 ID는 비워 둘 수 없습니다, 요청확인사용자완료로그인");
            }

            log.debug("추가요소, loginName: {}, owner: {}", createUserDto.getLoginName(), owner);

            // 정보변환까지Casdoor User, 비고: 정보목록전지원하지 않음변환
            User userToAdd = convertCreateUserDtoToCasdoorUser(createUserDto, owner);

            // 추가사용자
            log.debug("호출Casdoor API추가사용자, loginName: {}", userToAdd.name);
            CasdoorResponse<String, Object> addUserResponse = userExtendService.addUser(userToAdd);

            if (addUserResponse == null) {
                log.error("추가사용자실패: Casdoor API반환비어 있습니다, loginName: {}", userToAdd.name);
                return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "추가사용자실패: API반환비어 있습니다");
            }

            if (addUserResponse.getStatus() != null && !"ok".equals(addUserResponse.getStatus())) {
                log.error(
                        "추가사용자실패: Casdoor API반환오류, loginName: {}, status: {}, msg: {}",
                        userToAdd.name,
                        addUserResponse.getStatus(),
                        addUserResponse.getMsg());
                return AppResponse.error(
                        ErrorCodeEnum.E_API_EXCEPTION,
                        "추가사용자실패: " + (addUserResponse.getMsg() != null ? addUserResponse.getMsg() : "지원하지 않는오류"));
            }

            log.debug("추가사용자성공, loginName: {}", userToAdd.name);
            return AppResponse.success("추가사용자성공");
        } catch (IOException e) {
            log.error("추가요소실패", e);
            return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "추가요소실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("추가요소예외", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "추가요소예외: " + e.getMessage());
        }
    }

    /**
     * 를CreateUserDto변환로Casdoor User(추가사용자서비스사용)
     * 비고: 정보목록전지원하지 않음변환
     *
     * @param createUserDto 생성사용자DTO
     * @param owner 테넌트ID(organization name)
     * @return Casdoor User
     */
    private User convertCreateUserDtoToCasdoorUser(CreateUserDto createUserDto, String owner) {
        User userToAdd = new User();

        // owner및name(name로로그인이름)
        userToAdd.owner = owner;
        userToAdd.name = createUserDto.getLoginName() != null
                ? createUserDto.getLoginName().trim()
                : "";

        // 본필드
        userToAdd.displayName = createUserDto.getName() != null ? createUserDto.getName() : "";
        userToAdd.phone = createUserDto.getPhone() != null ? createUserDto.getPhone() : "";
        userToAdd.email = createUserDto.getEmail();

        // 사용자유형변환: userType -> isAdmin, isGlobalAdmin
        // SUPER_ADMIN(1), SYSTEM_ADMIN(2), NORMAL_USER(-1), RESOURCE_POOL_USER(3), TENANT_SUPER_ADMIN(0)
        if (createUserDto.getUserType() != null) {
            Integer userType = createUserDto.getUserType();
            // SUPER_ADMIN(1) 또는 TENANT_SUPER_ADMIN(0) 또는 SYSTEM_ADMIN(2) 로관리관리원
            userToAdd.isAdmin = (userType == 1 || userType == 0 || userType == 2);
            // SUPER_ADMIN(1) 로전체영역관리관리원
            userToAdd.isGlobalAdmin = (userType == 1);
        } else {
            // 값로통신사용자
            userToAdd.isAdmin = false;
            userToAdd.isGlobalAdmin = false;
        }

        // 상태필드변환: status (0중지사용 -> isForbidden=true, 1사용 -> isForbidden=false)
        if (createUserDto.getStatus() != null) {
            userToAdd.isForbidden = (createUserDto.getStatus() == 0);
        } else {
            // 사용
            userToAdd.isForbidden = false;
        }

        // 주소필드변환: String -> String[]
        if (createUserDto.getAddress() != null
                && !createUserDto.getAddress().trim().isEmpty()) {
            String address = createUserDto.getAddress().trim();
            if (address.contains(",")) {
                userToAdd.address = Arrays.stream(address.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toArray(String[]::new);
            } else {
                userToAdd.address = new String[] {address};
            }
            userToAdd.location = address;
        }

        // 비고필드까지bio
        if (createUserDto.getRemark() != null
                && !createUserDto.getRemark().trim().isEmpty()) {
            userToAdd.bio = createUserDto.getRemark();
        }

        // 인증
        if (createUserDto.getIdNumber() != null
                && !createUserDto.getIdNumber().trim().isEmpty()) {
            userToAdd.idCard = createUserDto.getIdNumber();
        }

        // 일필드변환: Date -> String (yyyy-MM-dd)
        if (createUserDto.getBirthday() != null) {
            userToAdd.birthday = formatDate(createUserDto.getBirthday());
        }

        // 값및새생성시간
        userToAdd.type = "normal-user";
        userToAdd.password = ""; // 새생성사용자시비밀번호비어 있습니다, 후가능필요단일비밀번호
        userToAdd.passwordSalt = "";
        userToAdd.isDeleted = false;
        userToAdd.emailVerified = false;
        userToAdd.createdTime = formatDateTime(new Date());
        userToAdd.updatedTime = formatDateTime(new Date());

        return userToAdd;
    }

    /**
     * 분조회현재기기의사용자
     * @param listUserDto 조회파일
     * @param request HTTP요청 
     * @return 분사용자목록
     */
    @Override
    public AppResponse<PageDto<DeptUserDto>> queryUserListByOrgId(ListUserDto listUserDto, HttpServletRequest request)
            throws IOException {
        // 조회매개변수queryMap, 가능선택, OrgId의기기(casdoor그룹)필터링
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("groupName", listUserDto.getOrgId());
        // listUserDto중사용까지완료데이터정보
        int pageNum = listUserDto.getPageNum() == null ? 1 : listUserDto.getPageNum();
        int pageSize = listUserDto.getPageSize() == null ? 100 : listUserDto.getPageSize();

        // 조회테넌트(casdoor의organization)아래사용자
        Map<String, Object> paginationUsers = userExtendService.getPaginationUsers(pageNum, pageSize, queryMap);
        List<User> userList = new ArrayList<>();
        if (!Objects.isNull(paginationUsers)) {
            // deptUserDtoList
            userList = (List<User>) paginationUsers.getOrDefault("casdoorUsers", Collections.emptyList());
        }

        Long totalCount = ((Number) paginationUsers.getOrDefault("data2", 0)).longValue();
        List<DeptUserDto> deptUserDtoList = new ArrayList<>();

        for (User user : userList) {
            DeptUserDto deptUserDto = new DeptUserDto();
            com.iflytek.rpa.auth.core.entity.User commonUser = userMapper.toCommonUser(user);
            BeanUtils.copyProperties(commonUser, deptUserDto);
            // 이름조회사용자 정보, 까지역할정보
            User userInfo = userExtendService.getUser(user.name);
            if (!CollectionUtils.isEmpty(userInfo.roles)) {
                if (userInfo.roles.size() > 1) {
                    return AppResponse.error(ErrorCodeEnum.E_SERVICE, "사용자저장에서다중개지정역할");
                }
                org.casbin.casdoor.entity.Role role = userInfo.roles.get(0);
                if (role != null) {
                    // 비고: casdoor중역할있음id있음name
                    deptUserDto.setRoleId(role.name);
                    deptUserDto.setRoleName(role.name);
                }
            }

            deptUserDtoList.add(deptUserDto);
        }

        // 그룹설치결과
        PageDto<DeptUserDto> deptUserPage = new PageDto<>();
        deptUserPage.setResult(deptUserDtoList);
        deptUserPage.setPageSize(pageSize);
        deptUserPage.setCurrentPageNo(pageNum);
        deptUserPage.setTotalCount(totalCount);
        return AppResponse.success(deptUserPage);
    }

    /**
     * 역할관리관리-근거모듈id조회모듈아래의사람원및모듈
     * @param id 모듈ID
     * @param request HTTP요청 
     * @return 모듈및사람원목록
     */
    @Override
    public AppResponse<List<CurrentDeptUserDto>> queryUserAndDept(String id, HttpServletRequest request) {
        try {
            log.debug("조회모듈아래의사람원및모듈, id: {}(Casdoor지원하지 않음공가능, 반환빈목록)", id);
            return AppResponse.success(Collections.emptyList());
        } catch (Exception e) {
            log.error("조회모듈아래의사람원및모듈예외, id: {}", id, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회모듈아래의사람원및모듈실패: " + e.getMessage());
        }
    }

    /**
     * 역할관리관리-근거이름문자또는휴대폰 번호조회요소
     * @param keyWord 검색닫기 문자
     * @param request HTTP요청 
     * @return 원목록
     */
    @Override
    public AppResponse<List<CurrentDeptUserDto>> searchUserWithStatus(String keyWord, HttpServletRequest request)
            throws IOException {
        // 가져오기현재테넌트ID(owner), 가능선택
        String owner = getCurrentTenantOwner(request);
        List<User> users = casdoorUserDao.searchUserByNameOrPhone(keyWord, owner, databaseName);
        List<CurrentDeptUserDto> currentDeptUserDtoList = new ArrayList<>();
        for (User user : users) {
            CurrentDeptUserDto currentDeptUserDto = new CurrentDeptUserDto();
            currentDeptUserDto.setId(user.id);
            currentDeptUserDto.setName(user.name + "(" + user.phone + ")");
            currentDeptUserDto.setType("user");
            User userInfo = userExtendService.getUser(user.name);
            if (!CollectionUtils.isEmpty(userInfo.roles)) {
                if (userInfo.roles.size() > 1) {
                    return AppResponse.error(ErrorCodeEnum.E_SERVICE, "사용자저장에서다중개지정역할");
                }
                org.casbin.casdoor.entity.Role role = userInfo.roles.get(0);
                currentDeptUserDto.setStatus(null != role);
            }
            currentDeptUserDtoList.add(currentDeptUserDto);
        }
        return AppResponse.success(currentDeptUserDtoList);
    }

    /**
     * 역할관리관리-추가구성원
     * @param bindUserListDto 지정사용자목록정보
     * @param request HTTP요청 
     * @return 지정결과
     */
    @Override
    public AppResponse<String> bindUserListRole(BindUserListDto bindUserListDto, HttpServletRequest request)
            throws IOException {
        if (StringUtils.isBlank(bindUserListDto.getRoleId()) || CollUtil.isEmpty(bindUserListDto.getUserIds())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        List<String> userIds = bindUserListDto.getUserIds();

        // 조회출력role정보, 필요업데이트역할의사용자id합치기, 사용호출업데이트연결
        org.casbin.casdoor.entity.Role roleInfoToUpdate = roleService.getRole(bindUserListDto.getRoleId());
        Set<String> userSetOfTargetRole = new HashSet<>(Arrays.asList(roleInfoToUpdate.roles));
        // 조회출력역할정보, 필요삭제역할의사용자id합치기, 사용호출업데이트연결지우기사용자의역할
        org.casbin.casdoor.entity.Role defaultRoleInfo = roleService.getRole("example-role");
        Set<String> userSetOfDefaultRole = new HashSet<>(Arrays.asList(defaultRoleInfo.roles));

        // 입력의사용자id목록
        for (String userId : userIds) {
            // 근거id조회사용자, 가져오기사용자지정역할정보
            User userById = userExtendService.getUserById(userId);
            // 조회사용자여부있음역할, 결과가있음, 해제역할
            String userIdForApi = userById.owner + "/" + userById.name;
            if (userSetOfDefaultRole.contains(userIdForApi)) {
                userSetOfDefaultRole.remove(userIdForApi);
            }
            // 추가까지필요업데이트역할의사용자id합치기
            userSetOfTargetRole.add(userIdForApi);
        }

        // 업데이트목록 역할및역할의user지정정보
        roleInfoToUpdate.roles = userSetOfTargetRole.toArray(new String[0]);
        CasdoorResponse<String, Object> updateRoleTargetResponse = roleService.updateRole(roleInfoToUpdate);
        defaultRoleInfo.roles = userSetOfDefaultRole.toArray(new String[0]);
        CasdoorResponse<String, Object> updateRoleDefaultResponse = roleService.updateRole(defaultRoleInfo);

        return AppResponse.success("지정역할성공");
    }

    /**
     * 사람원해제역할
     * @param bindRoleDto 해제역할정보
     * @param request HTTP요청 
     * @return 해제결과
     */
    @Override
    public AppResponse<String> unbindRole(BindRoleDto bindRoleDto, HttpServletRequest request) throws IOException {
        List<String> roleIdList = bindRoleDto.getRoleIdList();
        // 근거id조회출력사용자의정보, 사용합치기성공casdoor가능의id(owner/name)
        User targetUser = userExtendService.getUser(bindRoleDto.getUserId());
        String idForApi = targetUser.owner + "/" + targetUser.name;
        // 조회매개역할의역할정보, 가져오기기존users, 에서중목록사용자, 업데이트역할정보.(casdoor의역할해제예으로역할로의)
        for (String roleId : roleIdList) {
            org.casbin.casdoor.entity.Role role = roleService.getRole(roleId);
            List<String> usersToUpdate = new ArrayList<>();
            for (String userId : role.users) {
                if (!StringUtils.equals(userId, idForApi)) {
                    usersToUpdate.add(userId);
                }
            }
            role.users = usersToUpdate.toArray(new String[0]);
            CasdoorResponse<String, Object> updateRoleCasdoorResponse = roleService.updateRole(role);
        }
        return AppResponse.success("해제역할성공");
    }

    /**
     * 분가져오기역할지정의사용자목록, 가능근거로그인이름또는이름조회(casdoor지원하지 않음분조회역할아래의사용자목록, 분)
     * @param listUserByRoleDto 조회파일
     * @param request HTTP요청 
     * @return 분사용자목록
     */
    @Override
    public AppResponse<PageDto<com.iflytek.rpa.auth.core.entity.User>> queryBindListByRole(
            ListUserByRoleDto listUserByRoleDto, HttpServletRequest request) throws IOException {
        try {
            log.debug("열기 분가져오기역할지정의사용자목록, roleId: {}", listUserByRoleDto != null ? listUserByRoleDto.getRoleId() : "null");

            // 매개변수검증
            if (listUserByRoleDto == null || StringUtils.isBlank(listUserByRoleDto.getRoleId())) {
                log.warn("분가져오기역할지정의사용자목록실패: 역할ID비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "역할 ID는 비워 둘 수 없습니다");
            }

            String roleId = listUserByRoleDto.getRoleId();
            String keyWord = listUserByRoleDto.getKeyWord();

            // 분매개변수
            int pageNum = listUserByRoleDto.getPageNum() == null ? 1 : listUserByRoleDto.getPageNum();
            int pageSize = listUserByRoleDto.getPageSize() == null ? 10 : listUserByRoleDto.getPageSize();
            if (pageNum < 1) {
                pageNum = 1;
            }
            if (pageSize <= 0) {
                pageSize = 10;
            }

            log.debug("조회파일: roleId: {}, keyWord: {}, pageNum: {}, pageSize: {}", roleId, keyWord, pageNum, pageSize);

            // 가져오기역할, 후가져오기역할지정의모든사용자id(owner/name, API가능으로의id)
            org.casbin.casdoor.entity.Role role = roleService.getRole(roleId);
            if (role == null) {
                log.warn("조회하지 못한역할정보, roleId: {}", roleId);
                return AppResponse.error(ErrorCodeEnum.E_NO_ACCOUNT, "역할찾을 수 없습니다");
            }

            String[] users = role.users;
            if (users == null || users.length == 0) {
                log.debug("역할아래있음지정작업사용자, roleId: {}", roleId);

                PageDto<com.iflytek.rpa.auth.core.entity.User> emptyPage = new PageDto<>();
                emptyPage.setResult(Collections.emptyList());
                emptyPage.setCurrentPageNo(pageNum);
                emptyPage.setPageSize(pageSize);
                emptyPage.setTotalCount(0L);
                return AppResponse.success(emptyPage);
            }

            log.debug("역할아래지정의사용자수(기존): {}, roleId: {}", users.length, roleId);

            // 근거사용자id가져오기사용자 정보, 까지통신사용, 까지list
            List<com.iflytek.rpa.auth.core.entity.User> userList = new ArrayList<>();
            for (String userId : users) {
                if (userId == null || userId.trim().isEmpty()) {
                    continue;
                }
                try {
                    // userId 예 owner/name, 가져오기 name 모듈분로로그인이름
                    String[] parts = userId.split("/");
                    String userName = parts.length > 1 ? parts[1] : parts[0];

                    User casdoorUser = userExtendService.getUser(userName);
                    if (casdoorUser == null) {
                        log.warn("근거사용자명조회하지 못한사용자 정보, userId: {}, userName: {}", userId, userName);
                        continue;
                    }

                    com.iflytek.rpa.auth.core.entity.User commonUser = userMapper.toCommonUser(casdoorUser);
                    if (commonUser != null) {
                        userList.add(commonUser);
                    }
                } catch (Exception e) {
                    log.warn("조회또는변환사용자 정보실패, userId: {}", userId, e);
                }
            }

            log.debug("역할아래성공변환로통신사용사용자객체수: {}, roleId: {}", userList.size(), roleId);

            // 닫기 문자필터링: 근거로그인이름또는이름조회
            if (StringUtils.isNotBlank(keyWord)) {
                String kw = keyWord.trim();
                userList = userList.stream()
                        .filter(u -> u != null
                                && ((u.getLoginName() != null
                                                && u.getLoginName().contains(kw))
                                        || (u.getName() != null && u.getName().contains(kw))))
                        .collect(Collectors.toList());

                log.debug("닫기 문자필터링후사용자수: {}, keyword: {}", userList.size(), kw);
            }

            // 분
            int total = userList.size();
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);

            List<com.iflytek.rpa.auth.core.entity.User> pageResult;
            if (fromIndex >= total) {
                pageResult = Collections.emptyList();
            } else {
                pageResult = userList.subList(fromIndex, toIndex);
            }

            PageDto<com.iflytek.rpa.auth.core.entity.User> pageDto = new PageDto<>();
            pageDto.setResult(pageResult);
            pageDto.setCurrentPageNo(pageNum);
            pageDto.setPageSize(pageSize);
            pageDto.setTotalCount((long) total);

            log.debug(
                    "분가져오기역할지정의사용자목록성공, roleId: {}, 데이터: {}, 현재: {}, 매: {}, 현재수: {}",
                    roleId,
                    total,
                    pageNum,
                    pageSize,
                    pageResult.size());

            return AppResponse.success(pageDto);
        } catch (IOException e) {
            log.error(
                    "분가져오기역할지정의사용자목록실패(IO예외), roleId: {}",
                    listUserByRoleDto != null ? listUserByRoleDto.getRoleId() : "null",
                    e);
            return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "분가져오기역할지정의사용자목록실패: " + e.getMessage());
        } catch (Exception e) {
            log.error(
                    "분가져오기역할지정의사용자목록예외, roleId: {}",
                    listUserByRoleDto != null ? listUserByRoleDto.getRoleId() : "null",
                    e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "분가져오기역할지정의사용자목록예외: " + e.getMessage());
        }
    }

    /**
     * 가져오기현재로그인사용자
     * @param request HTTP요청 
     * @return 현재로그인사용자 정보
     */
    @Override
    public AppResponse<com.iflytek.rpa.auth.core.entity.User> getCurrentLoginUser(HttpServletRequest request) {
        try {
            // 에서session가져오기현재사용자
            User casdoorUser = SessionUserUtils.getUserFromSession(request);

            if (casdoorUser != null) {
                // 사용mapper변환로통신사용User
                com.iflytek.rpa.auth.core.entity.User commonUser = userMapper.toCommonUser(casdoorUser);
                return AppResponse.success(commonUser);
            } else {
                com.iflytek.rpa.auth.core.entity.User localUser = new com.iflytek.rpa.auth.core.entity.User();
                localUser.setId("admin");
                localUser.setName("admin");
                localUser.setLoginName("admin");
                localUser.setUserType(1);
                localUser.setStatus(1);
                return AppResponse.success(localUser);
            }
        } catch (Exception e) {
            log.error("가져오기현재로그인사용자실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기현재로그인사용자실패: " + e.getMessage());
        }
    }

    /**
     * 가져오기현재로그인사용자ID
     * @param request HTTP요청 
     * @return 현재로그인사용자ID
     */
    @Override
    public AppResponse<String> getCurrentUserId(HttpServletRequest request) {
        try {
            // 에서session가져오기현재사용자
            User casdoorUser = SessionUserUtils.getUserFromSession(request);

            if (casdoorUser != null) {
                return AppResponse.success(casdoorUser.id);
            } else {
                return AppResponse.success("admin");
            }
        } catch (Exception e) {
            log.error("가져오기현재사용자ID실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기현재사용자ID실패: " + e.getMessage());
        }
    }

    /**
     * 가져오기현재로그인사용자명
     * @param request HTTP요청 
     * @return 현재로그인사용자명
     */
    @Override
    public AppResponse<String> getCurrentLoginUsername(HttpServletRequest request) {
        try {
            // 에서session가져오기현재사용자
            User casdoorUser = SessionUserUtils.getUserFromSession(request);

            if (casdoorUser != null) {
                return AppResponse.success(casdoorUser.name);
            } else {
                return AppResponse.success("admin");
            }
        } catch (Exception e) {
            log.error("가져오기현재로그인사용자명실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기현재로그인사용자명실패: " + e.getMessage());
        }
    }

    /**
     * 근거사용자ID조회로그인이름
     * @param id 사용자ID
     * @param request HTTP요청 
     * @return 로그인이름
     */
    @Override
    public AppResponse<String> getLoginNameById(String id, HttpServletRequest request) {
        try {
            if (Objects.isNull(userExtendService) || Objects.isNull(id)) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "매개변수오류");
            }

            User casdoorUser = userExtendService.getUserById(id);
            if (Objects.isNull(casdoorUser)) {
                return AppResponse.error(ErrorCodeEnum.E_NO_ACCOUNT, "사용자를 찾을 수 없습니다");
            }

            return AppResponse.success(casdoorUser.name);
        } catch (Exception e) {
            log.error("근거사용자ID가져오기로그인이름실패: {}", id, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "근거사용자ID가져오기로그인이름실패: " + e.getMessage());
        }
    }

    /**
     * 근거사용자ID조회이름
     * @param id 사용자ID
     * @param request HTTP요청 
     * @return 사용자이름
     */
    @Override
    public AppResponse<String> getRealNameById(String id, HttpServletRequest request) {
        try {
            if (Objects.isNull(userExtendService) || Objects.isNull(id)) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "매개변수오류");
            }

            User casdoorUser = userExtendService.getUserById(id);
            if (Objects.isNull(casdoorUser)) {
                return AppResponse.error(ErrorCodeEnum.E_NO_ACCOUNT, "사용자를 찾을 수 없습니다");
            }

            return AppResponse.success(casdoorUser.displayName);
        } catch (Exception e) {
            log.error("근거사용자ID가져오기 이름실패: {}", id, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "근거사용자ID가져오기 이름실패: " + e.getMessage());
        }
    }

    /**
     * 근거사용자ID조회사용자 정보
     * @param id 사용자ID
     * @param request HTTP요청 
     * @return 사용자 정보
     */
    @Override
    public AppResponse<com.iflytek.rpa.auth.core.entity.User> getUserInfoById(String id, HttpServletRequest request) {
        try {
            if (Objects.isNull(userExtendService) || Objects.isNull(id)) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "매개변수오류");
            }

            User casdoorUser = userExtendService.getUserById(id);
            if (Objects.isNull(casdoorUser)) {
                return AppResponse.error(ErrorCodeEnum.E_NO_ACCOUNT, "사용자를 찾을 수 없습니다");
            }

            // 사용mapper변환로통신사용User
            com.iflytek.rpa.auth.core.entity.User commonUser = userMapper.toCommonUser(casdoorUser);
            return AppResponse.success(commonUser);
        } catch (Exception e) {
            log.error("근거사용자ID가져오기사용자 정보실패: {}", id, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "근거사용자ID가져오기사용자 정보실패: " + e.getMessage());
        }
    }

    /**
     * 근거휴대폰 번호조회사용자이름
     * @param phone 휴대폰 번호
     * @param request HTTP요청 
     * @return 사용자이름
     */
    @Override
    public AppResponse<String> getRealNameByPhone(String phone, HttpServletRequest request) {
        try {
            if (Objects.isNull(userExtendService) || Objects.isNull(phone)) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "매개변수오류");
            }

            User casdoorUser = userExtendService.getUserByPhone(phone);
            if (Objects.isNull(casdoorUser)) {
                return AppResponse.error(ErrorCodeEnum.E_NO_ACCOUNT, "사용자를 찾을 수 없습니다");
            }

            return AppResponse.success(casdoorUser.displayName);
        } catch (Exception e) {
            log.error("근거가져오기사용자이름실패: {}", phone, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "근거가져오기사용자이름실패: " + e.getMessage());
        }
    }

    /**
     * 근거휴대폰 번호조회로그인이름
     * @param phone 휴대폰 번호
     * @param request HTTP요청 
     * @return 로그인이름
     */
    @Override
    public AppResponse<String> getLoginNameByPhone(String phone, HttpServletRequest request) {
        try {
            if (Objects.isNull(userExtendService) || Objects.isNull(phone)) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "매개변수오류");
            }

            User casdoorUser = userExtendService.getUserByPhone(phone);
            if (Objects.isNull(casdoorUser)) {
                return AppResponse.error(ErrorCodeEnum.E_NO_ACCOUNT, "사용자를 찾을 수 없습니다");
            }

            return AppResponse.success(casdoorUser.name);
        } catch (Exception e) {
            log.error("근거가져오기로그인이름실패: {}", phone, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "근거가져오기로그인이름실패: " + e.getMessage());
        }
    }

    /**
     * 여부사용자(ext_info = 1 테이블사용자)
     * @param phone 휴대폰 번호
     * @return 여부사용자
     */
    @Override
    public AppResponse<Boolean> isHistoryUser(String phone) {
        try {
            log.debug("여부사용자, phone: {}(Casdoor지원하지 않음공가능, 반환false)", phone);
            return AppResponse.success(false);
        } catch (Exception e) {
            log.error("여부사용자예외, phone: {}", phone, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "여부사용자실패: " + e.getMessage());
        }
    }

    /**
     * 근거휴대폰 번호조회사용자 정보
     * @param phone 휴대폰 번호
     * @param request HTTP요청 
     * @return 사용자 정보
     */
    @Override
    public AppResponse<com.iflytek.rpa.auth.core.entity.User> getUserInfoByPhone(
            String phone, HttpServletRequest request) {
        try {
            if (Objects.isNull(userExtendService) || Objects.isNull(phone)) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "매개변수오류");
            }

            User casdoorUser = userExtendService.getUserByPhone(phone);
            if (Objects.isNull(casdoorUser)) {
                return AppResponse.error(ErrorCodeEnum.E_NO_ACCOUNT, "사용자를 찾을 수 없습니다");
            }

            // 사용mapper변환로통신사용User
            com.iflytek.rpa.auth.core.entity.User commonUser = userMapper.toCommonUser(casdoorUser);
            return AppResponse.success(commonUser);
        } catch (Exception e) {
            log.error("근거가져오기사용자 정보실패: {}", phone, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "근거가져오기사용자 정보실패: " + e.getMessage());
        }
    }

    /**
     * 근거사용자ID목록조회사용자 정보목록(다중지원100개id)
     * @param userIdList 사용자ID목록
     * @param request HTTP요청 
     * @return 사용자 정보목록
     */
    @Override
    public AppResponse<List<com.iflytek.rpa.auth.core.entity.User>> queryUserListByIds(
            List<String> userIdList, HttpServletRequest request) {
        try {
            if (Objects.isNull(userService) || userIdList == null || userIdList.isEmpty()) {
                return AppResponse.success(Collections.emptyList());
            }

            // 제한제어다중100개ID, 재후조직성공Set
            Set<String> limitedUserIds =
                    userIdList.stream().distinct().limit(100).collect(Collectors.toSet());

            List<User> allCasdoorUsers = userService.getUsers();
            List<User> filteredCasdoorUsers = allCasdoorUsers.stream()
                    .filter(user -> limitedUserIds.contains(user.id))
                    .collect(Collectors.toList());

            // 사용mapper변환로통신사용User목록
            List<com.iflytek.rpa.auth.core.entity.User> commonUsers =
                    filteredCasdoorUsers.stream().map(userMapper::toCommonUser).collect(Collectors.toList());

            return AppResponse.success(commonUsers);
        } catch (IOException e) {
            log.error("근거사용자ID목록조회사용자 정보실패", e);
            return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "근거사용자ID목록조회사용자 정보실패: " + e.getMessage());
        }
    }

    /**
     * 근거이름조회사람원
     * @param keyword 닫기 문자
     * @param deptId 모듈ID(가능선택)
     * @param request HTTP요청 
     * @return 사용자 정보목록
     */
    @Override
    public AppResponse<List<com.iflytek.rpa.auth.core.entity.User>> searchUserByName(
            String keyword, String deptId, HttpServletRequest request) {
        try {
            log.debug("열기 근거이름조회사람원, keyword: {}", keyword);

            // 매개변수검증
            if (keyword == null || keyword.trim().isEmpty()) {
                log.warn("근거이름조회사람원실패: 닫기 문자비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "검색어는 비워 둘 수 없습니다");
            }

            // 가져오기현재테넌트ID(owner), 가능선택
            String owner = getCurrentTenantOwner(request);
            // 조회사용자목록(제한지정에서현재테넌트아래, 결과가owner비어 있습니다이면조회전체)
            List<User> casdoorUsers = casdoorUserDao.searchUserByName(keyword, owner, databaseName);
            if (casdoorUsers == null) {
                log.debug("조회결과비어 있습니다, keyword: {}", keyword);
                return AppResponse.success(Collections.emptyList());
            }

            log.debug("조회까지 {} 개사용자, keyword: {}", casdoorUsers.size(), keyword);

            // 변환로통신사용사용자객체목록, 필터링변환실패의객체
            List<com.iflytek.rpa.auth.core.entity.User> userList = casdoorUsers.stream()
                    .filter(user -> user != null)
                    .map(user -> {
                        try {
                            return userMapper.toCommonUser(user);
                        } catch (Exception e) {
                            log.warn("사용자 정보변환실패, userId: {}, keyword: {}", user != null ? user.id : "null", keyword, e);
                            return null;
                        }
                    })
                    .filter(user -> user != null)
                    .collect(Collectors.toList());

            log.debug("성공변환 {} 개사용자, keyword: {}", userList.size(), keyword);
            return AppResponse.success(userList);
        } catch (Exception e) {
            log.error("근거이름조회사람원예외, keyword: {}", keyword, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "근거이름조회사람원실패: " + e.getMessage());
        }
    }

    /**
     * 근거휴대폰 번호조회사람원
     * @param keyword 닫기 문자
     * @param deptId 모듈ID(가능선택)
     * @param request HTTP요청 
     * @return 사용자 정보목록
     */
    @Override
    public AppResponse<List<com.iflytek.rpa.auth.core.entity.User>> searchUserByPhone(
            String keyword, String deptId, HttpServletRequest request) {
        try {
            log.debug("열기 근거휴대폰 번호조회사람원, keyword: {}", keyword);

            // 매개변수검증
            if (keyword == null || keyword.trim().isEmpty()) {
                log.warn("근거휴대폰 번호조회사람원실패: 닫기 문자비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "검색어는 비워 둘 수 없습니다");
            }

            // 가져오기현재테넌트ID(owner)
            String owner = getCurrentTenantOwner(request);

            // 조회사용자목록(제한지정에서현재테넌트아래, 결과가owner비어 있습니다이면조회전체)
            List<User> casdoorUsers = casdoorUserDao.searchUserByPhone(keyword, owner, databaseName);
            if (casdoorUsers == null) {
                log.debug("조회결과비어 있습니다, keyword: {}", keyword);
                return AppResponse.success(Collections.emptyList());
            }

            log.debug("조회까지 {} 개사용자, keyword: {}", casdoorUsers.size(), keyword);

            // 변환로통신사용사용자객체목록, 필터링변환실패의객체
            List<com.iflytek.rpa.auth.core.entity.User> userList = casdoorUsers.stream()
                    .filter(user -> user != null)
                    .map(user -> {
                        try {
                            return userMapper.toCommonUser(user);
                        } catch (Exception e) {
                            log.warn("사용자 정보변환실패, userId: {}, keyword: {}", user != null ? user.id : "null", keyword, e);
                            return null;
                        }
                    })
                    .filter(user -> user != null)
                    .collect(Collectors.toList());

            log.debug("성공변환 {} 개사용자, keyword: {}", userList.size(), keyword);
            return AppResponse.success(userList);
        } catch (Exception e) {
            log.error("근거휴대폰 번호조회사람원예외, keyword: {}", keyword, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "근거휴대폰 번호조회사람원실패: " + e.getMessage());
        }
    }

    /**
     * 근거이름또는휴대폰 번호조회사람원
     * @param keyword 닫기 문자
     * @param deptId 모듈ID(가능선택)
     * @param request HTTP요청 
     * @return 사용자 정보목록
     */
    @Override
    public AppResponse<List<com.iflytek.rpa.auth.core.entity.User>> searchUserByNameOrPhone(
            String keyword, String deptId, HttpServletRequest request) {
        try {
            log.debug("열기 근거이름또는휴대폰 번호조회사람원, keyword: {}", keyword);

            // 매개변수검증
            if (keyword == null || keyword.trim().isEmpty()) {
                log.warn("근거이름또는휴대폰 번호조회사람원실패: 닫기 문자비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "검색어는 비워 둘 수 없습니다");
            }

            // 가져오기현재테넌트ID(owner), 가능선택
            String owner = getCurrentTenantOwner(request);

            // 조회사용자목록(제한지정에서현재테넌트아래, 결과가owner비어 있습니다이면조회전체)
            List<User> casdoorUsers = casdoorUserDao.searchUserByNameOrPhone(keyword, owner, databaseName);
            if (casdoorUsers == null) {
                log.debug("조회결과비어 있습니다, keyword: {}", keyword);
                return AppResponse.success(Collections.emptyList());
            }

            log.debug("조회까지 {} 개사용자, keyword: {}", casdoorUsers.size(), keyword);

            // 변환로통신사용사용자객체목록, 필터링변환실패의객체
            List<com.iflytek.rpa.auth.core.entity.User> userList = casdoorUsers.stream()
                    .filter(user -> user != null)
                    .map(user -> {
                        try {
                            return userMapper.toCommonUser(user);
                        } catch (Exception e) {
                            log.warn("사용자 정보변환실패, userId: {}, keyword: {}", user != null ? user.id : "null", keyword, e);
                            return null;
                        }
                    })
                    .filter(user -> user != null)
                    .collect(Collectors.toList());

            log.debug("성공변환 {} 개사용자, keyword: {}", userList.size(), keyword);
            return AppResponse.success(userList);
        } catch (Exception e) {
            log.error("근거이름또는휴대폰 번호조회사람원예외, keyword: {}", keyword, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "근거이름또는휴대폰 번호조회사람원실패: " + e.getMessage());
        }
    }

    /**
     * 조회현재로그인의사용자 정보
     * @param request HTTP요청 
     * @return 사용자 정보
     */
    @Override
    public AppResponse<com.iflytek.rpa.auth.core.entity.User> getUserInfo(HttpServletRequest request) {
        // 및getCurrentLoginUser
        return getCurrentLoginUser(request);
    }

    /**
     * 변수변경모듈
     * @param userChangeDeptDto 변수변경모듈정보
     * @param request HTTP요청 
     * @return 변수변경결과
     */
    @Override
    public AppResponse<String> changeDept(UserChangeDeptDto userChangeDeptDto, HttpServletRequest request) {
        try {
            log.debug("변수변경모듈(Casdoor지원하지 않음공가능, 반환안내정보)");
            return AppResponse.success("Casdoor지원하지 않음변수변경모듈공가능");
        } catch (Exception e) {
            log.error("변수변경모듈예외", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "변수변경모듈실패: " + e.getMessage());
        }
    }

    /**
     * 삭제요소
     * @param userDeleteDto 삭제원정보
     * @param request HTTP요청 
     * @return 삭제결과
     */
    @Override
    public AppResponse<String> deleteUser(UserDeleteDto userDeleteDto, HttpServletRequest request) throws IOException {
        try {
            log.debug("열기 삭제요소");

            // 매개변수검증
            if (userDeleteDto == null || CollectionUtil.isEmpty(userDeleteDto.getUserIdList())) {
                log.warn("삭제요소실패: 매개변수가 비어 있습니다또는사용자ID목록비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "사용자ID목록은 비워 둘 수 없습니다");
            }

            List<String> userIdList = userDeleteDto.getUserIdList();
            log.debug("준비삭제 {} 개사용자", userIdList.size());

            int successCount = 0;
            int failCount = 0;
            List<String> failUserIds = new ArrayList<>();

            // 삭제매개사용자
            for (String userId : userIdList) {
                try {
                    if (userId == null || userId.trim().isEmpty()) {
                        log.warn("건너뛰기빈사용자ID");
                        failCount++;
                        continue;
                    }

                    log.debug("열기 삭제사용자, userId: {}", userId);

                    // 근거id조회출력사용자name및owner
                    User userById = null;
                    try {
                        userById = userExtendService.getUserById(userId);
                    } catch (Exception e) {
                        log.warn("조회사용자실패, userId: {}", userId, e);
                        failCount++;
                        failUserIds.add(userId);
                        continue;
                    }

                    if (userById == null) {
                        log.warn("사용자를 찾을 수 없습니다, userId: {}", userId);
                        failCount++;
                        failUserIds.add(userId);
                        continue;
                    }

                    // 호출삭제연결
                    log.debug("호출Casdoor API삭제사용자, userId: {}, name: {}", userId, userById.name);
                    CasdoorResponse<String, Object> deleteUserResponse = userExtendService.deleteUser(userById);

                    // 검증결과
                    if (deleteUserResponse == null) {
                        log.error("삭제사용자실패: Casdoor API반환비어 있습니다, userId: {}", userId);
                        failCount++;
                        failUserIds.add(userId);
                        continue;
                    }

                    if (deleteUserResponse.getStatus() != null && !"ok".equals(deleteUserResponse.getStatus())) {
                        log.error(
                                "삭제사용자실패: Casdoor API반환오류, userId: {}, status: {}, msg: {}",
                                userId,
                                deleteUserResponse.getStatus(),
                                deleteUserResponse.getMsg());
                        failCount++;
                        failUserIds.add(userId);
                        continue;
                    }

                    log.debug("삭제사용자성공, userId: {}, name: {}", userId, userById.name);
                    successCount++;
                } catch (Exception e) {
                    log.error("삭제사용자예외, userId: {}", userId, e);
                    failCount++;
                    failUserIds.add(userId);
                }
            }

            // 반환결과
            if (failCount == 0) {
                log.debug("삭제원완료, 공유삭제 {} 개사용자", successCount);
                return AppResponse.success("성공삭제 " + successCount + " 개사용자");
            } else if (successCount == 0) {
                log.warn("삭제요소실패, 모든사용자삭제실패, 공유 {} 개", failCount);
                return AppResponse.error(
                        ErrorCodeEnum.E_SERVICE,
                        "삭제실패, 공유 " + failCount + " 개사용자삭제실패, 실패사용자ID: " + String.join(", ", failUserIds));
            } else {
                log.warn("삭제요소모듈분성공, 성공: {}, 실패: {}", successCount, failCount);
                return AppResponse.error(
                        ErrorCodeEnum.E_SERVICE,
                        "모듈분삭제성공, 성공: " + successCount + " 개, 실패: " + failCount + " 개, 실패사용자ID: "
                                + String.join(", ", failUserIds));
            }
        } catch (Exception e) {
            log.error("삭제요소예외", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "삭제요소예외: " + e.getMessage());
        }
    }

    /**
     * 사용/사용 안 함요소
     * @param userEnableDto 사용/사용 안 함정보
     * @param request HTTP요청 
     * @return 결과
     */
    @Override
    public AppResponse<String> enableUser(UserEnableDto userEnableDto, HttpServletRequest request) {
        try {
            log.debug("열기 사용/사용 안 함요소");

            // 매개변수검증
            Integer status = userEnableDto.getStatus();
            if (CollectionUtil.isEmpty(userEnableDto.getUserList()) || status == null) {
                log.warn("사용/사용 안 함요소실패: 매개변수가 비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "사용자목록또는상태비워 둘 수 없습니다");
            }

            if (!status.equals(0) && !status.equals(1)) {
                log.warn("사용/사용 안 함요소실패: 상태값없음, status: {}", status);
                return AppResponse.error(ErrorCodeEnum.E_PARAM, "상태값없음, 가능로0(중지사용)또는1(사용)");
            }

            String statusText = status == 1 ? "사용" : "중지사용";
            log.debug("준비{} {} 개사용자", statusText, userEnableDto.getUserList().size());

            int successCount = 0;
            int failCount = 0;
            List<String> failUserIds = new ArrayList<>();

            // 관리매개사용자
            for (UpdateUserDto updateUserDto : userEnableDto.getUserList()) {
                try {
                    if (updateUserDto == null
                            || updateUserDto.getId() == null
                            || updateUserDto.getId().trim().isEmpty()) {
                        log.warn("건너뛰기지원하지 않는 사용자 정보");
                        failCount++;
                        continue;
                    }

                    String userId = updateUserDto.getId();
                    log.debug("열기 {}사용자, userId: {}", statusText, userId);

                    // 조회까지사용자
                    User existingUser = null;
                    try {
                        existingUser = userExtendService.getUserById(userId);
                    } catch (Exception e) {
                        log.warn("조회사용자실패, userId: {}", userId, e);
                        failCount++;
                        failUserIds.add(userId);
                        continue;
                    }

                    if (existingUser == null) {
                        log.warn("사용자를 찾을 수 없습니다, userId: {}", userId);
                        failCount++;
                        failUserIds.add(userId);
                        continue;
                    }

                    // 업데이트사용자, status의값수정사용자의사용상태필드(isForbidden)
                    // status: 0중지사용 -> isForbidden=true, 1사용 -> isForbidden=false
                    boolean isForbidden = (status == 0);

                    // 결과가상태완료, 건너뛰기
                    if (existingUser.isForbidden == isForbidden) {
                        log.debug("사용자상태완료예목록 상태, 건너뛰기, userId: {}, isForbidden: {}", userId, isForbidden);
                        successCount++;
                        continue;
                    }

                    // 생성업데이트객체, 업데이트상태필드
                    User userToUpdate = new User();
                    userToUpdate.id = existingUser.id;
                    userToUpdate.name = existingUser.name;
                    userToUpdate.owner = existingUser.owner;
                    userToUpdate.displayName = existingUser.displayName;
                    userToUpdate.phone = existingUser.phone;
                    userToUpdate.email = existingUser.email;
                    userToUpdate.isForbidden = isForbidden;
                    userToUpdate.isAdmin = existingUser.isAdmin;
                    userToUpdate.isGlobalAdmin = existingUser.isGlobalAdmin;
                    userToUpdate.type = existingUser.type != null ? existingUser.type : "normal-user";
                    userToUpdate.password = existingUser.password != null ? existingUser.password : "";
                    userToUpdate.passwordSalt = existingUser.passwordSalt != null ? existingUser.passwordSalt : "";
                    userToUpdate.isDeleted = existingUser.isDeleted;
                    userToUpdate.emailVerified = existingUser.emailVerified;
                    userToUpdate.createdTime = existingUser.createdTime;
                    userToUpdate.updatedTime = formatDateTime(new Date());
                    userToUpdate.properties = existingUser.properties;
                    userToUpdate.roles = existingUser.roles;
                    userToUpdate.permissions = existingUser.permissions;
                    userToUpdate.address = existingUser.address;
                    userToUpdate.location = existingUser.location;
                    userToUpdate.bio = existingUser.bio;
                    userToUpdate.idCard = existingUser.idCard;
                    userToUpdate.birthday = existingUser.birthday;

                    // 호출업데이트연결
                    log.debug("호출Casdoor API{}사용자, userId: {}, isForbidden: {}", statusText, userId, isForbidden);
                    CasdoorResponse<String, Object> updateUserResponse = userExtendService.updateUser(userToUpdate);

                    // 검증결과
                    if (updateUserResponse == null) {
                        log.error("{}사용자실패: Casdoor API반환비어 있습니다, userId: {}", statusText, userId);
                        failCount++;
                        failUserIds.add(userId);
                        continue;
                    }

                    if (updateUserResponse.getStatus() != null && !"ok".equals(updateUserResponse.getStatus())) {
                        log.error(
                                "{}사용자실패: Casdoor API반환오류, userId: {}, status: {}, msg: {}",
                                statusText,
                                userId,
                                updateUserResponse.getStatus(),
                                updateUserResponse.getMsg());
                        failCount++;
                        failUserIds.add(userId);
                        continue;
                    }

                    log.debug("{}사용자성공, userId: {}", statusText, userId);
                    successCount++;
                } catch (Exception e) {
                    log.error(
                            "{}사용자예외, userId: {}", statusText, updateUserDto != null ? updateUserDto.getId() : "null", e);
                    failCount++;
                    failUserIds.add(updateUserDto != null ? updateUserDto.getId() : "unknown");
                }
            }

            // 반환결과
            if (failCount == 0) {
                log.debug("{}원완료, 공유{} {} 개사용자", statusText, statusText, successCount);
                return AppResponse.success("성공" + statusText + " " + successCount + " 개사용자");
            } else if (successCount == 0) {
                log.warn("{}요소실패, 모든사용자{}실패, 공유 {} 개", statusText, statusText, failCount);
                return AppResponse.error(
                        ErrorCodeEnum.E_SERVICE,
                        statusText + "실패, 공유 " + failCount + " 개사용자" + statusText + "실패, 실패사용자ID: "
                                + String.join(", ", failUserIds));
            } else {
                log.warn("{}요소모듈분성공, 성공: {}, 실패: {}", statusText, successCount, failCount);
                return AppResponse.error(
                        ErrorCodeEnum.E_SERVICE,
                        "모듈분" + statusText + "성공, 성공: " + successCount + " 개, 실패: " + failCount + " 개, 실패사용자ID: "
                                + String.join(", ", failUserIds));
            }
        } catch (Exception e) {
            log.error("사용/사용 안 함요소예외", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "사용/사용 안 함요소예외: " + e.getMessage());
        }
    }

    /**
     * 조회현재기기의전체사용자(모듈추가, 모듈사람드롭다운)
     * @param orgId 기기ID(Casdoor의Group name)
     * @param request HTTP요청 
     * @return 사용자목록
     */
    @Override
    public AppResponse<List<com.iflytek.rpa.auth.core.entity.User>> queryUserDetailListByOrgId(
            String orgId, HttpServletRequest request) throws IOException {
        try {
            log.debug("열기 조회현재기기의전체사용자, orgId: {}", orgId);

            // 매개변수검증
            if (orgId == null || orgId.trim().isEmpty()) {
                log.warn("조회현재기기전체사용자실패: 기기ID비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "기기ID비워 둘 수 없습니다");
            }

            // 호출Casdoor서비스조회지정조직아래의사용자id(owner/name)목록
            Group group = null;
            try {
                group = casdoorGroupExtendService.getGroup(orgId);
            } catch (Exception e) {
                log.error("근거기기ID조회Casdoor그룹정보실패, orgId: {}", orgId, e);
                return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "조회기기정보실패: " + e.getMessage());
            }

            if (group == null) {
                log.warn("근거기기ID조회하지 못한Casdoor그룹정보, orgId: {}", orgId);
                return AppResponse.error(ErrorCodeEnum.E_NO_ACCOUNT, "기기찾을 수 없습니다");
            }

            List<String> userIds = group.users;
            if (userIds == null || userIds.isEmpty()) {
                log.debug("현재기기아래조회하지 못한사용자, orgId: {}", orgId);
                return AppResponse.success(Collections.emptyList());
            }

            log.debug("현재기기아래조회까지 {} 개지정사용자ID(owner/name), orgId: {}", userIds.size(), orgId);

            // 근거사용자id(owner/name)조회사용자 정보, 경과중작업단일개사용자실패아니요사용자
            List<User> users = userIds.stream()
                    .filter(Objects::nonNull)
                    .map(userId -> {
                        try {
                            String[] parts = userId.split("/");
                            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                                log.warn("사용자 ID 형식이 올바르지 않습니다, 로owner/name, 로: {}", userId);
                                return null;
                            }
                            String name = parts[1].trim();
                            return userExtendService.getUser(name);
                        } catch (IOException ioEx) {
                            log.warn("근거사용자ID조회Casdoor사용자실패(IO예외), userId: {}", userId, ioEx);
                            return null;
                        } catch (Exception ex) {
                            log.warn("근거사용자ID조회Casdoor사용자실패, userId: {}", userId, ex);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (users.isEmpty()) {
                log.debug("현재기기아래모든지정사용자ID미완료가능성공조회까지사용자 정보, orgId: {}", orgId);
                return AppResponse.success(Collections.emptyList());
            }

            // 변환로통신사용User목록
            List<com.iflytek.rpa.auth.core.entity.User> commonUsers = users.stream()
                    .filter(Objects::nonNull)
                    .map(u -> {
                        try {
                            return userMapper.toCommonUser(u);
                        } catch (Exception e) {
                            log.warn("사용자 정보변환실패, userId: {}", u != null ? u.id : "null", e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.debug("현재기기아래성공변환 {} 개통신사용사용자객체, orgId: {}", commonUsers.size(), orgId);
            return AppResponse.success(commonUsers);
        } catch (Exception e) {
            log.error("조회현재기기전체사용자예외, orgId: {}", orgId, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회현재기기전체사용자예외: " + e.getMessage());
        }
    }

    /**
     * 중-봇-모든아래선택-조회연결
     * 근거입력의닫기 문자(이름또는휴대폰 번호)조회사용자
     * @param keyword 닫기 문자(이름또는휴대폰 번호)
     * @param deptId 모듈ID(내용연결사용)
     * @return 사용자검색결과목록
     */
    @Override
    public AppResponse<List<UserSearchDto>> getUserByNameOrPhone(
            String keyword, String deptId, HttpServletRequest request) {
        try {
            log.debug("열기 근거이름또는휴대폰 번호조회사용자, keyword: {}", keyword);

            // 매개변수검증
            if (keyword == null || keyword.trim().isEmpty()) {
                log.warn("근거이름또는휴대폰 번호조회사용자실패: 닫기 문자비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "검색어는 비워 둘 수 없습니다");
            }

            // 가져오기현재테넌트ID(owner), 제한지정조회
            String owner = getCurrentTenantOwner(request);

            // 호출DAO이름또는휴대폰 번호조회(에서현재테넌트내부)
            List<User> casdoorUsers = casdoorUserDao.searchUserByNameOrPhone(keyword.trim(), owner, databaseName);
            if (casdoorUsers == null || casdoorUsers.isEmpty()) {
                log.debug("근거이름또는휴대폰 번호조회사용자결과비어 있습니다, keyword: {}", keyword);
                return AppResponse.success(Collections.emptyList());
            }

            log.debug("근거이름또는휴대폰 번호조회까지 {} 개사용자, keyword: {}", casdoorUsers.size(), keyword);

            // 변환로의 UserSearchDto 목록
            List<UserSearchDto> result = casdoorUsers.stream()
                    .filter(Objects::nonNull)
                    .map(u -> {
                        UserSearchDto dto = new UserSearchDto();
                        dto.setId(u.id);
                        dto.setName(u.name);
                        dto.setPhone(u.phone);
                        return dto;
                    })
                    .collect(Collectors.toList());

            log.debug("근거이름또는휴대폰 번호조회사용자성공, 반환 {} 결과, keyword: {}", result.size(), keyword);
            return AppResponse.success(result);
        } catch (Exception e) {
            log.error("근거이름또는휴대폰 번호조회사용자예외, keyword: {}", keyword, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "근거이름또는휴대폰 번호조회사용자예외: " + e.getMessage());
        }
    }

    /**
     * 가져오기사용자정보
     * @param tenantId 테넌트ID
     * @param getUserDto 조회매개변수
     * @param request HTTP요청 
     * @return 사용자정보
     */
    @Override
    public AppResponse<UserExtendDto> queryUserExtendInfo(
            String tenantId, GetUserDto getUserDto, HttpServletRequest request) throws IOException {
        UserExtendDto userExtendDto = new UserExtendDto();
        User userById = null;
        User userByName = null;

        // id조회
        if (Objects.nonNull(getUserDto.getUserId())) {
            userById = userExtendService.getUserById(getUserDto.getUserId());
        }

        // name조회
        if (Objects.nonNull(getUserDto.getLoginName())) {
            userByName = userExtendService.getUser(getUserDto.getLoginName());
        }

        // 조회아니요까지결과반환빈
        if (userById == null && userByName == null) {
            return AppResponse.success(userExtendDto);
        }
        // 결과가id및name조회출력의결과아니요예의, 반환빈
        if (userById != null && userByName != null && !StringUtils.equals(userById.id, userByName.id)) {
            return AppResponse.success(userExtendDto);
        }

        // 변환로통신사용user
        com.iflytek.rpa.auth.core.entity.User commonUser =
                userMapper.toCommonUser(userById == null ? userByName : userById);

        userExtendDto.setUser(commonUser);

        return AppResponse.success(userExtendDto);
    }

    /**
     * 가져오기현재사용자권한목록
     * @param request HTTP요청 
     * @return 사용자권한목록
     */
    @Override
    public AppResponse<List<Permission>> getCurrentUserPermissionList(HttpServletRequest request) throws IOException {
        try {
            // 에서session가져오기현재사용자
            User casdoorUser = SessionUserUtils.getUserFromSession(request);

            if (casdoorUser != null && casdoorUser.permissions != null) {
                // 사용mapper를Casdoor Permission변환로통신사용Permission
                List<Permission> commonPermissions = casdoorUser.permissions.stream()
                        .map(permissionMapper::toCommonPermission)
                        .collect(Collectors.toList());

                return AppResponse.success(commonPermissions);
            } else if (casdoorUser != null) {
                // 결과가사용자권한 없음목록, 반환빈목록
                return AppResponse.success(Collections.emptyList());
            } else {
                return AppResponse.error(ErrorCodeEnum.E_NOT_LOGIN, "사용자로그인되지 않았습니다");
            }
        } catch (Exception e) {
            log.error("가져오기현재사용자권한목록실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기현재사용자권한목록실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<UserEntitlementDto> getCurrentUserEntitlement(HttpServletRequest request) {
        UserEntitlementDto userEntitlementDto = new UserEntitlementDto();
        userEntitlementDto.setModuleConsole(true);
        userEntitlementDto.setModuleDesigner(true);
        userEntitlementDto.setModuleExecutor(true);
        userEntitlementDto.setModuleMarket(true);
        return AppResponse.success(userEntitlementDto);
    }

    @Override
    public AppResponse<String> getNameById(String id, HttpServletRequest request) {
        return AppResponse.success("");
    }

    @Override
    public AppResponse<PageDto<RobotExecute>> getDeployedUserList(
            GetDeployedUserListDto dto, HttpServletRequest request) {
        return AppResponse.success(new PageDto<>());
    }

    @Override
    public AppResponse<List<MarketDto>> getUserUnDeployed(GetUserUnDeployedDto dto, HttpServletRequest request) {
        return AppResponse.success(new ArrayList<>());
    }

    @Override
    public AppResponse<PageDto<MarketDto>> getMarketUserList(GetMarketUserListDto dto, HttpServletRequest request) {
        try {
            if (dto.getPageNo() == null || dto.getPageNo() < 1) {
                dto.setPageNo(1);
            }
            if (dto.getPageSize() == null || dto.getPageSize() < 1) {
                dto.setPageSize(10);
            }
            Page<MarketDto> page = new Page<>(dto.getPageNo(), dto.getPageSize(), true);
            
            // 1: 에서rpa데이터베이스조회마켓사용자목록(app_market_user테이블)
            IPage<MarketDto> marketUserPage = marketUserDao.getMarketUserListFromRpa(page, dto);
            List<MarketDto> marketUsers = marketUserPage.getRecords();
            
            if (CollectionUtils.isEmpty(marketUsers)) {
                PageDto<MarketDto> pageDto = new PageDto<>();
                pageDto.setResult(Collections.emptyList());
                pageDto.setTotalCount(0);
                pageDto.setCurrentPageNo((int) marketUserPage.getCurrent());
                pageDto.setPageSize((int) marketUserPage.getSize());
                return AppResponse.success(pageDto);
            }
            
            // 2: 모든creatorId, 량에서casdoor데이터베이스조회사용자정보
            List<String> creatorIds = marketUsers.stream()
                    .map(MarketDto::getCreatorId)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());
            
            // 에서casdoor데이터베이스량조회사용자 정보
            Map<String, User> userMap = new HashMap<>();
            if (!CollectionUtils.isEmpty(creatorIds)) {
                for (String creatorId : creatorIds) {
                    try {
                        User user = userExtendService.getUserById(creatorId);
                        if (user != null && !user.isDeleted) {
                            userMap.put(creatorId, user);
                        }
                    } catch (Exception e) {
                        log.warn("조회사용자 정보실패, creatorId: {}", creatorId, e);
                    }
                }
            }
            
            // 3: 병합데이터, 사용자정보, 사용필터링파일
            List<MarketDto> resultList = new ArrayList<>();
            for (MarketDto marketUser : marketUsers) {
                User user = userMap.get(marketUser.getCreatorId());
                if (user != null) {
                    // 사용사용자명및이름필터링(기존SQL중의파일)
                    boolean match = true;
                    if (StringUtils.isNotBlank(dto.getUserName())) {
                        match = match && (StringUtils.containsIgnoreCase(user.name, dto.getUserName())
                                || StringUtils.containsIgnoreCase(user.displayName, dto.getUserName()));
                    }
                    if (StringUtils.isNotBlank(dto.getRealName())) {
                        match = match && StringUtils.containsIgnoreCase(user.displayName, dto.getRealName());
                    }
                    
                    if (match) {
                        marketUser.setUserName(user.name);
                        marketUser.setRealName(user.displayName);
                        marketUser.setEmail(user.email);
                        marketUser.setPhone(user.phone);
                        resultList.add(marketUser);
                    }
                }
            }
            
            // 4: 사용정렬(기존SQL중의ORDER BY)
            if (StringUtils.isNotBlank(dto.getSortBy())) {
                String sortBy = dto.getSortBy();
                boolean isDesc = "descend".equals(dto.getSortType());
                resultList.sort((a, b) -> {
                    int compare = 0;
                    switch (sortBy) {
                        case "userName":
                            compare = StringUtils.compareIgnoreCase(a.getUserName(), b.getUserName());
                            break;
                        case "realName":
                            compare = StringUtils.compareIgnoreCase(a.getRealName(), b.getRealName());
                            break;
                        case "createTime":
                            compare = a.getCreateTime() != null && b.getCreateTime() != null
                                    ? a.getCreateTime().compareTo(b.getCreateTime())
                                    : 0;
                            break;
                        default:
                            compare = a.getCreateTime() != null && b.getCreateTime() != null
                                    ? a.getCreateTime().compareTo(b.getCreateTime())
                                    : 0;
                    }
                    return isDesc ? -compare : compare;
                });
            } else {
                // 생성 시간순서(기존SQL중의order by createTime desc)
                resultList.sort((a, b) -> {
                    if (a.getCreateTime() != null && b.getCreateTime() != null) {
                        return b.getCreateTime().compareTo(a.getCreateTime());
                    }
                    return 0;
                });
            }
            
            // 5: 분관리
            int total = resultList.size();
            int fromIndex = ((int) marketUserPage.getCurrent() - 1) * (int) marketUserPage.getSize();
            int toIndex = Math.min(fromIndex + (int) marketUserPage.getSize(), total);
            List<MarketDto> pagedResult = fromIndex < total ? resultList.subList(fromIndex, toIndex) : Collections.emptyList();
            
            PageDto<MarketDto> pageDto = new PageDto<>();
            pageDto.setResult(pagedResult);
            pageDto.setTotalCount(total);
            pageDto.setCurrentPageNo((int) marketUserPage.getCurrent());
            pageDto.setPageSize((int) marketUserPage.getSize());
            
            return AppResponse.success(pageDto);
        } catch (Exception e) {
            log.error("가져오기마켓사용자목록실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기마켓사용자목록실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<PageDto<MarketDto>> getMarketUserListByPublic(
            GetMarketUserListByPublicDto dto, HttpServletRequest request) {
        return AppResponse.success(new PageDto<>());
    }

    @Override
    public AppResponse<List<MarketDto>> getMarketUserByPhone(GetMarketUserByPhoneDto dto, HttpServletRequest request) {
        try {
            log.debug("열기 근거휴대폰 번호또는이름조회마켓사용자, marketId: {}, keyword: {}", 
                    dto != null ? dto.getMarketId() : "null", 
                    dto != null ? dto.getKeyword() : "null");

            // 매개변수검증
            if (dto == null || StringUtils.isBlank(dto.getMarketId())) {
                log.warn("근거휴대폰 번호또는이름조회마켓사용자실패: 매개변수가 비어 있습니다또는marketId비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "마켓ID비워 둘 수 없습니다");
            }

            // 1: 에서casdoor데이터베이스조회기호합치기파일의사용자(기존SQL중의FROM ${databaseName}.`user` u)
            List<User> casdoorUsers = Collections.emptyList();
            if (StringUtils.isNotBlank(dto.getKeyword())) {
                // 근거닫기 문자조회사용자(이름또는휴대폰 번호)
                casdoorUsers = casdoorUserDao.searchUserByNameOrPhone(dto.getKeyword(), null, databaseName);
            } else {
                // 결과가있음닫기 문자, 조회모든사용자(제한제어수)
                casdoorUsers = casdoorUserDao.searchUserByNameOrPhone("", null, databaseName);
            }
            
            if (CollectionUtils.isEmpty(casdoorUsers)) {
                return AppResponse.success(Collections.emptyList());
            }
            
            // 2: 에서rpa데이터베이스조회해당마켓아래완료저장에서의사용자ID목록(기존SQL중의NOT EXISTS조회)
            List<String> existingUserIds = marketUserDao.getExistingUserIdsByMarketId(dto.getMarketId());
            Set<String> existingUserIdsSet = new HashSet<>(existingUserIds != null ? existingUserIds : Collections.emptyList());
            
            // 3: 필터링완료저장에서의사용자, 변환로MarketDto(기존SQL중의NOT EXISTS및LIMIT 20)
            List<MarketDto> result = casdoorUsers.stream()
                    .filter(user -> user != null && !user.isDeleted && !existingUserIdsSet.contains(user.id))
                    .map(user -> {
                        MarketDto marketDto = new MarketDto();
                        marketDto.setCreatorId(user.id);
                        marketDto.setPhone(user.phone);
                        marketDto.setRealName(user.displayName);
                        return marketDto;
                    })
                    .limit(20)
                    .collect(Collectors.toList());

            log.debug("근거휴대폰 번호또는이름조회마켓사용자성공, 반환 {} 결과, marketId: {}", result.size(), dto.getMarketId());
            return AppResponse.success(result);
        } catch (Exception e) {
            log.error("근거휴대폰 번호또는이름조회마켓사용자예외, marketId: {}", 
                    dto != null ? dto.getMarketId() : "null", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "근거휴대폰 번호또는이름조회마켓사용자실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<List<MarketDto>> getMarketUserByPhoneForOwner(
            GetMarketUserByPhoneForOwnerDto dto, HttpServletRequest request) {
        return AppResponse.success(new ArrayList<>());
    }

    @Override
    public AppResponse<List<TenantUser>> getMarketTenantUserList(
            GetMarketTenantUserListDto dto, HttpServletRequest request) {
        try {
            log.debug("열기 근거사용자ID목록조회테넌트사용자목록, tenantId: {}, userIdList size: {}", 
                    dto != null ? dto.getTenantId() : "null",
                    dto != null && dto.getUserIdList() != null ? dto.getUserIdList().size() : 0);

            // 매개변수검증
            if (dto == null) {
                log.warn("근거사용자ID목록조회테넌트사용자목록실패: 매개변수가 비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "조회매개변수비워 둘 수 없습니다");
            }

            if (StringUtils.isBlank(dto.getTenantId())) {
                log.warn("근거사용자ID목록조회테넌트사용자목록실패: 테넌트ID비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "테넌트 ID는 비워 둘 수 없습니다");
            }

            if (CollectionUtils.isEmpty(dto.getUserIdList())) {
                log.debug("사용자ID목록비어 있습니다, 반환빈목록");
                return AppResponse.success(Collections.emptyList());
            }

            // 호출DAO조회테넌트사용자목록
            List<TenantUser> result = casdoorUserDao.getMarketTenantUserList(dto, databaseName);
            
            log.debug("근거사용자ID목록조회테넌트사용자목록성공, 반환 {} 결과, tenantId: {}", 
                    result != null ? result.size() : 0, dto.getTenantId());
            
            return AppResponse.success(result != null ? result : Collections.emptyList());
        } catch (Exception e) {
            log.error("근거사용자ID목록조회테넌트사용자목록실패, tenantId: {}", 
                    dto != null ? dto.getTenantId() : "null", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "근거사용자ID목록조회테넌트사용자목록실패: " + e.getMessage());
        }
    }
    @Override
    public AppResponse<String> logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            log.debug("열기 로그아웃");

            // 에서요청 중가져오기casdoor의session id
            String casdoorSessionId = casdoorLoginExtendService.extractCasdoorSessionIdFromRequest(request);
            if (casdoorSessionId == null || casdoorSessionId.isEmpty()) {
                log.warn("로그아웃실패: Casdoor session ID비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "Session ID비워 둘 수 없습니다");
            }

            //            // 가져오기현재사용자의access token
            //            String accessToken = null;
            //            try {
            //                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            //                if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails)
            // {
            //                    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            //                    User currentUser = userDetails.getUser();
            //                    if (currentUser != null && currentUser.name != null && !currentUser.name.isEmpty()) {
            //                        accessToken = TokenManager.getAccessToken(currentUser.name);
            //                        log.debug("가져오기까지현재사용자의access token, username: {}", currentUser.name);
            //
            //                        // 로그아웃시지우기Redis중의token
            //                        TokenManager.clearTokens(currentUser.name);
            //                    }
            //                }
            //            } catch (Exception e) {
            //                log.warn("가져오기현재사용자access token실패", e);
            //            }
            //
            //            if (accessToken == null || accessToken.isEmpty()) {
            //                log.warn("가져오기현재사용자access token비어 있습니다, 가능token완료경과또는찾을 수 없습니다");
            //                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기사용자access token실패, 요청다시 로그인");
            //            }

            // 사용session호출logout
            log.debug("호출Casdoor로그아웃연결, sessionId: {}", casdoorSessionId);
            casdoorLoginExtendService.logout(casdoorSessionId);

            log.debug("로그아웃성공");
            return AppResponse.success("로그아웃성공!");
        } catch (IOException e) {
            log.error("로그아웃실패(IO예외)", e);
            return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "로그아웃실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("로그아웃예외", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "로그아웃예외: " + e.getMessage());
        }
    }

    /**
     * 가져오기Casdoor로그인재지정URL(Casdoor사용, 준비사용)
     * @param request HTTP요청 
     * @return 로그인재지정URL
     */
    @Override
    public AppResponse<String> getRedirectUrl(HttpServletRequest request) {
        try {
            log.debug("열기 가져오기Casdoor로그인재지정URL");

            if (StringUtils.isBlank(redirectUrl)) {
                log.warn("가져오기로그인재지정URL실패: redirectUrl매칭비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "redirectUrl매칭비어 있습니다");
            }

            // 호출Casdoor서비스가져오기로그인URL
            String signinUrl = casdoorAuthExtendService.getCustomSigninUrl(redirectUrl);

            // 사용외부모듈endpoint반환프론트엔드, 확인프론트엔드가능방문까지정상의주소
            String fullUrl =
                    externalEndPoint != null && !externalEndPoint.isEmpty() ? externalEndPoint + signinUrl : signinUrl;

            log.debug("가져오기Casdoor로그인재지정URL성공: {}", fullUrl);
            return AppResponse.success(fullUrl);
        } catch (org.casbin.casdoor.exception.AuthException e) {
            log.error("Casdoor인증예외", e);
            return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "가져오기로그인재지정URL실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("가져오기로그인재지정URL예외", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기로그인재지정URL예외: " + e.getMessage());
        }
    }

    /**
     * Casdoor OAuth로그인(Casdoor사용, 준비사용)
     * @param code OAuth권한 부여코드
     * @param state OAuth state매개변수
     * @param request HTTP요청 
     * @return 사용자 정보
     */
    @Override
    public AppResponse<com.iflytek.rpa.auth.core.entity.User> signIn(
            String code, String state, HttpServletRequest request) throws IOException {
        //        try {
        //            log.debug("열기 Casdoor OAuth로그인, code: {}, state: {}", code, state);
        //
        //            // 매개변수검증
        //            if (StringUtils.isBlank(code)) {
        //                log.warn("OAuth로그인실패: 권한 부여코드비어 있습니다");
        //                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "권한 부여코드비워 둘 수 없습니다");
        //            }
        //
        //            OAuthJSONAccessTokenResponse oAuthTokenResponse =
        // casdoorAuthExtendService.getOAuthTokenResponse(code, state);
        //            String accessToken = oAuthTokenResponse.getAccessToken();
        //            String refreshToken = oAuthTokenResponse.getRefreshToken();
        //            String idToken = accessToken;
        //            // 가져오기시스템내부인증서, 에서initDataNewOnly로true시, 인증서수정
        //            ApplicationExtend applicationWithKey =
        // applicationExtendService.getApplicationWithKey("app-built-in");
        //            // 사용idToken파싱사용자 정보(예OIDC의: 에서id_token가져오기사용자)
        //            User user = authExtendService.parseJwtTokenWithCertificate(idToken,
        // applicationWithKey.certPublicKey);
        //
        //            // 1. 를사용자 정보저장까지session중(Spring Session관리관리Redis저장)
        //            HttpSession session = request.getSession();
        //            session.setAttribute("user", user);
        //
        //            // 2. Spring Security인증위아래문서
        //            CustomUserDetails userDetails = new CustomUserDetails(user);
        //            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        //                    userDetails, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
        //            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        //            SecurityContextHolder.getContext().setAuthentication(authentication);
        //
        //            // 3. accessToken및refreshToken저장까지Redis, 서버호출Casdoor API사용
        //            long tokenExpireTime = 24 * 60 * 60; // 24시간경과시간(초)
        //            TokenManager.storeTokens(user.name, accessToken, refreshToken, tokenExpireTime);
        //
        //            log.info("사용자 {} 로그인성공, session및인증위아래문서완료, 서버token완료저장", user.name);
        //
        //            // 6. 변환로통신사용User객체
        //            com.iflytek.rpa.auth.core.entity.User commonUser = userMapper.toCommonUser(user);
        //
        //            log.info("사용자 {} OAuth로그인성공, session및인증위아래문서완료, 서버token완료저장", user.name);
        //            return AppResponse.success(commonUser);
        //        } catch (org.casbin.casdoor.exception.AuthException e) {
        //            log.error("Casdoor인증예외", e);
        //            return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "OAuth로그인실패: " + e.getMessage());
        //        } catch (Exception e) {
        //            log.error("OAuth로그인예외", e);
        //            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "OAuth로그인예외: " + e.getMessage());
        //        }
        return null;
    }

    /**
     * 조회사용자로그인상태(Casdoor사용)
     * @param request HTTP요청 
     * @return 사용자 정보, 결과가로그인되지 않았습니다반환오류
     */
    @Override
    public AppResponse<com.iflytek.rpa.auth.core.entity.User> checkLoginStatus(HttpServletRequest request) {
        try {
            log.debug("열기 조회사용자로그인상태");

            // 1. 조회session여부저장에서
            javax.servlet.http.HttpSession session = request.getSession(false);
            if (session == null) {
                log.warn("조회로그인상태실패: session찾을 수 없습니다");
                return AppResponse.error(ErrorCodeEnum.E_NOT_LOGIN, "로그인되지 않았습니다");
            }

            // 2. 에서session중가져오기사용자 정보
            User casdoorUser = (User) session.getAttribute("user");
            if (casdoorUser == null) {
                log.warn("조회로그인상태실패: 사용자 정보찾을 수 없습니다");
                return AppResponse.error(ErrorCodeEnum.E_NOT_LOGIN, "사용자 정보찾을 수 없습니다");
            }

            // 3. 조회서버token여부있음
            boolean hasToken = TokenManager.hasToken(casdoorUser.name);
            if (!hasToken) {
                log.warn("조회로그인상태실패: 서버token완료경과, username: {}", casdoorUser.name);
                return AppResponse.error(ErrorCodeEnum.E_NOT_LOGIN, "서버token완료경과, 요청다시 로그인");
            }

            // 4. 변환로통신사용User객체
            com.iflytek.rpa.auth.core.entity.User commonUser = userMapper.toCommonUser(casdoorUser);

            log.debug("조회로그인상태성공, 사용자완료로그인, username: {}", casdoorUser.name);
            return AppResponse.success(commonUser);
        } catch (Exception e) {
            log.error("조회로그인상태예외", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회로그인상태실패: " + e.getMessage());
        }
    }

    /**
     * 새로고침서버token(Casdoor사용, accessToken경과시사용)
     * @param request HTTP요청 
     * @return 결과
     */
    @Override
    public AppResponse<String> refreshToken(HttpServletRequest request) {
        try {
            log.debug("열기 새로고침서버token");

            // 1. 에서session중가져오기사용자 정보
            javax.servlet.http.HttpSession session = request.getSession(false);
            if (session == null) {
                log.warn("새로고침token실패: session찾을 수 없습니다");
                return AppResponse.error(ErrorCodeEnum.E_NOT_LOGIN, "로그인되지 않았습니다");
            }

            User casdoorUser = (User) session.getAttribute("user");
            if (casdoorUser == null) {
                log.warn("새로고침token실패: 사용자 정보찾을 수 없습니다");
                return AppResponse.error(ErrorCodeEnum.E_NOT_LOGIN, "로그인되지 않았습니다");
            }

            // 2. 에서Redis가져오기refreshToken
            String refreshToken = TokenManager.getRefreshToken(casdoorUser.name);
            if (refreshToken == null || refreshToken.isEmpty()) {
                log.warn("새로고침token실패: RefreshToken찾을 수 없습니다, username: {}", casdoorUser.name);
                return AppResponse.error(ErrorCodeEnum.E_NOT_LOGIN, "RefreshToken찾을 수 없습니다, 요청다시 로그인");
            }

            // 3. 사용refreshToken가져오기새의token
            org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse newTokenResponse =
                    casdoorAuthExtendService.refreshToken(refreshToken, "read");

            if (newTokenResponse == null) {
                log.error("새로고침token실패: 가져오기새token비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "새로고침token실패: 비어 있습니다");
            }

            String newAccessToken = newTokenResponse.getAccessToken();
            String newRefreshToken = newTokenResponse.getRefreshToken();

            if (StringUtils.isBlank(newAccessToken)) {
                log.error("새로고침token실패: 새accessToken비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "새로고침token실패: 새accessToken비어 있습니다");
            }

            // 4. 업데이트Redis중의token
            long tokenExpireTime = 24 * 60 * 60; // 24시간경과시간(초)
            TokenManager.storeTokens(
                    casdoorUser.name,
                    newAccessToken,
                    newRefreshToken != null ? newRefreshToken : refreshToken,
                    tokenExpireTime);

            log.info("사용자 {} 의서버token완료새로고침", casdoorUser.name);
            return AppResponse.success("Token새로고침성공");
        } catch (org.casbin.casdoor.exception.AuthException e) {
            log.error("Casdoor인증예외", e);
            return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "새로고침token실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("새로고침token예외", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "새로고침token실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<PageDto<RobotExecute>> getDeployedUserListWithoutTenantId(
            GetDeployedUserListDto dto, HttpServletRequest request) {
        return AppResponse.success(new PageDto<RobotExecute>());
    }
}
