package com.iflytek.rpa.auth.sp.casdoor.service.impl;

import com.iflytek.rpa.auth.core.entity.*;
import com.iflytek.rpa.auth.core.service.TenantService;
import com.iflytek.rpa.auth.sp.casdoor.dao.CasdoorTenantDao;
import com.iflytek.rpa.auth.sp.casdoor.mapper.CasdoorOrganizationMapper;
import com.iflytek.rpa.auth.sp.casdoor.mapper.CasdoorTenantMapper;
import com.iflytek.rpa.auth.sp.casdoor.service.extend.CasdoorGroupExtendService;
import com.iflytek.rpa.auth.sp.casdoor.service.extend.CasdoorUserExtendService;
import com.iflytek.rpa.auth.sp.casdoor.utils.SessionUserUtils;
import com.iflytek.rpa.auth.utils.AppResponse;
import com.iflytek.rpa.auth.utils.ErrorCodeEnum;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.casbin.casdoor.entity.Group;
import org.casbin.casdoor.entity.Organization;
import org.casbin.casdoor.entity.User;
import org.casbin.casdoor.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * @desc: TODO
 * @author: weilai <laiwei3@iflytek.com>
 * @create: 2025/12/10 17:28
 */
@Slf4j
@Service("casdoorTenantService")
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "casdoor", matchIfMissing = true)
public class CasdoorTenantServiceImpl implements TenantService {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private CasdoorGroupExtendService casdoorGroupExtendService;

    @Autowired
    private CasdoorTenantMapper casdoorTenantMapper;

    @Autowired
    private CasdoorOrganizationMapper casdoorOrganizationMapper;

    @Autowired
    private CasdoorUserExtendService casdoorUserExtendService;

    @Autowired
    private CasdoorTenantDao casdoorTenantDao;

    @Value("${casdoor.database.name:casdoor}")
    private String databaseName;

    /**
     * 테넌트아래모든사용자
     * @param userName 조직이름(organizationName)
     * @return 사용자목록
     * @throws Exception 예외
     */
    @Override
    public AppResponse<List<UserVo>> getAllUser(String userName) throws Exception {

        try {
            log.debug("열기 조회테넌트아래모든사용자, organizationName: {}", userName);

            // 매개변수검증
            if (Objects.isNull(userName) || userName.trim().isEmpty()) {
                log.warn("조회테넌트아래모든사용자실패: 조직이름비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "조직이름비워 둘 수 없습니다");
            }

            // 조회사용자목록
            List<User> users = casdoorUserExtendService.getUsers(userName);
            if (CollectionUtils.isEmpty(users)) {
                log.debug("조직아래있음사용자, organizationName: {}", userName);
                return AppResponse.success(Collections.emptyList());
            }

            log.debug("조회까지 {} 개사용자, organizationName: {}", users.size(), userName);

            // 변환로UserVo목록, 필터링변환실패의객체
            List<UserVo> userVoList = users.stream()
                    .filter(user -> user != null)
                    .map(user -> {
                        try {
                            UserVo userVo = new UserVo();
                            userVo.setUserId(user.id);
                            userVo.setUserPhone(user.phone);
                            userVo.setUserName(user.name);
                            return userVo;
                        } catch (Exception e) {
                            log.warn(
                                    "사용자 정보변환실패, userId: {}, organizationName: {}",
                                    user != null ? user.id : "null",
                                    userName,
                                    e);
                            return null;
                        }
                    })
                    .filter(userVo -> userVo != null)
                    .collect(Collectors.toList());

            log.debug("성공변환 {} 개사용자, organizationName: {}", userVoList.size(), userName);
            return AppResponse.success(userVoList);
        } catch (IOException e) {
            log.error("조회테넌트아래모든사용자실패, organizationName: {}", userName, e);
            return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "조회테넌트아래모든사용자실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("조회테넌트아래모든사용자예외, organizationName: {}", userName, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회테넌트아래모든사용자예외: " + e.getMessage());
        }
    }

    /**
     * 현재로그인사용자에서사용의테넌트목록
     * @param request HTTP요청 
     * @return 테넌트목록
     */
    @Override
    public AppResponse<List<Tenant>> getTenantListInApp(HttpServletRequest request) {
        try {
            log.debug("조회현재로그인사용자에서사용의테넌트목록(Casdoor지원하지 않음공가능, 반환빈목록)");
            return AppResponse.success(Collections.emptyList());
        } catch (Exception e) {
            log.error("조회현재로그인사용자에서사용의테넌트목록예외", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회현재로그인사용자에서사용의테넌트목록실패: " + e.getMessage());
        }
    }

    /**
     * 정보조회
     * @param request HTTP요청 
     * @return 정보
     * @throws Exception 예외
     */
    @Override
    public AppResponse<TenantInfoDto> getTenantInfo(HttpServletRequest request) throws Exception {
        try {
            log.debug("열기 조회정보");

            // 1. 가져오기현재테넌트ID
            User user = SessionUserUtils.getUserFromSession(request);
            if (user == null) {
                log.warn("조회정보실패: 사용자로그인되지 않았습니다");
                return AppResponse.error(ErrorCodeEnum.E_NOT_LOGIN, "사용자로그인되지 않았습니다");
            }

            if (user.owner == null || user.owner.isEmpty()) {
                log.warn("조회정보실패: 테넌트ID비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_SERVICE_INFO_LOSE, "테넌트ID비어 있습니다");
            }

            String tenantId = user.owner;
            log.debug("가져오기까지테넌트ID: {}", tenantId);

            // 2. 조회Casdoor조직정보
            Organization organization = organizationService.getOrganization(tenantId);
            if (Objects.isNull(organization)) {
                log.warn("조회하지 못한조직정보, tenantId: {}", tenantId);
                return AppResponse.error(ErrorCodeEnum.E_NO_ACCOUNT, "조회하지 못한조직정보");
            }

            // 3. 사용mapper변환로Tenant
            Tenant tenant = casdoorTenantMapper.toCommonTenant(organization);
            if (Objects.isNull(tenant)) {
                log.warn("조직정보변환실패, tenantId: {}", tenantId);
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조직정보변환실패");
            }

            log.debug("테넌트 정보변환성공, tenantId: {}, tenantName: {}", tenantId, tenant.getName());

            // 4. 에서Tenant변환로TenantInfoDto
            TenantInfoDto tenantInfoDto = new TenantInfoDto();
            tenantInfoDto.setId(tenant.getId());
            tenantInfoDto.setName(tenant.getName());
            tenantInfoDto.setCode(tenant.getTenantCode());

            // TODO: 조회관리관리원정보, 목록전시예admin(casdoor내부테넌트)
            if (organization.owner != null && !organization.owner.isEmpty()) {
                tenantInfoDto.setManagerId(organization.owner);
                // 시사용owner로managerName, 후가능으로근거필요조회사용자정보
                tenantInfoDto.setManagerName(organization.owner);
            }

            log.debug("정보조회성공, tenantId: {}, tenantName: {}", tenantId, tenantInfoDto.getName());
            return AppResponse.success(tenantInfoDto);
        } catch (IOException e) {
            log.error("조회정보실패", e);
            return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "조회정보실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("조회정보예외", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회정보예외: " + e.getMessage());
        }
    }

    /**
     * 가져오기테넌트ID
     * @param request HTTP요청 
     * @return 테넌트ID
     */
    @Override
    public AppResponse<String> getTenantId(HttpServletRequest request) {
        try {
            // 에서session가져오기현재사용자
            User user = SessionUserUtils.getUserFromSession(request);

            if (user != null) {
                if (user.owner != null && !user.owner.isEmpty()) {
                    return AppResponse.success(user.owner);
                } else {
                    return AppResponse.error(ErrorCodeEnum.E_SERVICE_INFO_LOSE, "테넌트ID비어 있습니다");
                }
            } else {
                return AppResponse.success("7fd5161b-4bcc-4309-b5ec-8035fcdfceeb");
            }
        } catch (Exception e) {
            log.error("가져오기테넌트ID실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기테넌트ID실패: " + e.getMessage());
        }
    }

    /**
     * 가져오기현재로그인의테넌트ID
     * @param request HTTP요청 
     * @return 현재로그인의테넌트ID
     */
    @Override
    public AppResponse<String> getCurrentTenantId(HttpServletRequest request) {
        return getTenantId(request);
    }

    /**
     * 가져오기현재로그인의테넌트이름(ID)
     * @param request HTTP요청 
     * @return 현재로그인의테넌트이름
     */
    @Override
    public AppResponse<String> getCurrentTenantName(HttpServletRequest request) {
        return getTenantId(request);
    }

    /**
     * 근거테넌트ID조회테넌트 정보
     * @param tenantId 테넌트ID
     * @param request HTTP요청 
     * @return 테넌트 정보
     */
    @Override
    public AppResponse<Tenant> queryTenantInfoById(String tenantId, HttpServletRequest request) {
        try {
            // 매개변수검증
            if (Objects.isNull(tenantId) || tenantId.trim().isEmpty()) {
                log.warn("조회테넌트 정보실패: 테넌트ID비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "테넌트 ID는 비워 둘 수 없습니다");
            }

            log.debug("열기 조회테넌트 정보, tenantId: {}", tenantId);

            // 조회Casdoor조직정보
            Organization organization = organizationService.getOrganization(tenantId);
            if (Objects.isNull(organization)) {
                log.warn("조회하지 못한조직정보, tenantId: {}", tenantId);
                return AppResponse.error(ErrorCodeEnum.E_NO_ACCOUNT, "조회하지 못한조직정보");
            }

            // 변환로통신사용테넌트객체
            Tenant commonTenant = casdoorTenantMapper.toCommonTenant(organization);
            if (Objects.isNull(commonTenant)) {
                log.warn("조직정보변환실패, tenantId: {}", tenantId);
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조직정보변환실패");
            }

            log.debug("조회테넌트 정보성공, tenantId: {}, tenantName: {}", tenantId, commonTenant.getName());
            return AppResponse.success(commonTenant);
        } catch (IOException e) {
            log.error("조회테넌트 정보실패, tenantId: {}", tenantId, e);
            return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "조회테넌트 정보실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("조회테넌트 정보예외, tenantId: {}", tenantId, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회테넌트 정보예외: " + e.getMessage());
        }
    }

    /**
     * 변경수정관리관리원(지원하지 않음)
     * @param id 관리관리원ID
     * @param request HTTP요청 
     * @return 결과
     */
    @Override
    public AppResponse<String> changeManager(String id, HttpServletRequest request) {
        try {
            log.debug("변경수정관리관리원, id: {}(Casdoor지원하지 않음공가능, 반환안내정보)", id);
            return AppResponse.success("Casdoor지원하지 않음변경수정관리관리원공가능");
        } catch (Exception e) {
            log.error("변경수정관리관리원예외, id: {}", id, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "변경수정관리관리원실패: " + e.getMessage());
        }
    }

    /**
     * 근거테넌트id가져오기모든조직목록
     * @param tenantId 테넌트ID
     * @param request HTTP요청 
     * @return 조직목록
     */
    @Override
    public AppResponse<List<Org>> getAllOrgList(String tenantId, HttpServletRequest request) {
        try {
            // 매개변수검증
            if (Objects.isNull(tenantId) || tenantId.trim().isEmpty()) {
                log.warn("가져오기조직목록실패: 테넌트ID비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "테넌트 ID는 비워 둘 수 없습니다");
            }

            log.debug("열기 가져오기조직목록, tenantId: {}", tenantId);

            // 조회Casdoor그룹목록
            List<Group> groups = casdoorGroupExtendService.getGroups(tenantId);
            if (CollectionUtils.isEmpty(groups)) {
                log.debug("테넌트아래있음조직, tenantId: {}", tenantId);
                return AppResponse.success(Collections.emptyList());
            }

            log.debug("조회까지 {} 개조직, tenantId: {}", groups.size(), tenantId);

            // 변환로통신사용조직객체목록, 필터링변환실패의객체
            List<Org> orgList = groups.stream()
                    .map(casdoorOrganizationMapper::toCommonOrg)
                    .filter(org -> org != null)
                    .collect(Collectors.toList());

            log.debug("성공변환 {} 개조직, tenantId: {}", orgList.size(), tenantId);
            return AppResponse.success(orgList);
        } catch (IOException e) {
            log.error("가져오기조직목록실패, tenantId: {}", tenantId, e);
            return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "가져오기조직목록실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("가져오기조직목록예외, tenantId: {}", tenantId, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기조직목록예외: " + e.getMessage());
        }
    }

    /**
     * 테넌트(지원하지 않음)
     * @param tenantId 테넌트id
     * @param request HTTP요청 
     * @return 결과
     */
    @Override
    public AppResponse<String> switchTenant(String tenantId, HttpServletRequest request) {
        try {
            log.debug("테넌트, tenantId: {}(Casdoor지원하지 않음공가능, 반환안내정보)", tenantId);
            return AppResponse.success("Casdoor지원하지 않음테넌트공가능");
        } catch (Exception e) {
            log.error("테넌트예외, tenantId: {}", tenantId, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "테넌트실패: " + e.getMessage());
        }
    }

    /**
     * 근거휴대폰 번호조회사용자의테넌트목록
     * @param organizationName 조직이름
     * @param request HTTP요청 
     * @return 테넌트목록
     */
    @Override
    public AppResponse<List<Tenant>> getTenantList(String organizationName, HttpServletRequest request) {
        try {
            Organization organization = organizationService.getOrganization(organizationName);
            Tenant tenant = casdoorTenantMapper.toCommonTenant(organization);
            return AppResponse.success(Collections.singletonList(tenant));

        } catch (Exception e) {
            log.error("가져오기조직예외", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기테넌트목록실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<List<String>> getAllTenantId() {
        // Casdoor방식아래지원하지 않음공가능, 반환빈목록
        return AppResponse.success(Collections.emptyList());
    }

    @Override
    public AppResponse<List<String>> getTenantManagerIds(String tenantId) {
        // Casdoor방식아래지원하지 않음공가능, 반환빈목록
        return AppResponse.success(Collections.emptyList());
    }

    @Override
    public AppResponse<List<String>> getTenantNormalUserIds(String tenantId) {
        // Casdoor방식아래지원하지 않음공가능, 반환빈목록
        return AppResponse.success(Collections.emptyList());
    }

    @Override
    public AppResponse<List<String>> getNoClassifyTenantIds() {
        try {
            List<String> tenantIds = casdoorTenantDao.getNoClassifyTenantIds(databaseName);
            return AppResponse.success(tenantIds);
        } catch (Exception e) {
            log.error("가져오기지원하지 않는유형테넌트id실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기지원하지 않는유형테넌트id실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<Integer> updateTenantClassifyCompleted(List<String> ids) {
        // Casdoor방식아래지원하지 않음공가능, 반환성공업데이트수로1
        return AppResponse.success(1);
    }

    @Override
    public AppResponse<List<String>> getAllEnterpriseTenantId() {
        return AppResponse.success(new ArrayList<>());
    }

    @Override
    public AppResponse<Integer> getTenantUserType(String userId, String tenantId) {
        // 반환통신사용자
        return AppResponse.success(2);
    }

    @Override
    public AppResponse<TenantExpirationDto> getTenantExpiration(HttpServletRequest request) {
        // Casdoor방식아래지원하지 않음공가능, 반환의아니요제한테넌트 정보
        TenantExpirationDto dto = new TenantExpirationDto();
        dto.setTenantType("personal"); // 개사람버전
        // 가져오기현재로그인사용자의테넌트id
        String tenantId = SessionUserUtils.getTenantOwnerFromSession(request);
        dto.setTenantId(tenantId);
        dto.setExpirationDate(null); // 아니요제한
        dto.setRemainingDays(null); // 아니요제한
        dto.setIsExpired(false); // 아니요제한
        dto.setShouldAlert(false); // 아니요제한
        return AppResponse.success(dto);
    }

    @Override
    public boolean checkSpaceExpired(HttpServletRequest request) {
        // Casdoor방식아래지원하지 않음공가능, 반환미완료경과
        return false;
    }

    @Override
    public void fillTenantExpirationInfo(Tenant tenant) {
        // Casdoor방식아래지원하지 않음공가능, 아니요작업정보
    }
}
