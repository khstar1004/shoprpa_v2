package com.iflytek.rpa.auth.sp.uap.service.impl;

import static com.iflytek.rpa.auth.sp.uap.constants.RedisKeyConstant.REDIS_KEY_TENANT_EXPIRATION_PREFIX;
import static com.iflytek.rpa.auth.sp.uap.constants.RedisKeyConstant.REDIS_KEY_TENANT_HAS_SPACE_PREFIX;
import static com.iflytek.rpa.auth.sp.uap.constants.RedisKeyConstant.REDIS_KEY_TENANT_USER_PREFIX;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.rpa.auth.conf.condition.ConditionalOnSaaSOrUAP;
import com.iflytek.rpa.auth.core.entity.Org;
import com.iflytek.rpa.auth.core.entity.Tenant;
import com.iflytek.rpa.auth.core.entity.TenantExpirationDto;
import com.iflytek.rpa.auth.core.entity.TenantInfoDto;
import com.iflytek.rpa.auth.core.entity.UserVo;
import com.iflytek.rpa.auth.core.service.TenantService;
import com.iflytek.rpa.auth.exception.ServiceException;
import com.iflytek.rpa.auth.sp.uap.constants.UAPConstant;
import com.iflytek.rpa.auth.sp.uap.dao.TenantDao;
import com.iflytek.rpa.auth.sp.uap.dao.TenantExpirationDao;
import com.iflytek.rpa.auth.sp.uap.dao.UserDao;
import com.iflytek.rpa.auth.sp.uap.entity.TenantExpiration;
import com.iflytek.rpa.auth.sp.uap.mapper.OrgMapper;
import com.iflytek.rpa.auth.sp.uap.mapper.TenantMapper;
import com.iflytek.rpa.auth.sp.uap.utils.EncryptUtils;
import com.iflytek.rpa.auth.sp.uap.utils.TenantUtils;
import com.iflytek.rpa.auth.sp.uap.utils.UapManagementClientUtil;
import com.iflytek.rpa.auth.utils.AppResponse;
import com.iflytek.rpa.auth.utils.ErrorCodeEnum;
import com.iflytek.rpa.auth.utils.RedisUtils;
import com.iflytek.sec.uap.client.api.ClientAuthenticationAPI;
import com.iflytek.sec.uap.client.api.ClientManagementAPI;
import com.iflytek.sec.uap.client.api.UapUserInfoAPI;
import com.iflytek.sec.uap.client.core.client.ManagementClient;
import com.iflytek.sec.uap.client.core.dto.ResponseDto;
import com.iflytek.sec.uap.client.core.dto.app.UapApp;
import com.iflytek.sec.uap.client.core.dto.org.UapOrg;
import com.iflytek.sec.uap.client.core.dto.tenant.*;
import com.iflytek.sec.uap.client.core.dto.user.GetUserDto;
import com.iflytek.sec.uap.client.core.dto.user.UapUser;
import com.iflytek.sec.uap.client.core.dto.user.UserExtendDto;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service("tenantService")
@ConditionalOnSaaSOrUAP
public class TenantServiceImpl implements TenantService {
    @Value("${uap.database.name:uap_db}")
    private String databaseName;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private TenantMapper tenantMapper;

    @Autowired
    private OrgMapper orgMapper;

    @Autowired
    private TenantExpirationDao tenantExpirationDao;

    @Value("${tenant.expiration.alert.days:10}")
    private Integer alertDays;

    /**
     * 테넌트빈정보저장경과시간(초)
     * 패키지: 사용자여부에서테넌트빈중, 테넌트까지정보대기
     * 2시간(7200초)
     */
    private static final int TENANT_SPACE_CACHE_EXPIRE_SECONDS = 7200;

    /**
     * 테넌트빈정보빈값저장경과시간(초)
     * 사용저장"있음데이터"의, 조회데이터베이스
     * 2시간(7200초)
     */
    private static final int TENANT_SPACE_EMPTY_CACHE_EXPIRE_SECONDS = 7200;

    private static final char TENANT_NAME_SEPARATOR = '#';

    @Override
    public AppResponse<List<UserVo>> getAllUser(String userName) throws Exception {
        String tenantId = TenantUtils.getTenantId();
        String redisKey = REDIS_KEY_TENANT_USER_PREFIX + tenantId + ":" + userName;
        Object cache = RedisUtils.get(redisKey);
        if (cache != null && StringUtils.isNotBlank(cache.toString())) {
            ObjectMapper objectMapper = new ObjectMapper();
            List<UserVo> cachedList = objectMapper.readValue(
                    cache.toString(), objectMapper.getTypeFactory().constructCollectionType(List.class, UserVo.class));
            return AppResponse.success(cachedList);
        }
        List<UserVo> allList = tenantDao.getUserByTenantId(databaseName, tenantId, userName);
        // 통신경과set행필터링
        Set<UserVo> userVoSet = new HashSet<>(allList);
        List<UserVo> result = new ArrayList<>(userVoSet);
        RedisUtils.set(redisKey, new ObjectMapper().writeValueAsString(result), 3600);
        return AppResponse.success(allList);
    }

    @Override
    public AppResponse<List<Tenant>> getTenantListInApp(HttpServletRequest request) {
        List<UapTenant> uapTenantList = UapUserInfoAPI.getTenantListInApp(request);
        List<Tenant> tenantList = uapTenantList.stream()
                .map(tenantMapper::fromUapTenant)
                .filter(Objects::nonNull)
                .map(this::stripTenantNameSalt)
                .collect(Collectors.toList());
        return AppResponse.success(tenantList);
    }

    @Override
    public AppResponse<TenantInfoDto> getTenantInfo(HttpServletRequest request) throws Exception {
        String tenantId = UapUserInfoAPI.getTenantId(request);
        // 조회테넌트 정보
        TenantDetailDto tenantDetailDto = getTenantDetail(tenantId, request);
        TenantInfoDto tenantInfoDto = new TenantInfoDto();
        tenantInfoDto.setId(tenantDetailDto.getId());
        tenantInfoDto.setName(tenantDetailDto.getName());
        tenantInfoDto.setCode(tenantDetailDto.getTenantCode());
        // User유형아니요예public유형의,  사용매칭기기
        List<?> adminList = tenantDetailDto.getAdminList();
        if (adminList != null && !adminList.isEmpty()) {
            UserAdapter userAdapter = new UserAdapter(adminList.get(0));
            String managerId = userAdapter.getId();
            String managerName = userAdapter.getName();
            tenantInfoDto.setManagerId(managerId);
            // 근거사용자id조회
            GetUserDto getUserDto = new GetUserDto();
            getUserDto.setUserId(managerId);
            UserExtendDto userExtendDto = ClientManagementAPI.getUserExtendInfo(tenantId, getUserDto);
            UapUser user = userExtendDto.getUser();
            if (user == null || StringUtils.isBlank(user.getPhone())) {
                tenantInfoDto.setManagerName(managerName + "(" + ")");
            } else {
                tenantInfoDto.setManagerName(managerName + "(" + user.getPhone() + ")");
            }
        }
        return AppResponse.success(tenantInfoDto);
    }

    @Override
    public AppResponse<String> getTenantId(HttpServletRequest request) {
        String tenantId = UapUserInfoAPI.getTenantId(request);
        return AppResponse.success(tenantId);
    }

    @Override
    public AppResponse<String> getCurrentTenantId(HttpServletRequest request) {
        try {
            String tenantId = TenantUtils.getTenantId();
            if (StringUtils.isBlank(tenantId)) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기테넌트ID실패");
            }
            return AppResponse.success(tenantId);
        } catch (Exception e) {
            log.error("가져오기현재로그인의테넌트ID실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기테넌트ID실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<String> getCurrentTenantName(HttpServletRequest request) {
        try {
            String tenantName = TenantUtils.getTenantName();
            if (StringUtils.isBlank(tenantName)) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기테넌트이름실패");
            }
            return AppResponse.success(tenantName);
        } catch (Exception e) {
            log.error("가져오기현재로그인의테넌트이름실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기테넌트이름실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<Tenant> queryTenantInfoById(String tenantId, HttpServletRequest request) {
        try {
            if (StringUtils.isBlank(tenantId)) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "테넌트 ID는 비워 둘 수 없습니다");
            }
            UapTenant uapTenant = TenantUtils.queryTenantInfoById(tenantId);
            if (uapTenant == null) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "찾을 수 없는 테넌트 정보");
            }
            Tenant tenant = tenantMapper.fromUapTenant(uapTenant);
            return AppResponse.success(tenant);
        } catch (Exception e) {
            log.error("근거테넌트ID조회테넌트 정보실패, tenantId: {}", tenantId, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회테넌트 정보실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<String> changeManager(String id, HttpServletRequest request) {
        String tenantId = UapUserInfoAPI.getTenantId(request);
        UpdateTenantDto updateTenantDto = new UpdateTenantDto();
        updateTenantDto.setId(tenantId);
        List<TenantUserDto> adminList = new ArrayList<>();
        TenantUserDto tenantUserDto = new TenantUserDto();
        tenantUserDto.setId(id);
        adminList.add(tenantUserDto);
        updateTenantDto.setAdminList(adminList);
        // 조회사용정보
        UapApp appInfo = UapUserInfoAPI.getUapApp(request);
        List<TenantAppDto> appList = new ArrayList<>();
        TenantAppDto tenantAppDto = new TenantAppDto();
        tenantAppDto.setId(appInfo.getId());
        appList.add(tenantAppDto);
        updateTenantDto.setAppList(appList);
        // 조회테넌트 정보
        TenantDetailDto tenantDetailDto = getTenantDetail(tenantId, request);
        updateTenantDto.setName(tenantDetailDto.getName());
        ManagementClient managementClient = UapManagementClientUtil.getManagementClient(request);
        // 업데이트관리관리원
        ResponseDto<String> updateResponse = managementClient.updateTenant(updateTenantDto);
        if (!updateResponse.isFlag()) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, updateResponse.getMessage());
        }
        return AppResponse.success("수정성공");
    }

    @Override
    public AppResponse<List<Org>> getAllOrgList(String tenantId, HttpServletRequest request) {
        List<UapOrg> uapOrgList = ClientManagementAPI.queryAllOrgList(tenantId);
        List<Org> orgList = orgMapper.fromUapOrgs(uapOrgList);
        return AppResponse.success(orgList);
    }

    /**
     * 내부모듈방법법: 근거테넌트ID조회테넌트
     */
    private TenantDetailDto getTenantDetail(String tenantId, HttpServletRequest request) {
        GetTenantDto getTenantDto = new GetTenantDto();
        getTenantDto.setId(tenantId);
        ManagementClient managementClient = UapManagementClientUtil.getManagementClient(request);
        ResponseDto<TenantDetailDto> tenantDetailResponse = managementClient.queryTenantDetailInfo(getTenantDto);
        if (!tenantDetailResponse.isFlag()) {
            throw new ServiceException(tenantDetailResponse.getMessage());
        }
        TenantDetailDto tenantDetailDto = tenantDetailResponse.getData();
        if (tenantDetailDto == null) {
            throw new ServiceException("데이터예외, 테넌트 정보가 없습니다");
        }
        return tenantDetailDto;
    }

    @Override
    public AppResponse<String> switchTenant(String tenantId, HttpServletRequest request) {
        String userId = "";
        try {
            if (StringUtils.isBlank(tenantId)) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "테넌트 ID는 비워 둘 수 없습니다");
            }
            UapUser loginUser = UapUserInfoAPI.getLoginUser(request);
            if (loginUser == null || StringUtils.isEmpty(loginUser.getId())) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기현재로그인사용자실패");
            }
            userId = loginUser.getId();

            // 인증사용자여부있음목록 테넌트권한
            validateTenantPermission(loginUser, tenantId);

            // 실행테넌트
            UapUserInfoAPI.changeTenant(tenantId, userId, request);
            return AppResponse.success("테넌트성공");
        } catch (ServiceException e) {
            log.error("테넌트실패, tenantId: {}, userId: {}", tenantId, userId, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, e.getMessage());
        } catch (Exception e) {
            log.error("테넌트실패, tenantId: {}, userId: {}", tenantId, userId, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "테넌트실패: " + e.getMessage());
        }
    }

    /**
     * 인증사용자여부있음목록 테넌트권한
     * 근거로그인사용자 정보조회테넌트목록, 인증목록 테넌트ID여부에서목록중
     *
     * @param loginUser      현재로그인사용자
     * @param targetTenantId 목록 테넌트ID
     * @throws ServiceException 결과가인증 실패이면출력예외
     */
    private void validateTenantPermission(UapUser loginUser, String targetTenantId) {
        if (loginUser == null) {
            throw new ServiceException("사용자 정보비워 둘 수 없습니다");
        }
        if (StringUtils.isBlank(targetTenantId)) {
            throw new ServiceException("목록 테넌트 ID는 비워 둘 수 없습니다");
        }

        // 가져오기사용자로그인이름
        String loginName = loginUser.getLoginName();
        if (StringUtils.isBlank(loginName)) {
            // 결과가로그인이름비어 있습니다, 시도근거휴대폰 번호조회
            String phone = loginUser.getPhone();
            if (StringUtils.isNotBlank(phone)) {
                loginName = userDao.queryLoginNameByPhone(phone, databaseName);
            }
            if (StringUtils.isBlank(loginName)) {
                log.warn("불가가져오기사용자로그인이름, 사용자ID: {}", loginUser.getId());
                loginName = phone != null ? phone : loginUser.getId();
            }
        }

        // 근거로그인계정조회테넌트목록
        List<UapTenant> tenantList = ClientAuthenticationAPI.getTenantListInAppByLoginName(loginName);
        if (CollectionUtils.isEmpty(tenantList)) {
            log.warn("근거로그인이름찾을 수 없는 테넌트 정보, 로그인이름: {}", loginName);
            throw new ServiceException("현재사용자있음가능사용의테넌트빈");
        }

        // 인증목록 테넌트ID여부에서목록중
        boolean hasPermission = tenantList.stream().anyMatch(tenant -> targetTenantId.equals(tenant.getId()));

        if (!hasPermission) {
            log.warn("사용자있음목록 테넌트권한, 로그인이름: {}, 목록 테넌트ID: {}", loginName, targetTenantId);
            throw new ServiceException("있음해당테넌트빈의방문권한");
        }
    }

    @Override
    public AppResponse<List<Tenant>> getTenantList(String phone, HttpServletRequest request) {
        try {
            List<UapTenant> uapTenantList;
            if (StringUtils.isEmpty(phone)) {
                // 로그인후, 있음시token
                //                uapTenantList = UapUserInfoAPI.getTenantListInApp(request);
                UapUser uapUser = UapUserInfoAPI.getLoginUser(request);
                phone = uapUser.getPhone();
            }
            // 근거휴대폰 번호조회사용자loginName
            String loginName = userDao.queryLoginNameByPhone(phone, databaseName);
            if (StringUtils.isBlank(loginName)) {
                log.warn("근거휴대폰 번호찾을 수 없는 사용자로그인이름, 휴대폰 번호: {}", phone);
                loginName = phone;
            }

            // 근거loginName조회테넌트목록
            uapTenantList = ClientAuthenticationAPI.getTenantListInAppByLoginName(loginName);
            //            uapTenantList = tenantDao.queryTenantListByPhone(databaseName, phone);
            // uapTenantList  를 tenantCode 으로UAPConstant.PERSONAL_TENANT_CODE 열기 의 정렬에서후
            uapTenantList.sort((t1, t2) -> {
                boolean isT1Personal = StringUtils.isNotBlank(t1.getTenantCode())
                        && t1.getTenantCode().startsWith(UAPConstant.PERSONAL_TENANT_CODE);
                boolean isT2Personal = StringUtils.isNotBlank(t2.getTenantCode())
                        && t2.getTenantCode().startsWith(UAPConstant.PERSONAL_TENANT_CODE);
                if (isT1Personal && !isT2Personal) {
                    return 1;
                } else if (!isT1Personal && isT2Personal) {
                    return -1;
                } else {
                    return 0;
                }
            });

            if (uapTenantList == null || uapTenantList.isEmpty()) {
                log.warn("사용자있음테넌트 정보, 휴대폰 번호: {}", phone);
                return AppResponse.success(Collections.emptyList());
            }

            // 3. 변환로서비스
            List<Tenant> tenantList = uapTenantList.stream()
                    .map(tenantMapper::fromUapTenant)
                    .filter(Objects::nonNull)
                    .map(this::stripTenantNameSalt)
                    .collect(Collectors.toList());

            // 4. 로매개테넌트까지정보
            for (Tenant tenant : tenantList) {
                fillTenantExpirationInfo(tenant);
            }

            log.info("가져오기테넌트목록성공, 휴대폰 번호: {}, 테넌트수: {}", phone, tenantList.size());
            return AppResponse.success(tenantList);

        } catch (Exception e) {
            log.error("가져오기테넌트목록실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기테넌트목록실패: " + e.getMessage());
        }
    }

    /**
     * 제거테넌트이름중의기기후(사용후일개 '#' 로분기호).
     */
    private Tenant stripTenantNameSalt(Tenant tenant) {
        if (tenant == null) {
            return null;
        }
        String name = tenant.getName();
        if (StringUtils.isBlank(name)) {
            return tenant;
        }
        int idx = name.lastIndexOf(TENANT_NAME_SEPARATOR);
        if (idx > 0 && idx < name.length() - 1) {
            tenant.setName(name.substring(0, idx));
        }
        return tenant;
    }

    @Override
    public AppResponse<List<String>> getAllTenantId() {
        try {
            List<String> tenantIds = tenantDao.getAllTenantId(databaseName);
            return AppResponse.success(tenantIds);
        } catch (Exception e) {
            log.error("가져오기모든테넌트ID실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기모든테넌트ID실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<List<String>> getTenantManagerIds(String tenantId) {
        try {
            if (StringUtils.isBlank(tenantId)) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "테넌트 ID는 비워 둘 수 없습니다");
            }
            // tenant_user_type = 1 테이블테넌트관리관리원
            List<String> managerIds = tenantDao.getTenantUserIdsByType(databaseName, tenantId, 1);
            return AppResponse.success(managerIds);
        } catch (Exception e) {
            log.error("가져오기테넌트관리관리원ID실패, tenantId: {}", tenantId, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기테넌트관리관리원ID실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<List<String>> getTenantNormalUserIds(String tenantId) {
        try {
            if (StringUtils.isBlank(tenantId)) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "테넌트 ID는 비워 둘 수 없습니다");
            }
            List<String> normalUserIds = tenantDao.getTenantUserIdsByType(databaseName, tenantId, 2);
            return AppResponse.success(normalUserIds);
        } catch (Exception e) {
            log.error("가져오기테넌트통신사용자ID실패, tenantId: {}", tenantId, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기테넌트통신사용자ID실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<List<String>> getNoClassifyTenantIds() {
        try {
            List<String> tenantIds = tenantDao.getNoClassifyTenantIds(databaseName);
            return AppResponse.success(tenantIds);
        } catch (Exception e) {
            log.error("가져오기지원하지 않는유형테넌트id실패");
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기지원하지 않는유형테넌트id실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<Integer> updateTenantClassifyCompleted(List<String> ids) {
        try {
            Integer i = tenantDao.updateTenantClassifyCompleted(databaseName, ids);
            return AppResponse.success(i);
        } catch (Exception e) {
            log.error("업데이트테넌트분유형완료로그실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "업데이트테넌트분유형완료로그실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<List<String>> getAllEnterpriseTenantId() {
        try {
            List<String> tenantIds = tenantDao.getAllEnterpriseTenantId(databaseName);
            return AppResponse.success(tenantIds);
        } catch (Exception e) {
            log.error("가져오기모든테넌트ID목록실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기모든테넌트ID목록실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<Integer> getTenantUserType(String userId, String tenantId) {
        try {
            if (StringUtils.isBlank(userId) || StringUtils.isBlank(tenantId)) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "사용자ID및테넌트 ID는 비워 둘 수 없습니다");
            }
            Integer tenantUserType = tenantDao.getTenantUserType(databaseName, userId, tenantId);
            return AppResponse.success(tenantUserType);
        } catch (Exception e) {
            log.error("가져오기테넌트사용자유형실패, userId: {}, tenantId: {}", userId, tenantId, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기테넌트사용자유형실패: " + e.getMessage());
        }
    }

    public AppResponse<TenantExpirationDto> getTenantExpiration(HttpServletRequest request) {
        try {
            // 1. 가져오기테넌트ID및코드
            String tenantId = TenantUtils.getTenantId();
            if (StringUtils.isBlank(tenantId)) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기테넌트ID실패");
            }

            UapTenant uapTenant = UapUserInfoAPI.getTenant(request);
            if (uapTenant == null) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기테넌트 정보실패");
            }

            String tenantCode = uapTenant.getTenantCode();
            if (StringUtils.isBlank(tenantCode)) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "테넌트 코드가 비어 있습니다");
            }

            // 2. 생성반환DTO정보
            TenantExpirationDto dto = new TenantExpirationDto();
            dto.setTenantId(tenantId);
            dto.setTenantType(determineTenantType(tenantCode));

            // 3. 가져오기현재로그인사용자의loginName
            String loginName = null;
            try {
                UapUser loginUser = UapUserInfoAPI.getLoginUser(request);
                if (loginUser != null && StringUtils.isNotBlank(loginUser.getLoginName())) {
                    loginName = loginUser.getLoginName();
                }
            } catch (Exception e) {
                log.warn("가져오기현재로그인사용자 정보실패, 를건너뛰기사용자빈조회", e);
            }

            // 4. 계획까지정보
            calculateAndFillExpirationInfo(dto, tenantId, tenantCode, loginName);

            log.info(
                    "조회테넌트까지정보성공, tenantId: {}, expirationDate: {}, remainingDays: {}, isExpired: {}, shouldAlert: {}",
                    tenantId,
                    dto.getExpirationDate(),
                    dto.getRemainingDays(),
                    dto.getIsExpired(),
                    dto.getShouldAlert());

            return AppResponse.success(dto);

        } catch (Exception e) {
            log.error("조회테넌트까지정보실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회테넌트까지정보실패: " + e.getMessage());
        }
    }

    @Override
    public boolean checkSpaceExpired(HttpServletRequest request) {
        try {
            // 복사사용getTenantExpiration의
            AppResponse<TenantExpirationDto> response = getTenantExpiration(request);
            if (!response.ok()) {
                // 결과가조회실패, 로완료설치전체, 아니요중지방문
                log.warn("테넌트 정보 조회 실패, 접근을 차단하지 않습니다");
                return false;
            }

            TenantExpirationDto dto = response.getData();
            if (dto == null) {
                return false;
            }

            // 반환여부까지
            return dto.getIsExpired() != null && dto.getIsExpired();
        } catch (Exception e) {
            log.error("조회빈까지상태실패", e);
            // 조회실패시, 로완료설치전체, 아니요중지방문
            return false;
        }
    }

    /**
     * 근거테넌트코드테넌트유형
     *
     * @param tenantCode 테넌트코드
     * @return 테넌트유형(personal/professional/enterprise_purchased/enterprise_subscription)
     */
    private String determineTenantType(String tenantCode) {
        if (tenantCode != null && tenantCode.startsWith(UAPConstant.PERSONAL_TENANT_CODE)) {
            return UAPConstant.TENANT_TYPE_PERSONAL;
        } else if (tenantCode != null && tenantCode.startsWith(UAPConstant.PROFESSIONAL_TENANT_CODE)) {
            return UAPConstant.TENANT_TYPE_PROFESSIONAL;
        } else if (tenantCode != null && tenantCode.startsWith(UAPConstant.ENTERPRISE_PURCHASED_TENANT_CODE)) {
            return UAPConstant.TENANT_TYPE_ENTERPRISE_PURCHASED;
        } else if (tenantCode != null && tenantCode.startsWith(UAPConstant.ENTERPRISE_SUBSCRIPTION_TENANT_CODE)) {
            return UAPConstant.TENANT_TYPE_ENTERPRISE_SUBSCRIPTION;
        } else {
            // 반환개사람버전
            log.warn("지원하지 않는의테넌트유형, tenantCode: {}", tenantCode);
            return UAPConstant.TENANT_TYPE_PERSONAL;
        }
    }

    @Override
    public void fillTenantExpirationInfo(Tenant tenant) {
        if (tenant == null || StringUtils.isBlank(tenant.getId())) {
            return;
        }

        String tenantId = tenant.getId();
        String tenantCode = tenant.getTenantCode();

        TenantExpirationDto dto = new TenantExpirationDto();
        // fillTenantExpirationInfo 사용테넌트목록정보, 아니요필요조회사용자여부에서테넌트중
        // 으로아니요loginName매개변수, 사용null
        calculateAndFillExpirationInfo(dto, tenantId, tenantCode, null);

        // 까지정보까지테넌트객체
        tenant.setExpirationDate(dto.getExpirationDate());
        tenant.setRemainingDays(dto.getRemainingDays());
        tenant.setIsExpired(dto.getIsExpired());
        tenant.setShouldAlert(dto.getShouldAlert());
    }

    /**
     * 계획까지정보의(공유방법법)
     * 근거테넌트ID및코드계획까지정보, 까지DTO객체중
     *
     * @param dto 필요의DTO객체(TenantExpirationDto또는Tenant)
     * @param tenantId 테넌트ID
     * @param tenantCode 테넌트코드
     * @param loginName 사용자로그인이름(사용조회사용자여부에서테넌트중.결과가로null, 이면건너뛰기사용자조회)
     */
    private void calculateAndFillExpirationInfo(
            TenantExpirationDto dto, String tenantId, String tenantCode, String loginName) {
        try {
            // 테넌트유형
            String tenantType = determineTenantType(tenantCode);

            // 개사람버전및버전아니요제한
            if (UAPConstant.TENANT_TYPE_PERSONAL.equals(tenantType)
                    || UAPConstant.TENANT_TYPE_ENTERPRISE_PURCHASED.equals(tenantType)) {
                setDefaultExpirationInfo(dto);
                return;
            }

            // 버전및버전필요조회까지정보
            // 1. 조회사용자여부에서현재테넌트빈중(결과가완료loginName매개변수)
            if (StringUtils.isNotBlank(loginName)) {
                boolean hasSpace = checkTenantHasSpace(tenantId, loginName);
                if (!hasSpace) {
                    // 결과가사용자아니요에서현재테넌트빈중, isExpired로true, 테이블빈할 수 없음사용
                    log.debug("사용자아니요에서테넌트{}빈중, loginName: {}", tenantId, loginName);
                    dto.setExpirationDate(null);
                    dto.setRemainingDays(null);
                    dto.setIsExpired(true); // 아니요에서빈중로완료까지
                    dto.setShouldAlert(false);
                    return;
                }
            }

            // 2. 테넌트있음빈, 조회까지정보(저장)
            TenantExpiration expiration = getTenantExpirationWithCache(tenantId);
            if (expiration == null) {
                // 버전및버전있음까지정보, 결과가있음, 예외
                log.error("테넌트{}있음까지정보, 테넌트유형: {}", tenantId, tenantType);
                throw new ServiceException("빈까지, 요청 시스템관리관리요소");
            }

            // 파싱까지날짜
            String expirationDateStr = expiration.getExpirationDate();
            if (StringUtils.isBlank(expirationDateStr)) {
                // 버전및버전있음까지시간, 결과가있음, 예외
                log.error("테넌트{}까지시간비어 있습니다, 테넌트유형: {}", tenantId, tenantType);
                throw new ServiceException("일치하지 않는빈있음, 시스템 관리자에게 문의하세요");
            }

            // 시도복호화
            try {
                expirationDateStr = EncryptUtils.decrypt(expirationDateStr);
            } catch (Exception e) {
                log.error("복호화테넌트까지시간실패, tenantId: {}, 테넌트유형: {}", tenantId, tenantType, e);
                // 복호화실패, 설명까지시간없음, 예외
                throw new ServiceException("빈까지, 요청 시스템관리관리요소");
            }

            // 파싱날짜계획데이터
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate expirationDate;
            try {
                expirationDate = LocalDate.parse(expirationDateStr, formatter);
            } catch (Exception e) {
                log.error(
                        "파싱까지날짜실패, tenantId: {}, expirationDate: {}, 테넌트유형: {}",
                        tenantId,
                        expirationDateStr,
                        tenantType,
                        e);
                // 파싱실패, 설명까지시간형식없음, 예외
                throw new ServiceException("빈까지, 요청 시스템관리관리요소");
            }

            LocalDate now = LocalDate.now();
            long remainingDays = ChronoUnit.DAYS.between(now, expirationDate);
            boolean isExpired = remainingDays < 0;
            boolean shouldAlert = remainingDays >= 0 && remainingDays <= alertDays;

            // 까지정보
            dto.setExpirationDate(expirationDateStr);
            dto.setRemainingDays(remainingDays);
            dto.setIsExpired(isExpired);
            dto.setShouldAlert(shouldAlert);

        } catch (ServiceException e) {
            // ServiceException필요위출력, 아니요가져오기
            throw e;
        } catch (Exception e) {
            log.error("계획테넌트까지정보실패, tenantId: {}", tenantId, e);
            // 예외비어 있습니다까지
            throw new ServiceException("빈까지, 요청 시스템관리관리요소");
        }
    }

    /**
     * 조회사용자여부에서현재테넌트빈중(저장)
     * 통신경과호출ClientAuthenticationAPI.getTenantListInAppByLoginName사용자여부에서테넌트중
     *
     * @param tenantId 테넌트ID
     * @param loginName 사용자로그인이름
     * @return true테이블사용자에서테넌트빈중, false테이블사용자아니요에서테넌트빈중
     */
    private boolean checkTenantHasSpace(String tenantId, String loginName) {
        try {
            if (StringUtils.isBlank(loginName)) {
                log.warn("로그인이름비어 있습니다, 불가조회사용자여부에서테넌트빈중");
                // 로그인이름비어 있습니다시, 로완료설치전체, 아니요에서빈중
                return false;
            }

            // 저장key사용loginName및tenantId, 원인로아니요사용자일테넌트의방문권한가능아니요
            String redisKey = REDIS_KEY_TENANT_HAS_SPACE_PREFIX + loginName + ":" + tenantId;

            // 에서Redis저장중가져오기(저장시간2시간)
            Object cache = RedisUtils.get(redisKey);
            if (cache != null && StringUtils.isNotBlank(cache.toString())) {
                String cacheStr = cache.toString();
                // 결과가예빈값, 설명전조회경과사용자아니요에서빈중, 직선연결반환false
                if ("false".equals(cacheStr)) {
                    log.debug("에서 Redis 저장가져오기까지사용자아니요에서테넌트빈중, loginName: {}, tenantId: {}", loginName, tenantId);
                    return false;
                } else if ("true".equals(cacheStr)) {
                    log.debug("에서 Redis 저장가져오기까지사용자에서테넌트빈중, loginName: {}, tenantId: {}", loginName, tenantId);
                    return true;
                }
            }

            // 결과가Redis중있음, 호출UAP API조회사용자의테넌트목록
            List<UapTenant> tenantList = ClientAuthenticationAPI.getTenantListInAppByLoginName(loginName);
            if (CollectionUtils.isEmpty(tenantList)) {
                log.warn("사용자있음테넌트 정보, loginName: {}", loginName);
                // 저장결과(2시간)
                try {
                    RedisUtils.set(redisKey, "false", TENANT_SPACE_EMPTY_CACHE_EXPIRE_SECONDS);
                } catch (Exception e) {
                    log.warn("저장입력 Redis 저장실패, loginName: {}, tenantId: {}", loginName, tenantId, e);
                }
                return false;
            }

            // 조회현재테넌트ID여부에서사용자의테넌트목록중
            boolean hasSpace = tenantList.stream().anyMatch(tenant -> tenantId.equals(tenant.getId()));

            // 조회까지결과후저장입력Redis저장(2시간)
            try {
                RedisUtils.set(redisKey, hasSpace ? "true" : "false", TENANT_SPACE_CACHE_EXPIRE_SECONDS);
                log.debug(
                        "사용자테넌트빈있음상태완료저장입력 Redis 저장, loginName: {}, tenantId: {}, hasSpace: {}",
                        loginName,
                        tenantId,
                        hasSpace);
            } catch (Exception e) {
                log.warn("저장입력 Redis 저장실패, loginName: {}, tenantId: {}", loginName, tenantId, e);
            }

            return hasSpace;

        } catch (Exception e) {
            log.error("조회사용자여부에서테넌트빈중실패, tenantId: {}", tenantId, e);
            // 조회실패시, 로완료설치전체, 아니요에서빈중, 중지방문
            return false;
        }
    }

    /**
     * 가져오기테넌트까지정보(저장)
     *
     * @param tenantId 테넌트ID
     * @return 테넌트까지정보, 결과가찾을 수 없습니다반환null
     */
    private TenantExpiration getTenantExpirationWithCache(String tenantId) {
        try {
            String redisKey = REDIS_KEY_TENANT_EXPIRATION_PREFIX + tenantId;
            TenantExpiration expiration = null;
            boolean fromCache = false;

            // 에서 Redis 저장중가져오기
            try {
                Object cache = RedisUtils.get(redisKey);
                if (cache != null && StringUtils.isNotBlank(cache.toString())) {
                    String cacheStr = cache.toString();
                    // 결과가예빈값, 설명전조회경과데이터베이스있음데이터, 직선연결반환null
                    if ("{}".equals(cacheStr)) {
                        log.debug("에서 Redis 저장가져오기까지빈값, tenantId: {}", tenantId);
                        return null;
                    } else {
                        ObjectMapper objectMapper = new ObjectMapper();
                        expiration = objectMapper.readValue(cacheStr, TenantExpiration.class);
                        fromCache = true;
                        log.debug("에서 Redis 저장가져오기테넌트까지정보, tenantId: {}", tenantId);
                    }
                }
            } catch (Exception e) {
                log.warn("에서 Redis 가져오기테넌트까지정보실패, tenantId: {}, 를조회데이터베이스", tenantId, e);
            }

            // 결과가 Redis 중있음, 조회데이터베이스
            if (!fromCache) {
                expiration = tenantExpirationDao.queryByTenantId(tenantId);

                // 조회까지데이터후저장입력 Redis 저장(2시간, 로기기제어)
                // 비고: 결과가후있음업데이트까지정보의연결, 생성에서업데이트시삭제저장
                if (expiration != null) {
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        String expirationJson = objectMapper.writeValueAsString(expiration);
                        RedisUtils.set(redisKey, expirationJson, TENANT_SPACE_CACHE_EXPIRE_SECONDS);
                        log.debug("테넌트까지정보완료저장입력 Redis 저장, tenantId: {}", tenantId);
                    } catch (Exception e) {
                        log.warn("저장입력 Redis 저장실패, tenantId: {}", tenantId, e);
                    }
                } else {
                    // 결과가있음까지정보, 저장일개빈값, 조회데이터베이스(2시간)
                    try {
                        RedisUtils.set(redisKey, "{}", TENANT_SPACE_EMPTY_CACHE_EXPIRE_SECONDS);
                    } catch (Exception e) {
                        log.warn("저장입력 Redis 빈값실패, tenantId: {}", tenantId, e);
                    }
                }
            }

            return expiration;

        } catch (Exception e) {
            log.error("가져오기테넌트까지정보실패, tenantId: {}", tenantId, e);
            // 실패시반환null, 호출방법관리
            return null;
        }
    }

    /**
     * 의까지정보까지DTO(사용예외)
     *
     * @param dto DTO객체
     */
    private void setDefaultExpirationInfo(TenantExpirationDto dto) {
        if (dto != null) {
            dto.setExpirationDate(null);
            dto.setRemainingDays(null);
            dto.setIsExpired(false);
            dto.setShouldAlert(false);
        }
    }
}