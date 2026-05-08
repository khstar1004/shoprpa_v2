package com.iflytek.rpa.auth.sp.uap.service.impl;

import static com.iflytek.rpa.auth.sp.uap.constants.AuthConstant.*;
import static com.iflytek.rpa.auth.sp.uap.constants.RedisKeyConstant.*;
import static com.iflytek.rpa.auth.utils.RedisUtil.deleteRedisKeysByPrefix;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iflytek.rpa.auth.conf.condition.ConditionalOnSaaSOrUAP;
import com.iflytek.rpa.auth.core.entity.*;
import com.iflytek.rpa.auth.core.service.AuthService;
import com.iflytek.rpa.auth.core.service.DeptService;
import com.iflytek.rpa.auth.core.service.UserService;
import com.iflytek.rpa.auth.exception.ServiceException;
import com.iflytek.rpa.auth.sp.uap.constants.UAPConstant;
import com.iflytek.rpa.auth.sp.uap.dao.*;
import com.iflytek.rpa.auth.sp.uap.entity.LoginResultDto;
import com.iflytek.rpa.auth.sp.uap.mapper.*;
import com.iflytek.rpa.auth.sp.uap.utils.*;
import com.iflytek.rpa.auth.utils.AppResponse;
import com.iflytek.rpa.auth.utils.ErrorCodeEnum;
import com.iflytek.rpa.auth.utils.RedisUtils;
import com.iflytek.sec.uap.base.util.ClientConfigUtil;
import com.iflytek.sec.uap.client.api.ClientAuthenticationAPI;
import com.iflytek.sec.uap.client.api.ClientManagementAPI;
import com.iflytek.sec.uap.client.api.UapUserInfoAPI;
import com.iflytek.sec.uap.client.core.client.ManagementClient;
import com.iflytek.sec.uap.client.core.dto.PageDto;
import com.iflytek.sec.uap.client.core.dto.ResponseDto;
import com.iflytek.sec.uap.client.core.dto.app.ListAppDto;
import com.iflytek.sec.uap.client.core.dto.app.UapApp;
import com.iflytek.sec.uap.client.core.dto.authentication.TicketDomain;
import com.iflytek.sec.uap.client.core.dto.extand.UapExtendPropertyDto;
import com.iflytek.sec.uap.client.core.dto.org.UapOrg;
import com.iflytek.sec.uap.client.core.dto.pwd.UpdatePwdDto;
import com.iflytek.sec.uap.client.core.dto.role.RoleBaseDto;
import com.iflytek.sec.uap.client.core.dto.role.UapRole;
import com.iflytek.sec.uap.client.core.dto.tenant.CreateTenantDto;
import com.iflytek.sec.uap.client.core.dto.tenant.ListTenantDto;
import com.iflytek.sec.uap.client.core.dto.tenant.TenantAppDto;
import com.iflytek.sec.uap.client.core.dto.tenant.TenantBindUserDto;
import com.iflytek.sec.uap.client.core.dto.tenant.TenantUserDto;
import com.iflytek.sec.uap.client.core.dto.tenant.UapTenant;
import com.iflytek.sec.uap.client.core.dto.user.*;
import com.iflytek.sec.uap.client.core.dto.user.BindRoleDto;
import com.iflytek.sec.uap.client.core.dto.user.CreateUapUserDto;
import com.iflytek.sec.uap.client.core.dto.user.CreateUserDto;
import com.iflytek.sec.uap.client.core.dto.user.GetUserDto;
import com.iflytek.sec.uap.client.core.dto.user.ListUserByRoleDto;
import com.iflytek.sec.uap.client.core.dto.user.ListUserDto;
import com.iflytek.sec.uap.client.core.dto.user.UpdateUapUserDto;
import com.iflytek.sec.uap.client.core.dto.user.UpdateUserDto;
import com.iflytek.sec.uap.client.core.dto.user.UserExtendDto;
import com.iflytek.sec.uap.client.core.dto.userpool.CreatePoolUserDto;
import com.iflytek.sec.uap.client.core.dto.userpool.CreateUapPoolUserDto;
import com.iflytek.sec.uap.client.core.dto.userpool.UpdatePoolUserDto;
import com.iflytek.sec.uap.client.core.dto.userpool.UpdateUapPoolUserDto;
import com.iflytek.sec.uap.client.util.SessionUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Base64Utils;

/**
 * @author mjren
 * @date 2025-03-06 15:22
 * @copyright Copyright (c) 2025 mjren
 */
@Slf4j
@Service("userService")
@ConditionalOnSaaSOrUAP
public class UserServiceImpl implements UserService {

    @Value("${uap.database.name:uap_db}")
    private String databaseName;

    @Autowired
    private DeptService deptService;

    @Autowired
    private DeptDao deptDao;

    @Autowired
    RoleDao roleDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserEntitlementDao userEntitlementDao;

    @Autowired
    private CreateUapUserDtoMapper createUapUserDtoMapper;

    @Autowired
    private UpdateUapUserDtoMapper updateUapUserDtoMapper;

    @Autowired
    private GetDeptOrUserDtoMapper getDeptOrUserDtoMapper;

    @Autowired
    private ListUserDtoMapper listUserDtoMapper;

    @Autowired
    private ListUserByRoleDtoMapper listUserByRoleDtoMapper;

    @Autowired
    private GetUserDtoMapper getUserDtoMapper;

    @Autowired
    private UserExtendDtoMapper userExtendDtoMapper;

    @Autowired
    private AuthService authService;

    private Integer smsRetryMax = 3;
    /**
     * 제어-직선연결추가사용자
     */
    public AppResponse<String> addUser(AddUserDto dto, HttpServletRequest request) {
        ManagementClient managementClient = UapManagementClientUtil.getManagementClient(request);
        RegisterDto registerDto = RegisterDto.builder().build();
        BeanUtils.copyProperties(dto, registerDto);
        registerDto.setLoginName(dto.getName());
        String userId = addPoolUser(buildPoolUser(registerDto), managementClient);
        updateInitialPassword(registerDto);
        doBindTenantRoleDept(dto, request, userId, managementClient);
        return AppResponse.success(userId);
    }

    public void doBindTenantRoleDept(AddUserDto dto, HttpServletRequest request) {
        String userId = userDao.getUserIdByPhone(dto.getPhone(), databaseName);
        ManagementClient managementClient = UapManagementClientUtil.getManagementClient(request);
        doBindTenantRoleDept(dto, request, userId, managementClient);
    }

    private void doBindTenantRoleDept(
            AddUserDto dto, HttpServletRequest request, String userId, ManagementClient managementClient) {
        String tenantId = UapUserInfoAPI.getTenantId(request);
        // 지정까지지정테넌트
        TenantBindUserDto tenantBindUserDto = new TenantBindUserDto();
        tenantBindUserDto.setTenantId(tenantId);
        tenantBindUserDto.setUserIds(Collections.singletonList(userId));
        ResponseDto<String> bindResponse = managementClient.bindTenantUser(tenantBindUserDto);
        if (!bindResponse.isFlag()) {
            throw new ServiceException(bindResponse.getMessage());
        }
        // 지정까지지정의역할
        String roleId = dto.getRoleId();
        bindRole(userId, roleId, tenantId);
        Integer existsCount = roleDao.checkTenantRoleExists(databaseName, tenantId, roleId);
        if (existsCount == null || existsCount == 0) {
            roleDao.insertTenantRole(databaseName, tenantId, roleId);
        }
        String orgId = dto.getOrgId();
        com.iflytek.rpa.auth.core.entity.UpdateUserDto updateUserDto =
                new com.iflytek.rpa.auth.core.entity.UpdateUserDto();
        UapUser user = UserUtils.getUserInfoById(userId);
        BeanUtils.copyProperties(user, updateUserDto);
        com.iflytek.rpa.auth.core.entity.UpdateUapUserDto updateUapUserDto =
                new com.iflytek.rpa.auth.core.entity.UpdateUapUserDto();
        updateUserDto.setOrgId(orgId);
        updateUapUserDto.setUser(updateUserDto);
        UpdateUapUserDto uapUpdateUapUserDto = updateUapUserDtoMapper.toUapUpdateUapUserDto(updateUapUserDto);
        ResponseDto<String> updateUserResponse = managementClient.updateUser(uapUpdateUapUserDto);
        if (!updateUserResponse.isFlag()) {
            throw new ServiceException(updateUserResponse.getMessage());
        }
    }

    /**
     * 회원가입
     * @param registerDto 회원가입정보
     * @param request HTTP요청 
     * @return 테넌트ID
     */
    @Override
    public AppResponse<String> register(RegisterDto registerDto, HttpServletRequest request) {
        ManagementClient managementClient = UapManagementClientUtil.getManagementClient(request);
        String userId = addPoolUser(buildPoolUser(registerDto), managementClient);
        updateInitialPassword(registerDto);
        String tenantId = createPersonalTenantAndBindRpa(userId, registerDto.getLoginName(), managementClient);

        // 1. 조회"회원가입역할"의역할ID
        String registerRoleId = roleDao.getRoleIdByName(databaseName, "회원가입역할");
        if (StringUtils.isBlank(registerRoleId)) {
            log.warn("찾을 수 없는 '회원가입역할', 사용역할ID: 1");
            registerRoleId = "1";
        }

        // 2. 지정사용자까지회원가입역할
        bindRole(userId, registerRoleId, tenantId);

        // 3. 에서 t_uap_tenant_role 테이블중삽입테넌트및역할의닫기 정보(결과가찾을 수 없습니다)
        Integer existsCount = roleDao.checkTenantRoleExists(databaseName, tenantId, registerRoleId);
        if (existsCount == null || existsCount == 0) {
            roleDao.insertTenantRole(databaseName, tenantId, registerRoleId);
            log.info("완료삽입테넌트역할닫기 , 테넌트ID: {}, 역할ID: {}", tenantId, registerRoleId);
        } else {
            log.debug("테넌트역할닫기 완료저장에서, 테넌트ID: {}, 역할ID: {}", tenantId, registerRoleId);
        }

        return AppResponse.success(tenantId);
    }

    private CreatePoolUserDto buildPoolUser(RegisterDto registerDto) {
        CreatePoolUserDto user = new CreatePoolUserDto();
        user.setLoginName(registerDto.getPhone());
        user.setPhone(registerDto.getPhone());
        user.setName(registerDto.getLoginName());
        return user;
    }

    private void updateInitialPassword(RegisterDto registerDto) {
        if (StringUtils.isEmpty(registerDto.getPassword())) {
            return;
        }
        UpdatePwdDto updatePwdDto = new UpdatePwdDto();
        updatePwdDto.setLoginName(registerDto.getPhone());
        updatePwdDto.setOldPwd(
                Base64Utils.encodeToString(UAPConstant.DEFAULT_INITIAL_PASSWORD.getBytes(StandardCharsets.UTF_8)));
        updatePwdDto.setNewPwd(
                Base64Utils.encodeToString(registerDto.getPassword().getBytes(StandardCharsets.UTF_8)));
        ResponseDto<String> updatePwdResponse = ClientAuthenticationAPI.updateUserPwd(updatePwdDto);
        if (!updatePwdResponse.isFlag()) {
            throw new ServiceException(updatePwdResponse.getMessage());
        }
    }

    public String createPersonalTenantAndBindRpa(String userId, String loginName, ManagementClient managementClient) {
        CreateTenantDto createTenantDto = buildPersonalTenant(userId, loginName);
        UapApp rpaApp = getRpaClientApp(managementClient);
        TenantAppDto tenantAppDto = new TenantAppDto();
        tenantAppDto.setId(rpaApp.getId());
        createTenantDto.setAppList(Collections.singletonList(tenantAppDto));
        ResponseDto<String> tenantResponse = managementClient.addTenant(createTenantDto);
        if (!tenantResponse.isFlag()) {
            throw new ServiceException(tenantResponse.getMessage());
        }
        return tenantResponse.getData();
    }

    private static final String SUFFIX = "의빈";
    private static final int TENANT_NAME_SALT_LENGTH = 6;
    private static final char TENANT_NAME_SEPARATOR = '#';

    private CreateTenantDto buildPersonalTenant(String userId, String loginName) {
        CreateTenantDto createTenantDto = new CreateTenantDto();
        createTenantDto.setTenantCode(UAPConstant.PERSONAL_TENANT_CODE + userId);
        createTenantDto.setName(buildTenantDisplayName(loginName));
        createTenantDto.setStatus(1);
        TenantUserDto tenantUserDto = new TenantUserDto();
        tenantUserDto.setId(userId);
        createTenantDto.setAdminList(Collections.singletonList(tenantUserDto));
        return createTenantDto;
    }

    /**
     * 완료기기후의테넌트이름,  UAP 이름일.
     */
    private String buildTenantDisplayName(String loginName) {
        String salt =
                RandomStringUtils.randomAlphanumeric(TENANT_NAME_SALT_LENGTH).toLowerCase(Locale.ROOT);
        return loginName + SUFFIX + TENANT_NAME_SEPARATOR + salt;
    }

    private UapApp getRpaClientApp(ManagementClient managementClient) {
        // todo:  전rpa클라이언트및중예에서robot목록 , 으로에서uap매칭공유사용완료일개code, 개code완료중.
        // 후rpa클라이언트및중분열기후, 있음아니요의code, 아래필요rpa클라이언트의code, 가능변경의까지로그인의권한제어.

        ListAppDto listAppDto = new ListAppDto();
        //        listAppDto.setAppName(UAPConstant.RPA_CLIENT_NAME);
        listAppDto.setAppName(UAPConstant.RPA_ADMIN_NAME);
        ResponseDto<PageDto<UapApp>> appPageResponse = managementClient.queryAppPageList(listAppDto);
        if (!appPageResponse.isFlag()) {
            throw new ServiceException(appPageResponse.getMessage());
        }
        PageDto<UapApp> appPage = appPageResponse.getData();
        List<UapApp> appList = appPage == null ? Collections.emptyList() : appPage.getResult();
        return appList.stream()
                .filter(app -> UAPConstant.RPA_ADMIN_NAME.equals(app.getName()))
                .findFirst()
                .orElseThrow(() -> new ServiceException("RPA클라이언트찾을 수 없습니다"));
    }

    // @Override
    // public AppResponse<String> register(RegisterDto registerDto, HttpServletRequest request) {
    //     //todo 입력매개복호화
    //     if (!registerDto.getPassWord().equals(registerDto.getConfirmPassword())) {
    //         return AppResponse.error(ErrorCodeEnum.E_PARAM, "입력한 비밀번호가 올바르지 않습니다");
    //     }
    //     //인증 코드검증
    //     String verifyCode = getVerifyCode(registerDto.getPhone());
    //     if (StringUtils.equals(verifyCode, registerDto.getCode())) {

    //         return doRegister(registerDto, request);
    //     } else {
    //         return AppResponse.error(ErrorCodeEnum.E_PARAM, "인증 코드오류");
    //     }
    // }

    private AppResponse<String> doRegister(RegisterDto registerDto, HttpServletRequest request) {
        CreatePoolUserDto user = new CreatePoolUserDto();
        BeanUtils.copyProperties(registerDto, user);
        ManagementClient managementClient = UapManagementClientUtil.getManagementClient(request);
        // 추가사용자
        String userId = addPoolUser(user, managementClient);
        // 를비밀번호수정로사용자입력의비밀번호
        UpdatePwdDto updatePwdDto = new UpdatePwdDto();
        updatePwdDto.setLoginName(registerDto.getLoginName());
        updatePwdDto.setOldPwd(Base64Utils.encodeToString("y3#J3vm!4hJ8k2v".getBytes()));
        updatePwdDto.setNewPwd(
                Base64Utils.encodeToString(registerDto.getPassword().getBytes()));
        ResponseDto<String> updatePwdResponse = ClientAuthenticationAPI.updateUserPwd(updatePwdDto);
        if (!updatePwdResponse.isFlag()) {
            throw new ServiceException(updatePwdResponse.getMessage());
        }
        // 지정까지개사람테넌트
        addToPersonalTenant(userId, managementClient);
        return AppResponse.success("회원가입성공");
    }

    public String getVerifyCode(String phone) {
        String smsPrefix = SMS_REGISTER_PREFIX;
        String retryNumPrefix = SMS_REGISTER_RETRY_NUM;
        String smsCode = SMS_REGISTER_CODE;
        Object retryNumStr = RedisUtils.get(smsPrefix + ":" + phone + ":" + retryNumPrefix);
        if (retryNumStr != null) {
            int retryTimes = (int) retryNumStr;
            // 제한제어재시도 데이터, 중지라이브러리,가져오기 아니요사용발송인증 코드가능대량회원가입
            if (retryTimes >= smsRetryMax) {
                RedisUtils.del(smsPrefix + ":" + phone + ":" + retryNumPrefix);
                RedisUtils.del(smsPrefix + ":" + phone + ":" + smsCode);
                return null;
            }
        }
        RedisUtils.incr(smsPrefix + ":" + phone + ":" + retryNumPrefix, 1);
        return String.valueOf(RedisUtils.get(smsPrefix + ":" + phone + ":" + smsCode));
    }

    /**
     * 삭제요소
     * @param userDto 삭제요소DTO
     * @param request HTTP요청 
     * @return 결과
     */
    @Override
    public AppResponse<String> deleteUser(UserDeleteDto userDto, HttpServletRequest request) {
        List<String> userIdList = userDto.getUserIdList();
        if (CollectionUtil.isEmpty(userIdList)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        ManagementClient managementClient = UapManagementClientUtil.getManagementClient(request);
        for (String userId : userIdList) {
            com.iflytek.sec.uap.client.core.dto.DeleteCommonDto deleteCommonDto =
                    new com.iflytek.sec.uap.client.core.dto.DeleteCommonDto();
            deleteCommonDto.setId(userId);
            ResponseDto<String> deleteResponse = managementClient.deleteUser(deleteCommonDto);
            if (!deleteResponse.isFlag()) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, deleteResponse.getMessage());
            }
        }
        // 모듈사용자저장
        deleteRedisKeysByPrefix(REDIS_KEY_DEPT_USER_PREFIX);
        // 테넌트사용자저장
        deleteRedisKeysByPrefix(REDIS_KEY_TENANT_USER_PREFIX);
        return AppResponse.success("완료");
    }

    /**
     * 사용/사용 안 함요소
     * 변수변경사용자정보 가능통신경과닫기연결, 통신사용자연결권한이 없습니다사용자본정보
     * @param userDto 사용/사용 안 함DTO
     * @param request HTTP요청 
     * @return 결과
     */
    @Override
    public AppResponse<String> enableUser(UserEnableDto userDto, HttpServletRequest request) {
        /*
        변수변경사용자정보 가능통신경과닫기연결, 통신사용자연결권한이 없습니다사용자본정보
         */
        List<com.iflytek.rpa.auth.core.entity.UpdateUserDto> updateUserDtoList = userDto.getUserList();
        Integer status = userDto.getStatus();
        if (CollectionUtil.isEmpty(updateUserDtoList) || status == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        if (!status.equals(0) && !status.equals(1)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM);
        }
        // ManagementClient managementClient = UapManagementClientUtil.getManagementClient(request);
        for (com.iflytek.rpa.auth.core.entity.UpdateUserDto updateUserDto : updateUserDtoList) {
            if (updateUserDto == null || StringUtils.isBlank(updateUserDto.getId())) {
                continue;
            }
            String tenantUserId =
                    tenantDao.getTenantUserId(databaseName, updateUserDto.getId(), UapUserInfoAPI.getTenantId(request));
            Integer i = tenantDao.enableTenantUser(databaseName, tenantUserId, status);
            if (i == null || i == 0) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "실패");
            }
            /*            UpdateUapPoolUserDto updateUapPoolUserDto = new UpdateUapPoolUserDto();
            UpdatePoolUserDto poolUserDto = new UpdatePoolUserDto();
            poolUserDto.setId(updateUserDto.getId());
            poolUserDto.setLoginName(updateUserDto.getLoginName());
            poolUserDto.setStatus(status);
            updateUapPoolUserDto.setUser(poolUserDto);
            ResponseDto<String> updateUserResponse = managementClient.updatePoolUser(updateUapPoolUserDto);

            if (!updateUserResponse.isFlag()) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, updateUserResponse.getMessage());
            }*/
        }
        return AppResponse.success("완료");
    }

    private String addPoolUser(CreatePoolUserDto user, ManagementClient managementClient) {

        CreateUapPoolUserDto createUapPoolUserDto = new CreateUapPoolUserDto();
        createUapPoolUserDto.setUser(user);
        ResponseDto<String> addPoolUserResponse = managementClient.addPoolUser(createUapPoolUserDto);
        if (!addPoolUserResponse.isFlag()) {
            throw new ServiceException(addPoolUserResponse.getMessage());
        }
        return addPoolUserResponse.getData();
    }

    private void addToPersonalTenant(String userId, ManagementClient managementClient) {
        // 조회개사람테넌트
        ListTenantDto listTenantDto = new ListTenantDto();
        listTenantDto.setName(PERSONAL_TENANT_NAME);
        ResponseDto<PageDto<UapTenant>> tenantPageResponse = managementClient.queryTenantPageList(listTenantDto);
        if (!tenantPageResponse.isFlag()) {
            throw new ServiceException(tenantPageResponse.getMessage());
        }
        List<UapTenant> tenantList = tenantPageResponse.getData().getResult();
        UapTenant personalTenant = null;
        for (UapTenant tenant : tenantList) {
            if (null == tenant) {
                continue;
            }
            if (PERSONAL_TENANT_NAME.equals(tenant.getName())) {
                personalTenant = tenant;
            }
        }
        if (null == personalTenant) {
            // todo 삭제사용자
            throw new ServiceException("찾을 수 없는 개사람테넌트");
        }
        // 지정까지개사람테넌트
        TenantBindUserDto tenantBindUserDto = new TenantBindUserDto();
        tenantBindUserDto.setTenantId(personalTenant.getId());
        tenantBindUserDto.setUserIds(Collections.singletonList(userId));
        ResponseDto<String> bindResponse = managementClient.bindTenantUser(tenantBindUserDto);
        if (!bindResponse.isFlag()) {
            throw new ServiceException(bindResponse.getMessage());
        }
    }

    /**
     * 이름검색모든요소또는모듈
     * @param name 이름
     * @param request HTTP요청 
     * @return 모듈또는사용자 정보
     */
    @Override
    public AppResponse<GetDeptOrUserDto> searchDeptOrUser(String name, HttpServletRequest request) {
        String tenantId = UapUserInfoAPI.getTenantId(request);

        List<UapUser> uapUsers = userDao.queryUapUserByName(name, tenantId, databaseName);
        List<UapOrg> deptList = deptDao.queryUapOrgByName(name, tenantId, databaseName);

        GetDeptOrUserDto result = getDeptOrUserDtoMapper.toCoreGetDeptOrUserDto(uapUsers, deptList);

        return AppResponse.success(result);
    }

    /**
     * 사용자 정보, 사용자허용수정기기및역할, 본정보아니요허용수정
     *
     * @param updateUapUserDto
     * @param request
     * @return
     */
    @Override
    public AppResponse<String> editUser(
            com.iflytek.rpa.auth.core.entity.UpdateUapUserDto updateUapUserDto, HttpServletRequest request) {
        // core유형변환uap유형
        UpdateUapUserDto uapUpdateUapUserDto = updateUapUserDtoMapper.toUapUpdateUapUserDto(updateUapUserDto);
        // 1. 매개변수검증
        UpdateUserDto userInfo = uapUpdateUapUserDto.getUser();
        if (isInvalidUserInfo(userInfo)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }

        // 2. 가져오기 관리역할정보
        String roleId = extractAndRemoveRoleId(uapUpdateUapUserDto);

        String userId = userInfo.getId();

        // 3. 가져오기사용자현재역할
        String oldRoleId = getCurrentUserRoleId(userId, userInfo.getLoginName(), request);

        // 4. 관리역할지정/해제
        handleRoleBinding(userId, roleId, oldRoleId, request);

        // 5. 업데이트사용자본정보
        return updateUserBasicInfo(uapUpdateUapUserDto, request);
    }

    // 방법법: 검증사용자 정보
    private boolean isInvalidUserInfo(UpdateUserDto userInfo) {
        return userInfo == null || userInfo.getLoginName() == null || userInfo.getStatus() == null;
    }

    // 에서속성중가져오기 제거역할ID
    private String extractAndRemoveRoleId(UpdateUapUserDto updateUapUserDto) {
        List<UapExtendPropertyDto> extands = updateUapUserDto.getExtands();

        String roleId = null;
        List<UapExtendPropertyDto> toRemove = new ArrayList<>();
        for (UapExtendPropertyDto extendInfo : extands) {
            if (extendInfo.getId().equals("roleId")) {
                roleId = extendInfo.getValue();
                toRemove.add(extendInfo);
            }
        }
        extands.removeAll(toRemove);
        updateUapUserDto.setExtands(extands);
        return roleId;
    }

    // 가져오기사용자현재역할ID
    private String getCurrentUserRoleId(String userId, String loginName, HttpServletRequest request) {
        GetUserDto getUserDto = new GetUserDto();
        getUserDto.setUserId(userId);
        getUserDto.setLoginName(loginName);

        List<UapRole> roleList = ClientManagementAPI.queryRoleListByUserId(UapUserInfoAPI.getTenantId(request), userId);

        if (roleList.size() > 1) {
            throw new ServiceException("사용자저장에서다중개지정역할");
        }

        return roleList.get(0) != null ? roleList.get(0).getId() : null;
    }

    // 관리역할지정
    private void handleRoleBinding(String userId, String newRoleId, String oldRoleId, HttpServletRequest request) {
        String tenantId = UapUserInfoAPI.getTenantId(request);
        // 1: 새역할ID비어 있습니다역할ID아니요비어 있습니다 - 해제역할
        if (StringUtils.isBlank(newRoleId) && StringUtils.isNotBlank(oldRoleId)) {
            unbindRole(userId, oldRoleId, tenantId);
        }
        // 역할[지정되지 않았습니다]
        if (StringUtils.isEmpty(newRoleId)) {
            newRoleId = "1";
        }
        // 2: 새역할ID아니요비어 있습니다및역할아니요 - 해제역할지정새역할
        if (StringUtils.isNotBlank(newRoleId) && !newRoleId.equals(oldRoleId)) {
            if (StringUtils.isNotBlank(oldRoleId)) {
                unbindRole(userId, oldRoleId, tenantId);
            }
            bindRole(userId, newRoleId, tenantId);
        }
    }

    // 해제역할
    private void unbindRole(String userId, String roleId, String tenantId) {
        BindRoleDto bindRoleDto = createBindRoleDto(userId, roleId);
        ResponseDto<Object> response = ClientManagementAPI.unbindUserRole(tenantId, bindRoleDto);
        if (!response.isFlag()) {
            throw new ServiceException(response.getMessage());
        }
    }

    // 지정역할
    private void bindRole(String userId, String roleId, String tenantId) {
        BindRoleDto bindRoleDto = createBindRoleDto(userId, roleId);
        ResponseDto<Object> response = ClientManagementAPI.bindUserRole(tenantId, bindRoleDto);
        if (!response.isFlag()) {
            throw new ServiceException(response.getMessage());
        }
    }

    // 생성역할지정DTO
    private BindRoleDto createBindRoleDto(String userId, String roleId) {
        BindRoleDto bindRoleDto = new BindRoleDto();
        bindRoleDto.setUserId(userId);
        bindRoleDto.setRoleIdList(Collections.singletonList(roleId));
        return bindRoleDto;
    }

    /**
     * 사용자있음역할시, 분매칭회원가입역할다시 가져오기역할목록
     *
     * @param tenantId 테넌트ID
     * @param userId 사용자ID
     * @param accessToken 방문브랜드
     * @param loginName 로그인이름(사용로그)
     * @return 역할목록, 결과가분매칭실패이면반환빈목록
     */
    private List<UapRole> assignRegisterRoleIfNeeded(
            String tenantId, String userId, String accessToken, String loginName) {
        try {
            // 조회"회원가입역할"의역할ID
            String registerRoleId = roleDao.getRoleIdByName(databaseName, "회원가입역할");
            if (StringUtils.isBlank(registerRoleId)) {
                log.warn("찾을 수 없는 '회원가입역할', 사용역할ID: 1");
                registerRoleId = "1";
            }

            // 지정사용자까지회원가입역할
            bindRole(userId, registerRoleId, tenantId);
            log.info("완료로사용자 {} 지정회원가입역할, 역할ID: {}", loginName, registerRoleId);

            // 확인테넌트역할닫기 저장에서
            Integer existsCount = roleDao.checkTenantRoleExists(databaseName, tenantId, registerRoleId);
            if (existsCount == null || existsCount == 0) {
                roleDao.insertTenantRole(databaseName, tenantId, registerRoleId);
                log.info("완료삽입테넌트역할닫기 , 테넌트ID: {}, 역할ID: {}", tenantId, registerRoleId);
            }

            // 다시 가져오기역할목록
            List<UapRole> roleList = ClientAuthenticationAPI.getUserRoleListInApp(tenantId, userId, accessToken);
            if (CollectionUtil.isEmpty(roleList)) {
                log.warn("지정회원가입역할후, 다시 가져오기역할목록 비어 있습니다");
            }
            return roleList != null ? roleList : Collections.emptyList();
        } catch (Exception e) {
            log.error("로사용자 {} 지정회원가입역할실패", loginName, e);
            return Collections.emptyList();
        }
    }

    // 업데이트사용자본정보
    private AppResponse<String> updateUserBasicInfo(UpdateUapUserDto updateUapUserDto, HttpServletRequest request) {
        UpdateUserDto user = updateUapUserDto.getUser();
        ManagementClient managementClient = UapManagementClientUtil.getManagementClient(request);
        // 연결, 업데이트사용자본정보, 예메일함, 이름대기, 아니요패키지모듈및역할
        UpdateUapPoolUserDto updateUapPoolUserDto = new UpdateUapPoolUserDto();
        UpdatePoolUserDto poolUser = new UpdatePoolUserDto();
        BeanUtils.copyProperties(user, poolUser);
        updateUapPoolUserDto.setUser(poolUser);
        ResponseDto<String> updateUserResponse = managementClient.updatePoolUser(updateUapPoolUserDto);
        if (!updateUserResponse.isFlag()) {
            return AppResponse.error(updateUserResponse.getMessage());
        }
        if (StringUtils.isBlank(user.getOrgId())) {
            return AppResponse.success("완료, 지정되지 않았습니다모듈");
        }
        // 업데이트모듈
        user.setUserType(3);
        UapUser userInfo = UserUtils.getUserInfoById(user.getId());
        if (null == userInfo) {
            return AppResponse.error("로조회까지추가의사용자 정보");
        }
        // email,가능으로예빈문자열,아니요이면오류: 테넌트빈내부불가사용자
        user.setEmail(userInfo.getEmail());
        user.setIdNumber(userInfo.getIdNumber());
        user.setRemark(userInfo.getRemark());
        user.setAddress(userInfo.getAddress());
        ResponseDto<String> response = managementClient.updateUser(updateUapUserDto);
        if (!response.isFlag()) {
            throw new ServiceException(response.getMessage());
        }

        // 모듈사용자저장
        deleteRedisKeysByPrefix(REDIS_KEY_DEPT_USER_PREFIX);
        // 테넌트사용자저장
        deleteRedisKeysByPrefix(REDIS_KEY_TENANT_USER_PREFIX);

        return AppResponse.success("완료");

        // todo 작업일실패, 돌아가기의
    }

    /**
     * 추가사용자
     *
     * @param createUapUserDto
     * @param request
     * @return
     */
    @Override
    public AppResponse<String> addUser(
            com.iflytek.rpa.auth.core.entity.CreateUapUserDto createUapUserDto, HttpServletRequest request) {
        CreateUapUserDto uapCreateUapUserDto = createUapUserDtoMapper.toUapCreateUapUserDto(createUapUserDto);
        // 1. 매개변수검증
        validateCreateUserDto(uapCreateUapUserDto);

        // 2. 관리모듈분매칭
        handleDefaultOrganization(uapCreateUapUserDto, request);

        // 3. 생성사용자
        ManagementClient managementClient = UapManagementClientUtil.getManagementClient(request);
        String userId = createPoolUser(uapCreateUapUserDto.getUser(), managementClient);
        if (userId == null) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "생성사용자실패");
        }

        // 4. 테넌트지정사용자
        bindTenantUser(userId, request);

        // 5. 사용자지정모듈및역할
        bindOrganizationAndRole(userId, uapCreateUapUserDto, request);

        // 지정개사람빈
        addToPersonalTenant(userId, managementClient);

        return AppResponse.success("완료");
        // todo 작업일실패, 돌아가기전의
    }

    private void bindTenantUser(String userId, HttpServletRequest request) {
        TenantBindUserDto tenantBindUserDto = new TenantBindUserDto();
        tenantBindUserDto.setTenantId(UapUserInfoAPI.getTenantId(request));
        tenantBindUserDto.setUserIds(Collections.singletonList(userId));
        ManagementClient managementClient = UapManagementClientUtil.getManagementClient(request);
        ResponseDto<String> bindResponse = managementClient.bindTenantUser(tenantBindUserDto);
        if (!bindResponse.isFlag()) {
            throw new ServiceException(bindResponse.getMessage());
        }
    }

    // 인증생성사용자매개변수
    private void validateCreateUserDto(CreateUapUserDto createUapUserDto) {
        CreateUserDto userDto = createUapUserDto.getUser();
        if (userDto == null || userDto.getLoginName() == null) {
            throw new ServiceException("사용자명은 비워 둘 수 없습니다");
        }
    }

    // 관리모듈분매칭
    private void handleDefaultOrganization(CreateUapUserDto createUapUserDto, HttpServletRequest request) {
        CreateUserDto userDto = createUapUserDto.getUser();

        String tenantId = UapUserInfoAPI.getTenantId(request);
        if (StringUtils.isNotBlank(userDto.getOrgId())) {
            return;
        }
        String unGroupOrgId = findUnassignedOrganization(tenantId, request);

        userDto.setOrgId(unGroupOrgId);
        createUapUserDto.setUser(userDto);
    }

    // 조회지원하지 않는그룹모듈
    private String findUnassignedOrganization(String tenantId, HttpServletRequest request) {
        OrgListDto orgListDto = new OrgListDto();
        orgListDto.setOrgName("지원하지 않는그룹");

        ResponseDto<PageDto<UapOrg>> orgPageList =
                UapManagementClientUtil.queryOrgPageList(tenantId, orgListDto, request);

        if (!orgPageList.isFlag()) {
            throw new ServiceException(orgPageList.getMessage());
        }

        if (orgPageList.getData() == null
                || CollectionUtil.isEmpty(orgPageList.getData().getResult())) {
            throw new ServiceException("찾을 수 없는 지원하지 않는그룹모듈정보");
        }

        String unGroupOrgId = findUnGroupOrgId(orgPageList.getData().getResult());
        if (unGroupOrgId == null) {
            throw new ServiceException("찾을 수 없는 지원하지 않는그룹모듈정보");
        }

        return unGroupOrgId;
    }

    // 방법법: 조회지원하지 않는그룹모듈ID
    private String findUnGroupOrgId(List<UapOrg> orgList) {
        return orgList.stream()
                .filter(org -> org != null && "지원하지 않는그룹".equals(org.getName()))
                .findFirst()
                .map(UapOrg::getId)
                .orElse(null);
    }

    // 생성사용자
    private String createPoolUser(CreateUserDto createUserDto, ManagementClient managementClient) {
        CreatePoolUserDto createPoolUserDto = new CreatePoolUserDto();
        BeanUtils.copyProperties(createUserDto, createPoolUserDto);
        return addPoolUser(createPoolUserDto, managementClient);
    }

    // 지정모듈및역할
    private void bindOrganizationAndRole(String userId, CreateUapUserDto createUapUserDto, HttpServletRequest request) {
        UpdateUapUserDto updateUapUserDto = new UpdateUapUserDto();
        UpdateUserDto user = new UpdateUserDto();

        user.setId(userId);
        user.setName(createUapUserDto.getUser().getName());
        // 유형로사용자
        user.setUserType(3);
        user.setLoginName(createUapUserDto.getUser().getLoginName());
        user.setPhone(createUapUserDto.getUser().getPhone());
        user.setOrgId(createUapUserDto.getUser().getOrgId());
        user.setEmail(createUapUserDto.getUser().getEmail());

        updateUapUserDto.setUser(user);
        updateUapUserDto.setExtands(createUapUserDto.getExtands());

        // uap변환core유형
        com.iflytek.rpa.auth.core.entity.UpdateUapUserDto coreUpdateUapUserDto =
                updateUapUserDtoMapper.fromUapUpdateUapUserDto(updateUapUserDto);
        editUser(coreUpdateUapUserDto, request);
    }

    /**
     * 분조회현재기기의사용자
     * @param listUserDto 조회파일
     * @param request HTTP요청 
     * @return 분사용자목록
     */
    @Override
    public AppResponse<com.iflytek.rpa.auth.core.entity.PageDto<DeptUserDto>> queryUserListByOrgId(
            com.iflytek.rpa.auth.core.entity.ListUserDto listUserDto, HttpServletRequest request) { //
        // core 까지 uap 
        ListUserDto uapListUserDto = listUserDtoMapper.toUapListUserDto(listUserDto);

        if (StringUtils.isBlank(uapListUserDto.getOrgId())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "적음모듈id");
        }
        if (uapListUserDto.getPageNum() == null || uapListUserDto.getPageSize() == null) {
            uapListUserDto.setPageNum(1);
            uapListUserDto.setPageSize(100);
        }
        // 조회전체상태
        uapListUserDto.setStatus(null);
        PageDto<UserExtendDto> userInfoPage =
                ClientManagementAPI.queryUserDetailPageList(UapUserInfoAPI.getTenantId(request), uapListUserDto);
        List<UserExtendDto> userExtendDtoList = userInfoPage.getResult();
        List<DeptUserDto> deptUserDtoList = new ArrayList<>();
        for (UserExtendDto userExtendDto : userExtendDtoList) {
            DeptUserDto deptUserDto = new DeptUserDto();
            UapUser user = userExtendDto.getUser();
            Integer tenantUserStatus =
                    tenantDao.getTenantUserStatus(databaseName, user.getId(), UapUserInfoAPI.getTenantId(request));
            user.setStatus(tenantUserStatus);
            BeanUtils.copyProperties(user, deptUserDto);
            List<RoleBaseDto> roleList = userExtendDto.getRoles();
            if (!CollUtil.isEmpty(roleList)) {
                if (roleList.size() > 1) {
                    //                    return AppResponse.error(ErrorCodeEnum.E_SERVICE, "사용자저장에서다중개지정역할");
                }
                RoleBaseDto role = roleList.get(0);
                if (null != role) {
                    deptUserDto.setRoleId(role.getId());
                    deptUserDto.setRoleName(role.getName());
                }
            }
            deptUserDtoList.add(deptUserDto);
        }
        com.iflytek.rpa.auth.core.entity.PageDto<DeptUserDto> deptUserPage =
                new com.iflytek.rpa.auth.core.entity.PageDto<>();
        deptUserPage.setResult(deptUserDtoList);
        deptUserPage.setPageSize(userInfoPage.getPageSize());
        deptUserPage.setCurrentPageNo(userInfoPage.getCurrentPageNo());
        deptUserPage.setTotalCount(userInfoPage.getTotalCount());
        return AppResponse.success(deptUserPage);
    }

    /**
     * 역할관리관리-근거모듈id조회모듈아래의사람원및모듈
     * @param id 모듈ID
     * @param request HTTP요청 
     * @return 모듈사용자목록
     */
    @Override
    public AppResponse<List<CurrentDeptUserDto>> queryUserAndDept(String id, HttpServletRequest request) {
        String tenantId = UapUserInfoAPI.getTenantId(request);

        // 결과가id로0, 이면조회중의id
        if (TOP_ORG_ID.equals(id)) {
            UapUser uapUser = UapUserInfoAPI.getLoginUser(request);
            String loginName = null == uapUser ? null : uapUser.getLoginName();
            String firstLevelOrgId = deptDao.queryFirstLevelOrgIdByLoginName(loginName, tenantId, databaseName);
            if (StringUtils.isBlank(firstLevelOrgId)) {
                return AppResponse.success(new ArrayList<>());
            }
            id = firstLevelOrgId;
        }

        List<CurrentDeptUserDto> result = new ArrayList<>();

        // 조회모듈아래의사람
        List<UserVo> userList = deptDao.queryUserListByDeptId(null, id, tenantId, databaseName);

        // 조회사용자중있음역할
        List<String> userIdList = new ArrayList<>();
        if (!CollectionUtil.isEmpty(userList)) {
            for (UserVo userVo : userList) {
                if (userVo != null && userVo.getUserId() != null) {
                    userIdList.add(userVo.getUserId());
                }
            }
        }

        // 가져오기있음역할의사용자ID및역할이름합치기
        List<UserRoleDto> usersWithRoles = new ArrayList<>();
        if (!CollectionUtil.isEmpty(userIdList)) {
            usersWithRoles = deptDao.queryUserIdsWithRoles(userIdList, tenantId, databaseName);
            usersWithRoles.removeIf(Objects::isNull);
        }

        Map<String, String> userIdMapRoleName = usersWithRoles.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(UserRoleDto::getUserId, UserRoleDto::getRoleName, (v1, v2) -> v1));

        if (!CollectionUtil.isEmpty(userList)) {
            // 상태
            for (UserVo userVo : userList) {
                if (null == userVo) {
                    continue;
                }
                CurrentDeptUserDto currentDeptUserDto = new CurrentDeptUserDto();
                currentDeptUserDto.setId(userVo.getUserId());
                currentDeptUserDto.setName(userVo.getUserName() + "(" + userVo.getUserPhone() + ")");
                currentDeptUserDto.setType(TYPE_USER);

                String roleName = null;
                boolean hasRole = false;
                if (userIdMapRoleName.containsKey(userVo.getUserId())) {
                    String roleN = userIdMapRoleName.get(userVo.getUserId());
                    if (roleN.trim().equals("지정되지 않았습니다")) {
                        roleName = roleN;
                    } else {
                        hasRole = true;
                        roleName = roleN;
                    }
                }
                currentDeptUserDto.setStatus(hasRole);
                currentDeptUserDto.setRoleName(roleName);
                result.add(currentDeptUserDto);
            }
        }

        // 조회모듈아래의단계모듈
        List<UserVo> childOrgList = deptDao.queryChildOrgsByParentOrgId(id, tenantId, databaseName);
        if (!CollectionUtil.isEmpty(childOrgList)) {
            childOrgList.removeIf(Objects::isNull);
            for (UserVo childOrg : childOrgList) {
                CurrentDeptUserDto currentDeptUserDto = new CurrentDeptUserDto();
                currentDeptUserDto.setId(childOrg.getUserId());
                currentDeptUserDto.setName(childOrg.getUserName());
                currentDeptUserDto.setType(TYPE_DEPT);
                result.add(currentDeptUserDto);
            }
        }
        return AppResponse.success(result);
    }

    /**
     * 역할관리관리-근거이름문자또는휴대폰 번호조회요소
     * @param keyWord 닫기 문자
     * @param request HTTP요청 
     * @return 사용자목록
     */
    @Override
    public AppResponse<List<CurrentDeptUserDto>> searchUserWithStatus(String keyWord, HttpServletRequest request) {
        String tenantId = UapUserInfoAPI.getTenantId(request);
        // 조회전체상태
        ListUserDto listUserDto = new ListUserDto();
        listUserDto.setPageNum(1);
        listUserDto.setPageSize(100);
        listUserDto.setName(keyWord);
        listUserDto.setStatus(null);
        PageDto<UserExtendDto> userInfoPageByName = ClientManagementAPI.queryUserDetailPageList(tenantId, listUserDto);
        List<UserExtendDto> userExtendDtoListByName = userInfoPageByName.getResult();
        listUserDto.setName(null);
        listUserDto.setPhone(keyWord);
        PageDto<UserExtendDto> userInfoPageByPhone = ClientManagementAPI.queryUserDetailPageList(tenantId, listUserDto);
        List<UserExtendDto> userExtendDtoListByPhone = userInfoPageByPhone.getResult();
        List<UserExtendDto> userExtendList = new ArrayList<>();
        if (!CollUtil.isEmpty(userExtendDtoListByName)) {
            userExtendList.addAll(userExtendDtoListByName);
        }
        if (!CollUtil.isEmpty(userExtendDtoListByPhone)) {
            userExtendList.addAll(userExtendDtoListByPhone);
        }
        if (CollUtil.isEmpty(userExtendList)) {
            return AppResponse.success(new ArrayList<>());
        }
        List<CurrentDeptUserDto> userList = new ArrayList<>();
        for (UserExtendDto userExtendDto : userExtendList) {
            CurrentDeptUserDto currentDeptUserDto = new CurrentDeptUserDto();
            UapUser user = userExtendDto.getUser();
            currentDeptUserDto.setId(user.getId());
            currentDeptUserDto.setName(user.getName() + "(" + user.getPhone() + ")");
            currentDeptUserDto.setType(TYPE_USER);
            List<RoleBaseDto> roleList = userExtendDto.getRoles();
            if (!CollUtil.isEmpty(roleList)) {
                if (roleList.size() > 1) {
                    return AppResponse.error(ErrorCodeEnum.E_SERVICE, "사용자저장에서다중개지정역할");
                }
                RoleBaseDto role = roleList.get(0);
                currentDeptUserDto.setStatus(null != role);
            }
            userList.add(currentDeptUserDto);
        }
        return AppResponse.success(userList);
    }

    /**
     * 역할관리관리-추가구성원
     * @param bindUserListDto 지정사용자목록DTO
     * @param request HTTP요청 
     * @return 결과
     */
    @Override
    @Transactional
    public AppResponse<String> bindUserListRole(BindUserListDto bindUserListDto, HttpServletRequest request) {
        if (StringUtils.isBlank(bindUserListDto.getRoleId()) || CollUtil.isEmpty(bindUserListDto.getUserIds())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        String tenantId = UapUserInfoAPI.getTenantId(request);
        // 새조회 userIds 모든 지정 역할[지정되지 않았습니다] 의 id
        List<String> userIds = bindUserListDto.getUserIds();
        String roleId = bindUserListDto.getRoleId();
        if (!roleId.equals("1")) {
            List<String> ids = roleDao.getBindUnspecifiedRoleIds(userIds, tenantId, databaseName);
            if (!ids.isEmpty()) {
                roleDao.batchDeleteUnspecifiedRoleBind(ids, databaseName);
            }
        }
        userIds.parallelStream().forEach(userId -> {
            BindRoleDto bindRoleDto = new BindRoleDto();
            bindRoleDto.setUserId(userId);
            bindRoleDto.setRoleIdList(Collections.singletonList(bindUserListDto.getRoleId()));
            ResponseDto<Object> bindRoleResponse = ClientManagementAPI.bindUserRole(tenantId, bindRoleDto);
            if (!bindRoleResponse.isFlag()) {
                throw new ServiceException(bindRoleResponse.getMessage());
            }
        });
        return AppResponse.success("추가성공");
    }

    /**
     * 사람원해제역할
     * @param bindRoleDto 지정역할DTO
     * @param request HTTP요청 
     * @return 결과
     */
    @Override
    public AppResponse<String> unbindRole(
            com.iflytek.rpa.auth.core.entity.BindRoleDto bindRoleDto, HttpServletRequest request) {

        if (StringUtils.isBlank(bindRoleDto.getUserId()) || CollectionUtil.isEmpty(bindRoleDto.getRoleIdList())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        String userId = bindRoleDto.getUserId();
        // 결과가저장에서사용자, 이면를해당역할아래의사용자까지"지정되지 않았습니다"역할
        if (userId != null && !userId.isEmpty()) {
            List<String> userIds = new ArrayList<>();
            userIds.add(userId);
            String tenantId = UapUserInfoAPI.getTenantId(request);
            roleDao.migrateUsersToUnspecifiedRole(databaseName, userIds, tenantId);
        }
        return AppResponse.success("해제완료");
    }

    /**
     * 분가져오기역할지정의사용자목록, 가능근거로그인이름또는이름조회
     * @param listUserByRoleDto 조회파일
     * @param request HTTP요청 
     * @return 분사용자목록
     */
    @Override
    public AppResponse<com.iflytek.rpa.auth.core.entity.PageDto<User>> queryBindListByRole(
            com.iflytek.rpa.auth.core.entity.ListUserByRoleDto listUserByRoleDto, HttpServletRequest request) {
        // core까지uap
        ListUserByRoleDto uapListUserByRoleDto = listUserByRoleDtoMapper.toUapListUserByRoleDto(listUserByRoleDto);
        if (uapListUserByRoleDto == null) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "요청 매개변수는 비워 둘 수 없습니다");
        }
        if (StringUtils.isBlank(uapListUserByRoleDto.getRoleId())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "적음역할id");
        }
        Integer pageNum = uapListUserByRoleDto.getPageNum();
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        Integer pageSize = uapListUserByRoleDto.getPageSize();
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > 1000) {
            pageSize = 1000;
        }
        IPage pageConfig = new Page<>(pageNum, pageSize, true);
        IPage<String> idPage = userDao.queryUserIdsByRole(pageConfig, uapListUserByRoleDto, databaseName);

        long total = idPage.getTotal();
        long current = idPage.getCurrent();
        long size = idPage.getSize();
        List<String> ids = idPage.getRecords();
        if (CollectionUtil.isEmpty(ids)) {
            com.iflytek.rpa.auth.core.entity.PageDto<User> emptyPageDto =
                    new com.iflytek.rpa.auth.core.entity.PageDto<>();
            emptyPageDto.setCurrentPageNo((int) current);
            emptyPageDto.setPageSize((int) size);
            emptyPageDto.setTotalCount((int) total);
            emptyPageDto.setResult(new ArrayList<>());
            return AppResponse.success(emptyPageDto);
        }
        String tenantId = UapUserInfoAPI.getTenantId(request);
        List<UapUser> list = userDao.queryUapUserByIds(ids, databaseName, tenantId);
        List<User> userList = userMapper.fromUapUsers(list);

        com.iflytek.rpa.auth.core.entity.PageDto<User> pageDto = new com.iflytek.rpa.auth.core.entity.PageDto<>();
        pageDto.setCurrentPageNo((int) current);
        pageDto.setPageSize((int) size);
        pageDto.setTotalCount((int) total);
        pageDto.setResult(userList);
        return AppResponse.success(pageDto);
    }

    public AppResponse<UapUser> loginNoPasswordByPhone(String phone, String tenantId, HttpServletRequest request) {
        if (StringUtils.isBlank(phone)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "휴대폰 번호는 비워 둘 수 없습니다");
        }
        if (StringUtils.isBlank(tenantId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "테넌트 ID는 비워 둘 수 없습니다");
        }
        try {
            ListUserDto listUserDto = new ListUserDto();
            listUserDto.setPhone(phone);
            listUserDto.setPageSize(1);
            PageDto<UapUser> userPage = ClientManagementAPI.queryUserPageList(tenantId, listUserDto);
            if (userPage == null || CollectionUtil.isEmpty(userPage.getResult())) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "찾을 수 없는 휴대폰 번호의사용자");
            }
            UapUser targetUser = userPage.getResult().get(0);
            if (targetUser == null || StringUtils.isBlank(targetUser.getLoginName())) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "휴대폰 번호지정되지 않았습니다로그인계정");
            }
            return loginNoPassword(targetUser.getLoginName(), tenantId, request);
        } catch (Exception e) {
            log.error("로그인실패, phone: {}, tenantId: {}", phone, tenantId, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "로그인실패: " + e.getMessage());
        }
    }

    /**
     * 없음비밀번호로그인(통신경과계정로그인)
     *
     * @param loginName 로그인계정
     * @param tenantId 테넌트ID(가능선택)
     * @param request HTTP요청 
     * @return 로그인결과, 패키지사용자 정보
     */
    public AppResponse<UapUser> loginNoPassword(String loginName, String tenantId, HttpServletRequest request) {
        try {
            // 1. 호출 ClientAuthenticationAPIExt.loginUapByAccount 가져오기 ticket
            log.info("열기 없음비밀번호로그인, 계정: {}, 테넌트ID: {}", loginName, tenantId);
            LoginResultDto loginResult = ClientAuthenticationAPIExt.loginUapByAccount(loginName, tenantId);

            if (loginResult == null || StringUtils.isBlank(loginResult.getTicket())) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기로그인인증실패");
            }

            String ticket = loginResult.getTicket();
            String service = ClientConfigUtil.instance().getCasClientContext();

            // 2. 호출 ClientAuthenticationAPI.validateTicket 인증 ticket
            // 비고: validateTicket 내부모듈관리 token 의저장까지 Redis(UAP완료)
            ResponseDto<TicketDomain> validateResponse = ClientAuthenticationAPI.validateTicket(ticket, service);

            if (validateResponse == null || !validateResponse.isFlag()) {
                String errorMsg = validateResponse != null ? validateResponse.getMessage() : "인증로그인인증실패";
                log.error("ticket 인증 실패: {}", errorMsg);
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, errorMsg);
            }

            // 3. 에서 validateTicket 의반환결과중가져오기 UapUser
            TicketDomain ticketDomain = validateResponse.getData();
            if (ticketDomain == null) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "인증 결과가 비어 있습니다");
            }

            UapUser uapUser = ticketDomain.getUapUser();
            if (uapUser == null) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "인증 결과에서 사용자 정보를 가져올 수 없습니다");
            }

            // 4. 를사용자 정보저장까지 session
            javax.servlet.http.HttpSession session = request.getSession(true);
            SessionUtil.getInstance().saveUser(session, uapUser);
            SessionUtil.getInstance().saveTenantId(session, tenantId);

            // 5. 에서 TicketDomain 중가져오기 저장 accessToken 및 refreshToken 까지 Redis
            // 사용및 UAP 내용의순서열방식(JdkSerializationRedisSerializer)
            String accessToken = ticketDomain.getAccessToken();
            String refreshToken = ticketDomain.getRefreshToken();

            // 통신경과accessToken가져오기테넌트 정보
            UapTenant uapTenant = ClientAuthenticationAPI.getTenantInfo(tenantId, accessToken);
            SessionUtil.getInstance().saveTenant(session, uapTenant);

            // 호출 ClientAuthenticationAPI.getUserRoleListInApp 가져오기사용자역할목록
            List<UapRole> roleList =
                    ClientAuthenticationAPI.getUserRoleListInApp(tenantId, uapUser.getId(), accessToken);
            if (CollectionUtil.isEmpty(roleList)) {
                log.warn("사용자 {} 에서테넌트 {} 중있음역할, 시도분매칭회원가입역할", uapUser.getLoginName(), tenantId);
                roleList = assignRegisterRoleIfNeeded(tenantId, uapUser.getId(), accessToken, uapUser.getLoginName());
            }

            // 저장역할목록까지 session
            if (!CollectionUtil.isEmpty(roleList)) {
                SessionUtil.getInstance().saveUserRole(session, roleList);
            }

            LocalDateTime expTime = ticketDomain.getExpTime();
            Long cacheSecond = 0L;
            if (null == expTime) {
                cacheSecond = 7200L;
            } else {
                // 서버가져오기의access_token까지시간 300초
                long endTime = Math.abs((expTime.atZone(ZoneId.systemDefault()).toEpochSecond()) - 300L);
                // 현재시간초데이터
                long startTime =
                        LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
                cacheSecond = endTime - startTime;
            }

            if (StringUtils.isNotBlank(accessToken)) {
                UapTokenUtils.saveAccessToken(session.getId(), accessToken, cacheSecond);
            } else {
                log.warn("TicketDomain 중미완료패키지 accessToken");
            }

            if (StringUtils.isNotBlank(refreshToken)) {
                UapTokenUtils.saveRefreshToken(session.getId(), refreshToken, cacheSecond * 2);
            } else {
                log.warn("TicketDomain 중미완료패키지 refreshToken");
            }

            log.info("없음비밀번호로그인성공, 사용자: {}, 완료완료 Session 및 Token 의저장", uapUser.getLoginName());

            // 가져오기사용자메뉴경로저장입력Session
            storeUserMenuPathsInSession(request);

            return AppResponse.success(uapUser);

        } catch (Exception e) {
            log.error("없음비밀번호로그인실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "로그인실패: " + e.getMessage());
        }
    }

    /**
     * UAP비밀번호로그인(테넌트ID)생성session
     * 사용UAP인증방식의정상방식로그인
     *
     * @param loginName 로그인계정
     * @param password 비밀번호
     * @param tenantId 테넌트ID
     * @param request HTTP요청 
     * @return 사용자 정보
     */
    public UapUser loginUapByPasswordWithTenant(
            String loginName, String password, String tenantId, HttpServletRequest request) {
        try {
            log.info("열기 UAP비밀번호로그인, 계정: {}, 테넌트ID: {}", loginName, tenantId);

            // 1. 생성UAP로그인요청 매개변수
            com.iflytek.sec.uap.client.core.dto.authentication.UapLoginByPasswordDto uapLoginByPasswordDto =
                    new com.iflytek.sec.uap.client.core.dto.authentication.UapLoginByPasswordDto();
            uapLoginByPasswordDto.setAppCode(ClientConfigUtil.instance().getAppCode());
            uapLoginByPasswordDto.setService(ClientConfigUtil.instance().getCasClientContext());
            uapLoginByPasswordDto.setRedirect(ClientConfigUtil.instance().getCasClientContext());
            uapLoginByPasswordDto.setLoginName(loginName);
            uapLoginByPasswordDto.setPassword(password);
            uapLoginByPasswordDto.setTenantId(tenantId);
            uapLoginByPasswordDto.setReferer(ClientConfigUtil.instance().getRestServerUrl());

            // 2. 호출UAP로그인연결가져오기ticket
            ResponseDto<com.iflytek.sec.uap.client.core.dto.authentication.LoginResultDto> loginResponse =
                    ClientAuthenticationAPI.loginUapByPassword(uapLoginByPasswordDto);

            if (loginResponse == null || !loginResponse.isFlag()) {
                String errorMsg = loginResponse != null ? loginResponse.getMessage() : "UAP로그인비어 있습니다";
                log.error("UAP로그인실패: {}", errorMsg);
                throw new ServiceException("UAP로그인실패: " + errorMsg);
            }

            com.iflytek.sec.uap.client.core.dto.authentication.LoginResultDto loginResult = loginResponse.getData();
            if (loginResult == null || StringUtils.isBlank(loginResult.getTicket())) {
                log.error("UAP로그인중미완료패키지있음의ticket");
                throw new ServiceException("UAP로그인실패: 반환되지 않았습니다있음의ticket");
            }

            String ticket = loginResult.getTicket();
            String service = ClientConfigUtil.instance().getCasClientContext();

            // 3. 호출 ClientAuthenticationAPI.validateTicket 인증 ticket
            ResponseDto<TicketDomain> validateResponse = ClientAuthenticationAPI.validateTicket(ticket, service);

            if (validateResponse == null || !validateResponse.isFlag()) {
                String errorMsg = validateResponse != null ? validateResponse.getMessage() : "인증로그인인증실패";
                log.error("ticket 인증 실패: {}", errorMsg);
                throw new ServiceException("ticket 인증 실패: " + errorMsg);
            }

            // 4. 에서 validateTicket 의반환결과중가져오기 UapUser
            TicketDomain ticketDomain = validateResponse.getData();
            if (ticketDomain == null) {
                throw new ServiceException("인증 결과가 비어 있습니다");
            }

            UapUser uapUser = ticketDomain.getUapUser();
            if (uapUser == null) {
                throw new ServiceException("인증 결과에서 사용자 정보를 가져올 수 없습니다");
            }

            // 5. 를사용자 정보저장까지 session
            javax.servlet.http.HttpSession session = request.getSession(true);
            SessionUtil.getInstance().saveUser(session, uapUser);
            SessionUtil.getInstance().saveTenantId(session, tenantId);

            // 6. 에서 TicketDomain 중가져오기 저장 accessToken 및 refreshToken 까지 Redis
            String accessToken = ticketDomain.getAccessToken();
            String refreshToken = ticketDomain.getRefreshToken();

            // 통신경과accessToken가져오기테넌트 정보
            UapTenant uapTenant = ClientAuthenticationAPI.getTenantInfo(tenantId, accessToken);
            SessionUtil.getInstance().saveTenant(session, uapTenant);

            // 7. 가져오기사용자역할목록저장까지 session
            if (StringUtils.isNotBlank(accessToken)) {
                try {
                    List<UapRole> roleList =
                            ClientAuthenticationAPI.getUserRoleListInApp(tenantId, uapUser.getId(), accessToken);
                    if (CollectionUtil.isEmpty(roleList)) {
                        log.warn("사용자 {} 에서테넌트 {} 중있음역할, 시도분매칭회원가입역할", uapUser.getLoginName(), tenantId);
                        roleList = assignRegisterRoleIfNeeded(
                                tenantId, uapUser.getId(), accessToken, uapUser.getLoginName());
                    }

                    // 저장역할목록까지 session
                    if (!CollectionUtil.isEmpty(roleList)) {
                        SessionUtil.getInstance().saveUserRole(session, roleList);
                    }
                } catch (Exception e) {
                    log.warn("사용자 역할 목록 조회 실패, 로그인하지 않음: {}", e.getMessage());
                }
            }

            // 8. 저장 token 까지 Redis
            LocalDateTime expTime = ticketDomain.getExpTime();
            Long cacheSecond = 0L;
            if (null == expTime) {
                cacheSecond = 7200L;
            } else {
                // 서버가져오기의access_token까지시간 300초
                long endTime = Math.abs((expTime.atZone(ZoneId.systemDefault()).toEpochSecond()) - 300L);
                // 현재시간초데이터
                long startTime =
                        LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
                cacheSecond = endTime - startTime;
            }

            if (StringUtils.isNotBlank(accessToken)) {
                UapTokenUtils.saveAccessToken(session.getId(), accessToken, cacheSecond);
            } else {
                log.warn("TicketDomain 중미완료패키지 accessToken");
            }

            if (StringUtils.isNotBlank(refreshToken)) {
                UapTokenUtils.saveRefreshToken(session.getId(), refreshToken, cacheSecond * 2);
            } else {
                log.warn("TicketDomain 중미완료패키지 refreshToken");
            }

            log.info("UAP비밀번호로그인성공, 사용자: {}, 완료완료 Session 및 Token 의저장", uapUser.getLoginName());

            // 가져오기사용자메뉴경로저장입력Session
            storeUserMenuPathsInSession(request);

            return uapUser;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("UAP비밀번호로그인실패", e);
            throw new ServiceException("UAP비밀번호로그인실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<User> getCurrentLoginUser(HttpServletRequest request) {
        try {
            UapUser uapUser = UserUtils.nowLoginUser();
            User user = userMapper.fromUapUser(uapUser);
            return AppResponse.success(user);
        } catch (com.iflytek.rpa.auth.exception.NoLoginException e) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "사용자로그인되지 않았습니다");
        } catch (Exception e) {
            log.error("가져오기현재로그인사용자실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기현재로그인사용자실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<String> getCurrentUserId(HttpServletRequest request) {
        try {
            String userId = UserUtils.nowUserId();
            return AppResponse.success(userId);
        } catch (com.iflytek.rpa.auth.exception.NoLoginException e) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "사용자로그인되지 않았습니다");
        } catch (Exception e) {
            log.error("가져오기현재로그인사용자ID실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기현재로그인사용자ID실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<String> getCurrentLoginUsername(HttpServletRequest request) {
        try {
            String loginName = UserUtils.nowLoginUsername();
            return AppResponse.success(loginName);
        } catch (com.iflytek.rpa.auth.exception.NoLoginException e) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "사용자로그인되지 않았습니다");
        } catch (Exception e) {
            log.error("가져오기현재로그인사용자명실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기현재로그인사용자명실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<String> getLoginNameById(String id, HttpServletRequest request) {
        try {
            String loginName = UserUtils.getLoginNameById(id);
            if (loginName == null) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "찾을 수 없는 사용자 정보");
            }
            return AppResponse.success(loginName);
        } catch (Exception e) {
            log.error("근거사용자ID조회로그인이름실패, userId: {}", id, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회로그인이름실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<String> getRealNameById(String id, HttpServletRequest request) {
        try {
            String realName = UserUtils.getRealNameById(id);
            if (realName == null) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "찾을 수 없는 사용자 정보");
            }
            return AppResponse.success(realName);
        } catch (Exception e) {
            log.error("근거사용자ID조회이름실패, userId: {}", id, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회이름실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<User> getUserInfoById(String id, HttpServletRequest request) {
        try {

            UapUser uapUser = userDao.getUserById(id, databaseName);
            if (uapUser == null) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "찾을 수 없는 사용자 정보");
            }
            User user = userMapper.fromUapUser(uapUser);
            return AppResponse.success(user);
        } catch (Exception e) {
            log.error("근거사용자ID조회사용자 정보실패, userId: {}", id, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회사용자 정보실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<String> getRealNameByPhone(String phone, HttpServletRequest request) {
        try {
            String realName = UserUtils.getRealNameByPhone(phone);
            if (realName == null) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "찾을 수 없는 사용자 정보");
            }
            return AppResponse.success(realName);
        } catch (Exception e) {
            log.error("근거휴대폰 번호조회사용자이름실패, phone: {}", phone, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회사용자이름실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<String> getLoginNameByPhone(String phone, HttpServletRequest request) {
        try {
            String loginName = UserUtils.getLoginNameByPhone(phone);
            if (loginName == null) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "찾을 수 없는 사용자 정보");
            }
            return AppResponse.success(loginName);
        } catch (Exception e) {
            log.error("근거휴대폰 번호조회로그인이름실패, phone: {}", phone, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회로그인이름실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<Boolean> isHistoryUser(String phone) {
        try {
            if (StringUtils.isEmpty(phone)) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "휴대폰 번호는 비워 둘 수 없습니다");
            }
            String extInfo = userDao.queryExtInfoByPhone(phone, databaseName);
            boolean history = "1".equals(extInfo);
            return AppResponse.success(history);
        } catch (Exception e) {
            log.error("조회사용자실패, phone: {}", phone, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회사용자실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<User> getUserInfoByPhone(String phone, HttpServletRequest request) {
        try {
            UapUser uapUser = UserUtils.getUserInfoByPhone(phone);
            if (uapUser == null) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "찾을 수 없는 사용자 정보");
            }
            User user = userMapper.fromUapUser(uapUser);
            return AppResponse.success(user);
        } catch (Exception e) {
            log.error("근거휴대폰 번호조회사용자 정보실패, phone: {}", phone, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회사용자 정보실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<List<User>> queryUserListByIds(List<String> userIdList, HttpServletRequest request) {
        try {
            if (CollectionUtil.isEmpty(userIdList)) {
                return AppResponse.success(Collections.emptyList());
            }
            List<UapUser> uapUsers = UserUtils.queryUserPageList(userIdList);
            List<User> users = userMapper.fromUapUsers(uapUsers);
            return AppResponse.success(users);
        } catch (Exception e) {
            log.error("근거사용자ID목록조회사용자 정보실패, userIds: {}", userIdList, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회사용자 정보실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<List<User>> searchUserByName(String keyword, String deptId, HttpServletRequest request) {
        try {
            List<UapUser> uapUsers = UserUtils.searchUserByName(keyword, deptId);
            if (uapUsers == null) {
                return AppResponse.success(Collections.emptyList());
            }
            List<User> users = userMapper.fromUapUsers(uapUsers);
            return AppResponse.success(users);
        } catch (Exception e) {
            log.error("근거이름조회사람원실패, keyword: {}, deptId: {}", keyword, deptId, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회사용자 정보실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<List<User>> searchUserByPhone(String keyword, String deptId, HttpServletRequest request) {
        try {
            List<UapUser> uapUsers = UserUtils.searchUserByPhone(keyword, deptId);
            if (uapUsers == null) {
                return AppResponse.success(Collections.emptyList());
            }
            List<User> users = userMapper.fromUapUsers(uapUsers);
            return AppResponse.success(users);
        } catch (Exception e) {
            log.error("근거휴대폰 번호조회사람원실패, keyword: {}, deptId: {}", keyword, deptId, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회사용자 정보실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<List<User>> searchUserByNameOrPhone(String keyword, String deptId, HttpServletRequest request) {
        try {
            List<UapUser> uapUsers = UserUtils.searchUserByNameOrPhone(keyword, deptId);
            if (CollectionUtil.isEmpty(uapUsers)) {
                return AppResponse.success(Collections.emptyList());
            }
            List<User> users = userMapper.fromUapUsers(uapUsers);
            return AppResponse.success(users);
        } catch (Exception e) {
            log.error("근거이름또는휴대폰 번호조회사람원실패, keyword: {}, deptId: {}", keyword, deptId, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회사용자 정보실패: " + e.getMessage());
        }
    }

    /**
     * 조회현재로그인의사용자 정보
     * @param request HTTP요청 
     * @return 사용자 정보
     */
    @Override
    public AppResponse<User> getUserInfo(HttpServletRequest request) {

        UapUser uapLoginUser = UapUserInfoAPI.getLoginUser(request);
        User user = userMapper.fromUapUser(uapLoginUser);
        return AppResponse.success(user);
    }

    /**
     * 조회현재기기의전체사용자(모듈추가, 모듈사람드롭다운)
     * @param orgId 모듈ID
     * @param request HTTP요청 
     * @return 사용자목록
     */
    @Override
    public AppResponse<List<User>> queryUserDetailListByOrgId(String orgId, HttpServletRequest request) {
        if (StringUtils.isBlank(orgId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "적음모듈id");
        }
        List<UserExtendDto> userExtendList =
                ClientManagementAPI.queryUserDetailListByOrgId(UapUserInfoAPI.getTenantId(request), orgId);
        List<UapUser> uapUserList = new ArrayList<>();
        for (UserExtendDto user : userExtendList) {
            if (user == null || user.getUser() == null) {
                continue;
            }
            uapUserList.add(user.getUser());
        }
        List<User> userList = userMapper.fromUapUsers(uapUserList);
        return AppResponse.success(userList);
    }

    /**
     * 회원가입후업데이트비밀번호(사용비밀번호로비밀번호)
     *
     * @param loginName 로그인이름
     * @param newPassword 새비밀번호
     */
    public void updatePasswordAfterRegister(String loginName, String newPassword) {
        try {
            UpdatePwdDto updatePwdDto = new UpdatePwdDto();
            updatePwdDto.setLoginName(loginName);
            updatePwdDto.setOldPwd(
                    Base64Utils.encodeToString(UAPConstant.DEFAULT_INITIAL_PASSWORD.getBytes(StandardCharsets.UTF_8)));
            updatePwdDto.setNewPwd(Base64Utils.encodeToString(newPassword.getBytes(StandardCharsets.UTF_8)));

            ResponseDto<String> updatePwdResponse = ClientAuthenticationAPI.updateUserPwd(updatePwdDto);

            if (!updatePwdResponse.isFlag()) {
                throw new ServiceException("업데이트비밀번호실패: " + updatePwdResponse.getMessage());
            }

            log.info("회원가입후업데이트비밀번호성공, 로그인이름: {}", loginName);

        } catch (Exception e) {
            log.error("회원가입후업데이트비밀번호실패, 로그인이름: {}", loginName, e);
            throw new ServiceException("업데이트비밀번호실패: " + e.getMessage());
        }
    }

    /**
     * 출력로그인
     * @param request HTTP요청 
     * @param response HTTP
     * @return 결과
     */
    @Override
    public AppResponse<String> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 지우기단일로그인의session
            try {
                UapUser loginUser = UapUserInfoAPI.getLoginUser(request);
                if (loginUser != null && StringUtils.isNotBlank(loginUser.getId())) {
                    String redisKey = REDIS_KEY_USER_SESSION_PREFIX + loginUser.getId();
                    RedisUtils.del(redisKey);
                    log.debug("완료지우기단일로그인session, 사용자ID: {}", loginUser.getId());
                }
            } catch (Exception e) {
                log.warn("지우기단일로그인session실패", e);
                // 아니요출력예외, 계속실행출력로그인
            }

            UapUserInfoAPI.logout(request, response);
            // cookie
            Cookie cookie = new Cookie("SESSION", "");
            cookie.setMaxAge(0); // 대저장시간로 0
            cookie.setPath("/"); // 경로, 확인및기존경로일
            response.addCookie(cookie); // 추가까지중

            return AppResponse.success("출력로그인성공");
        } catch (Exception e) {
            log.error("출력로그인실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "출력로그인실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<String> changeDept(UserChangeDeptDto userChangeDeptDto, HttpServletRequest request) {
        /*
        변수변경모듈또는역할가능통신경과updateUser연결, 닫기연결있음있음업데이트모듈또는역할의공가능
         */
        List<com.iflytek.rpa.auth.core.entity.UpdateUserDto> userList = userChangeDeptDto.getUserList();
        String deptId = userChangeDeptDto.getDeptId();
        if (CollectionUtil.isEmpty(userList) || StringUtils.isBlank(deptId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        ManagementClient managementClient = UapManagementClientUtil.getManagementClient(request);
        for (com.iflytek.rpa.auth.core.entity.UpdateUserDto userInfo : userList) {
            if (null == userInfo || StringUtils.isBlank(userInfo.getId())) {
                continue;
            }
            // 근거id조회사용자 정보
            UapUser user = UserUtils.getUserInfoById(userInfo.getId());
            // 사용자 정보
            userInfo.setName(user.getName());
            // 유형로사용자
            userInfo.setUserType(3);
            userInfo.setLoginName(user.getLoginName());
            userInfo.setPhone(user.getPhone());
            userInfo.setEmail(user.getEmail());

            // 업데이트상태정보
            com.iflytek.rpa.auth.core.entity.UpdateUapUserDto updateUapUserDto =
                    new com.iflytek.rpa.auth.core.entity.UpdateUapUserDto();
            userInfo.setOrgId(deptId);
            updateUapUserDto.setUser(userInfo);
            // 변환로UAP의UpdateUapUserDto
            UpdateUapUserDto uapUpdateUapUserDto = updateUapUserDtoMapper.toUapUpdateUapUserDto(updateUapUserDto);
            ResponseDto<String> updateUserResponse = managementClient.updateUser(uapUpdateUapUserDto);
            if (!updateUserResponse.isFlag()) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, updateUserResponse.getMessage());
            }
        }
        return AppResponse.success("완료");
    }

    /**
     * 중-봇-모든아래선택-조회연결
     * 근거입력의닫기 문자(이름또는휴대폰 번호)조회사용자
     * @param keyword 닫기 문자(이름또는휴대폰 번호)
     * @param deptId 모듈ID
     * @return 사용자검색결과목록
     */
    @Override
    public AppResponse<List<UserSearchDto>> getUserByNameOrPhone(
            String keyword, String deptId, HttpServletRequest request) {
        List<UapUser> userList = UserUtils.searchUserByNameOrPhone(keyword, deptId);
        if (CollectionUtil.isEmpty(userList)) {
            return AppResponse.success(new ArrayList<>());
        }
        List<UserSearchDto> result = new ArrayList<>();
        // 사용 Set 필터링재복사사용자(으로사용자ID로일식별자)
        Set<String> userIdSet = new HashSet<>();
        for (UapUser user : userList) {
            if (user == null || userIdSet.contains(user.getId())) {
                continue;
            }
            userIdSet.add(user.getId());
            UserSearchDto userSearchDto = new UserSearchDto();
            BeanUtils.copyProperties(user, userSearchDto);
            result.add(userSearchDto);
        }
        return AppResponse.success(result);
    }

    /**
     * 가져오기사용자정보(패키지정보대기)
     * @param tenantId 테넌트ID
     * @param getUserDto 조회매개변수
     * @param request HTTP요청 
     * @return 사용자정보
     */
    @Override
    public AppResponse<com.iflytek.rpa.auth.core.entity.UserExtendDto> queryUserExtendInfo(
            String tenantId, com.iflytek.rpa.auth.core.entity.GetUserDto getUserDto, HttpServletRequest request) {
        // getUserDto 변환로 UAP 의 GetUserDto
        GetUserDto uapGetUserDto = getUserDtoMapper.toUapGetUserDto(getUserDto);
        // 호출 UAP 연결가져오기사용자정보
        UserExtendDto uapUserExtendInfo = ClientManagementAPI.getUserExtendInfo(tenantId, uapGetUserDto);
        // 변환로 core 의 UserExtendDto
        com.iflytek.rpa.auth.core.entity.UserExtendDto coreUserExtendDto =
                userExtendDtoMapper.fromUapUserExtendDto(uapUserExtendInfo);
        return AppResponse.success(coreUserExtendDto);
    }

    /**
     * 가져오기현재로그인사용자의권한
     * 근거session조회테넌트code, 결과가예테넌트, 이면조회데이터베이스중권한
     * 결과가있음데이터, 있음모든권한
     *
     * @param request HTTP요청 
     * @return 사용자권한정보
     */
    @Override
    public AppResponse<UserEntitlementDto> getCurrentUserEntitlement(HttpServletRequest request) {
        try {
            // 1. 가져오기현재로그인사용자
            UapUser loginUser = UapUserInfoAPI.getLoginUser(request);
            if (loginUser == null || StringUtils.isBlank(loginUser.getId())) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기현재로그인사용자실패");
            }
            String userId = loginUser.getId();

            // 2. 가져오기테넌트 정보
            UapTenant uapTenant = UapUserInfoAPI.getTenant(request);
            if (uapTenant == null) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기테넌트 정보실패");
            }

            String tenantCode = uapTenant.getTenantCode();
            String tenantId = uapTenant.getId();

            if (StringUtils.isBlank(tenantCode)) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "테넌트 코드가 비어 있습니다");
            }

            // 3. 여부로테넌트
            boolean isEnterpriseTenant = isEnterpriseTenant(tenantCode);

            // 4. 결과가아니요예테넌트, 반환모든권한
            if (!isEnterpriseTenant) {
                UserEntitlementDto defaultDto = createDefaultEntitlement();
                log.info("테넌트, 반환권한, userId: {}, tenantId: {}", userId, tenantId);
                return AppResponse.success(defaultDto);
            }

            // 5. 결과가예테넌트, 조회데이터베이스중의권한
            UserEntitlement entitlement = userEntitlementDao.queryByUserIdAndTenantId(userId, tenantId);

            // 6. 결과가있음데이터, 반환모든권한
            if (entitlement == null) {
                UserEntitlementDto defaultDto = createDefaultEntitlement();
                log.info("테넌트일치하지 않는권한, 반환권한, userId: {}, tenantId: {}", userId, tenantId);
                return AppResponse.success(defaultDto);
            }

            // 7. 결과가있음데이터, 변환로DTO반환
            UserEntitlementDto dto = convertToDto(entitlement);
            log.info(
                    "조회사용자권한완료, userId: {}, tenantId: {}, designer: {}, executor: {}, console: {}, market: {}",
                    userId,
                    tenantId,
                    dto.getModuleDesigner(),
                    dto.getModuleExecutor(),
                    dto.getModuleConsole(),
                    dto.getModuleMarket());
            return AppResponse.success(dto);

        } catch (Exception e) {
            log.error("가져오기현재사용자권한실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기현재사용자권한실패: " + e.getMessage());
        }
    }

    /**
     * 여부로테넌트
     *
     * @param tenantCode 테넌트코드
     * @return true-테넌트, false-테넌트
     */
    private boolean isEnterpriseTenant(String tenantCode) {
        if (StringUtils.isBlank(tenantCode)) {
            return false;
        }
        return tenantCode.startsWith(UAPConstant.ENTERPRISE_PURCHASED_TENANT_CODE)
                || tenantCode.startsWith(UAPConstant.ENTERPRISE_SUBSCRIPTION_TENANT_CODE);
    }

    /**
     * 생성권한(모든모듈있음권한)
     *
     * @return 권한DTO
     */
    private UserEntitlementDto createDefaultEntitlement() {
        UserEntitlementDto dto = new UserEntitlementDto();
        dto.setModuleDesigner(true);
        dto.setModuleExecutor(true);
        dto.setModuleConsole(true);
        dto.setModuleMarket(true);
        return dto;
    }

    /**
     * 를유형변환로DTO
     *
     * @param entitlement 사용자권한
     * @return 사용자권한DTO
     */
    private UserEntitlementDto convertToDto(UserEntitlement entitlement) {
        UserEntitlementDto dto = new UserEntitlementDto();
        dto.setModuleDesigner(entitlement.getModuleDesigner() != null && entitlement.getModuleDesigner() == 1);
        dto.setModuleExecutor(entitlement.getModuleExecutor() != null && entitlement.getModuleExecutor() == 1);
        dto.setModuleConsole(entitlement.getModuleConsole() != null && entitlement.getModuleConsole() == 1);
        dto.setModuleMarket(entitlement.getModuleMarket() != null && entitlement.getModuleMarket() == 1);
        return dto;
    }

    @Override
    public AppResponse<String> getNameById(String id, HttpServletRequest request) {
        try {
            String name = userDao.getNameById(id, databaseName);
            if (StringUtils.isEmpty(name)) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "찾을 수 없는 사용자 정보");
            }
            return AppResponse.success(name);
        } catch (Exception e) {
            log.error("근거사용자ID조회사용자이름실패, userId: {}", id, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회사용자이름실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<com.iflytek.rpa.auth.core.entity.PageDto<RobotExecute>> getDeployedUserList(
            GetDeployedUserListDto dto, HttpServletRequest request) {
        try {
            if (dto.getPageNo() == null || dto.getPageNo() < 1) {
                dto.setPageNo(1);
            }
            if (dto.getPageSize() == null || dto.getPageSize() < 1) {
                dto.setPageSize(10);
            }

            Page<RobotExecute> page = new Page<>(dto.getPageNo(), dto.getPageSize());

            IPage<RobotExecute> result = userDao.getDeployedUserList(page, dto, databaseName);

            com.iflytek.rpa.auth.core.entity.PageDto<RobotExecute> pageDto =
                    new com.iflytek.rpa.auth.core.entity.PageDto<>();
            pageDto.setResult(result.getRecords());
            pageDto.setTotalCount(result.getTotal());
            pageDto.setCurrentPageNo((int) result.getCurrent());
            pageDto.setPageSize((int) result.getSize());

            return AppResponse.success(pageDto);
        } catch (Exception e) {
            log.error("가져오기완료모듈사용자목록실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기완료모듈사용자목록실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<com.iflytek.rpa.auth.core.entity.PageDto<RobotExecute>> getDeployedUserListWithoutTenantId(
            GetDeployedUserListDto dto, HttpServletRequest request) {
        try {
            if (dto.getPageNo() == null || dto.getPageNo() < 1) {
                dto.setPageNo(1);
            }
            if (dto.getPageSize() == null || dto.getPageSize() < 1) {
                dto.setPageSize(10);
            }
            Page<RobotExecute> page = new Page<>(dto.getPageNo(), dto.getPageSize());

            IPage<RobotExecute> result = userDao.getDeployedUserListWithoutTenantId(page, dto, databaseName);

            com.iflytek.rpa.auth.core.entity.PageDto<RobotExecute> pageDto =
                    new com.iflytek.rpa.auth.core.entity.PageDto<>();
            pageDto.setResult(result.getRecords());
            pageDto.setTotalCount(result.getTotal());
            pageDto.setCurrentPageNo((int) result.getCurrent());
            pageDto.setPageSize((int) result.getSize());
            return AppResponse.success(pageDto);
        } catch (Exception e) {
            log.error("가져오기완료모듈사용자목록실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기완료모듈사용자목록실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<List<MarketDto>> getUserUnDeployed(GetUserUnDeployedDto dto, HttpServletRequest request) {
        try {
            List<MarketDto> result = userDao.getUserUnDeployed(dto, databaseName);
            return AppResponse.success(result);
        } catch (Exception e) {
            log.error("모듈 사용자 목록 조회 실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "모듈 사용자 목록 조회 실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<com.iflytek.rpa.auth.core.entity.PageDto<MarketDto>> getMarketUserList(
            GetMarketUserListDto dto, HttpServletRequest request) {
        try {
            if (dto.getPageNo() == null || dto.getPageNo() < 1) {
                dto.setPageNo(1);
            }
            if (dto.getPageSize() == null || dto.getPageSize() < 1) {
                dto.setPageSize(10);
            }

            Page<MarketDto> page = new Page<>(dto.getPageNo(), dto.getPageSize(), true);

            IPage<MarketDto> result = userDao.getMarketUserList(page, dto, databaseName);

            com.iflytek.rpa.auth.core.entity.PageDto<MarketDto> pageDto =
                    new com.iflytek.rpa.auth.core.entity.PageDto<>();
            pageDto.setResult(result.getRecords());
            pageDto.setTotalCount(result.getTotal());
            pageDto.setCurrentPageNo((int) result.getCurrent());
            pageDto.setPageSize((int) result.getSize());

            return AppResponse.success(pageDto);
        } catch (Exception e) {
            log.error("가져오기마켓사용자목록실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기마켓사용자목록실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<com.iflytek.rpa.auth.core.entity.PageDto<MarketDto>> getMarketUserListByPublic(
            GetMarketUserListByPublicDto dto, HttpServletRequest request) {
        try {
            if (dto.getPageNo() == null || dto.getPageNo() < 1) {
                dto.setPageNo(1);
            }
            if (dto.getPageSize() == null || dto.getPageSize() < 1) {
                dto.setPageSize(10);
            }

            Page<MarketDto> page = new Page<>(dto.getPageNo(), dto.getPageSize(), true);

            IPage<MarketDto> result = userDao.getMarketUserListByPublic(page, dto, databaseName);

            com.iflytek.rpa.auth.core.entity.PageDto<MarketDto> pageDto =
                    new com.iflytek.rpa.auth.core.entity.PageDto<>();
            pageDto.setResult(result.getRecords());
            pageDto.setTotalCount(result.getTotal());
            pageDto.setCurrentPageNo((int) result.getCurrent());
            pageDto.setPageSize((int) result.getSize());

            return AppResponse.success(pageDto);
        } catch (Exception e) {
            log.error("가져오기 공유마켓사용자목록실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기 공유마켓사용자목록실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<List<MarketDto>> getMarketUserByPhone(GetMarketUserByPhoneDto dto, HttpServletRequest request) {
        try {
            List<MarketDto> result = userDao.getMarketUserByPhone(dto, databaseName);
            return AppResponse.success(result);
        } catch (Exception e) {
            log.error("근거휴대폰 번호조회마켓사용자실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "근거휴대폰 번호조회마켓사용자실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<List<MarketDto>> getMarketUserByPhoneForOwner(
            GetMarketUserByPhoneForOwnerDto dto, HttpServletRequest request) {
        try {
            List<MarketDto> result = userDao.getMarketUserByPhoneForOwner(dto, databaseName);
            return AppResponse.success(result);
        } catch (Exception e) {
            log.error("근거휴대폰 번호조회마켓중의사용자실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "근거휴대폰 번호조회마켓중의사용자실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<List<TenantUser>> getMarketTenantUserList(
            GetMarketTenantUserListDto dto, HttpServletRequest request) {
        try {
            List<TenantUser> result = userDao.getMarketTenantUserList(dto, databaseName);
            return AppResponse.success(result);
        } catch (Exception e) {
            log.error("근거사용자ID목록조회테넌트사용자목록실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "근거사용자ID목록조회테넌트사용자목록실패: " + e.getMessage());
        }
    }

    /**
     * 가져오기현재사용자권한목록(casdoor의공가능연결)
     * @param request HTTP요청 
     * @return 사용자목록
     */
    @Override
    public AppResponse<List<Permission>> getCurrentUserPermissionList(HttpServletRequest request) throws IOException {
        throw new UnsupportedOperationException("지원하지 않는 기능입니다.");
    }

    /**
     * (casdoor의공가능연결)
     * @param request HTTP요청 
     * @return
     */
    @Override
    public AppResponse<String> getRedirectUrl(HttpServletRequest request) {
        throw new UnsupportedOperationException("지원하지 않는 기능입니다.");
    }

    /**
     * (casdoor의공가능연결)
     * @param code OAuth권한 부여코드
     * @param state OAuth state매개변수
     * @param request HTTP요청 
     * @return
     * @throws IOException
     */
    @Override
    public AppResponse<User> signIn(String code, String state, HttpServletRequest request) throws IOException {
        throw new UnsupportedOperationException("지원하지 않는 기능입니다.");
    }

    /**
     * (casdoor의공가능연결)
     * @param request HTTP요청 
     * @return
     */
    @Override
    public AppResponse<User> checkLoginStatus(HttpServletRequest request) {
        throw new UnsupportedOperationException("지원하지 않는 기능입니다.");
    }

    /**
     * (casdoor의공가능연결)
     * @param request HTTP요청 
     * @return
     */
    @Override
    public AppResponse<String> refreshToken(HttpServletRequest request) {
        throw new UnsupportedOperationException("지원하지 않는 기능입니다.");
    }

    /**
     * 가져오기사용자메뉴경로저장입력Session
     * @param request HTTP요청 
     */
    private void storeUserMenuPathsInSession(HttpServletRequest request) {
        try {
            // 가져오기사용자메뉴
            AppResponse<List<TreeNode>> menuTreeResponse = authService.getUserAuthTreeInApp(request);
            if (!menuTreeResponse.ok() || menuTreeResponse.getData() == null) {
                log.warn("가져오기사용자메뉴실패, 불가저장메뉴경로까지Session");
                return;
            }

            // 가져오기메뉴경로목록
            List<TreeNode> menuTreeList = menuTreeResponse.getData();
            Set<String> menuPaths = extractMenuPaths(menuTreeList);

            // 저장입력Session
            javax.servlet.http.HttpSession session = request.getSession(false);
            if (session != null) {
                session.setAttribute("userMenuPaths", menuPaths);
                log.debug("사용자메뉴경로완료저장입력Session, 공유{}경로", menuPaths.size());
            } else {
                log.warn("Session찾을 수 없습니다, 불가저장메뉴경로");
            }
        } catch (Exception e) {
            log.error("가져오기 저장사용자메뉴경로까지Session실패", e);
            // 아니요출력예외, 로그인프로세스
        }
    }

    /**
     * 에서메뉴중가져오기모든메뉴경로
     * @param treeNodeList 메뉴목록
     * @return 메뉴경로 합치기(재)
     */
    private Set<String> extractMenuPaths(List<TreeNode> treeNodeList) {
        Set<String> menuPaths = new HashSet<>();
        if (treeNodeList == null || treeNodeList.isEmpty()) {
            return menuPaths;
        }

        for (TreeNode rootNode : treeNodeList) {
            // 에서열기 , 경로전비어 있습니다문자열
            extractPathsFromNode(rootNode, "", menuPaths);
        }

        return menuPaths;
    }

    /**
     * 가져오기 의경로
     * @param node 현재
     * @param parentPath 경로전(예 "/schedule")
     * @param menuPaths 경로 합치기
     */
    private void extractPathsFromNode(TreeNode node, String parentPath, Set<String> menuPaths) {
        if (node == null) {
            return;
        }

        String nodeValue = node.getValue();
        String currentPath = parentPath;

        // 결과가현재있음value, 이면연결까지경로중
        if (StringUtils.isNotBlank(nodeValue)) {
            // 생성경로: 경로 + "/" + 현재value
            if (StringUtils.isBlank(parentPath)) {
                // 결과가경로비어 있습니다, 직선연결사용현재value, 추가전가져오기 
                currentPath = "/" + nodeValue.trim();
            } else {
                // 결과가경로아니요비어 있습니다, 연결경로및현재value
                currentPath = parentPath + "/" + nodeValue.trim();
            }
            // 경로
            currentPath = normalizeMenuPath(currentPath);
        }

        // 관리, 를현재경로 로의경로
        List<TreeNode> children = node.getNodes();
        if (children != null && !children.isEmpty()) {
            // 있음, 계속, 아니요추가까지경로 합치기
            for (TreeNode child : children) {
                // 결과가현재있음value, 사용currentPath로의경로
                // 결과가현재있음value, 계속사용parentPath
                String childParentPath = StringUtils.isNotBlank(nodeValue) ? currentPath : parentPath;
                extractPathsFromNode(child, childParentPath, menuPaths);
            }
        } else {
            // 있음, 설명예, 를경로추가까지합치기중
            if (StringUtils.isNotBlank(currentPath)) {
                menuPaths.add(currentPath);
            }
        }
    }

    /**
     * 메뉴경로
     * 제거모듈, 확인경로형식시스템일(예 /schedule/task)
     * @param path 기존경로
     * @return 후의경로
     */
    private String normalizeMenuPath(String path) {
        if (StringUtils.isBlank(path)) {
            return path;
        }
        path = path.trim();
        // 제거모듈(보관경로 "/")
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
