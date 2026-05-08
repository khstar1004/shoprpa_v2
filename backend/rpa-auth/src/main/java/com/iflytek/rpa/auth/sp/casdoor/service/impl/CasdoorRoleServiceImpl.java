package com.iflytek.rpa.auth.sp.casdoor.service.impl;

import com.iflytek.rpa.auth.core.entity.*;
import com.iflytek.rpa.auth.core.service.RoleService;
import com.iflytek.rpa.auth.sp.casdoor.dao.CasdoorRoleDao;
import com.iflytek.rpa.auth.sp.casdoor.mapper.CasdoorRoleMapper;
import com.iflytek.rpa.auth.sp.casdoor.utils.SessionUserUtils;
import com.iflytek.rpa.auth.utils.AppResponse;
import com.iflytek.rpa.auth.utils.ErrorCodeEnum;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.casbin.casdoor.util.http.CasdoorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @desc: ShopRPA authentication compatibility component.
 * @author: weilai <laiwei3@iflytek.com>
 * @create: 2025/12/11 9:46
 */
@Slf4j
@Service("casdoorRoleService")
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "casdoor", matchIfMissing = true)
public class CasdoorRoleServiceImpl implements RoleService {

    @Autowired
    private CasdoorRoleMapper casdoorRoleMapper;

    @Autowired
    private org.casbin.casdoor.service.RoleService roleService;

    @Autowired
    private CasdoorRoleDao casdoorRoleDao;

    @Value("${casdoor.database.name:casdoor}")
    private String databaseName;

    /**
     * 조회사용내부전체역할목록
     * @param request HTTP요청 
     * @return 역할목록
     */
    @Override
    public AppResponse<List<Role>> getUserRoleListInApp(HttpServletRequest request) throws IOException {
        try {
            log.debug("열기 조회사용내부전체역할목록");

            // casdoor의역할목록예직선연결조직의, 조직중패키지사용, 사용및역할없음지정닫기시스템, 조직아래모든역할목록 관리
            AppResponse<List<Role>> response = getUserRoleList(request);

            if (response.ok() && response.getData() != null) {
                log.debug("조회사용내부전체역할목록성공, 공유 {} 개역할", response.getData().size());
            }

            return response;
        } catch (Exception e) {
            log.error("조회사용내부전체역할목록예외", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회사용내부전체역할목록예외: " + e.getMessage());
        }
    }

    /**
     * 조회역할목록
     * @param request HTTP요청 
     * @return 역할목록
     */
    @Override
    public AppResponse<List<Role>> getUserRoleList(HttpServletRequest request) throws IOException {
        try {
            log.debug("열기 조회역할목록");

            // 조회Casdoor역할목록
            List<org.casbin.casdoor.entity.Role> casdoorRoles = roleService.getRoles();
            if (casdoorRoles == null) {
                log.debug("조회역할목록결과비어 있습니다");
                return AppResponse.success(Collections.emptyList());
            }

            log.debug("조회까지 {} 개Casdoor역할", casdoorRoles.size());

            // 변환로통신사용역할객체목록, 필터링변환실패의객체
            List<Role> roles = casdoorRoles.stream()
                    .filter(role -> role != null)
                    .map(role -> {
                        try {
                            return casdoorRoleMapper.toCommonRole(role);
                        } catch (Exception e) {
                            log.warn("역할정보변환실패, roleName: {}", role != null ? role.name : "null", e);
                            return null;
                        }
                    })
                    .filter(role -> role != null)
                    .collect(Collectors.toList());

            log.debug("성공변환 {} 개역할", roles.size());
            return AppResponse.success(roles);
        } catch (IOException e) {
            log.error("조회역할목록실패", e);
            return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "조회역할목록실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("조회역할목록예외", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회역할목록예외: " + e.getMessage());
        }
    }

    /**
     * 조회역할
     * @param dto 조회매개변수
     * @param request HTTP요청 
     * @return 역할
     */
    @Override
    public AppResponse<Role> queryRoleDetail(GetRoleDto dto, HttpServletRequest request) throws IOException {
        try {
            log.debug("열기 조회역할, roleId: {}", dto != null ? dto.getId() : "null");

            // 매개변수검증
            if (dto == null || StringUtils.isEmpty(dto.getId())) {
                log.warn("조회역할실패: 역할ID비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "역할 ID는 비워 둘 수 없습니다");
            }

            String roleId = dto.getId();
            log.debug("조회역할, roleId: {} (casdoor의역할이름)", roleId);

            // 조회Casdoor역할(의역할idcasdoor의역할이름)
            org.casbin.casdoor.entity.Role casdoorRole = roleService.getRole(roleId);
            if (casdoorRole == null) {
                log.warn("조회하지 못한역할정보, roleId: {}", roleId);
                return AppResponse.error(ErrorCodeEnum.E_NO_ACCOUNT, "역할찾을 수 없습니다");
            }

            // 변환로통신사용역할객체
            Role commonRole = casdoorRoleMapper.toCommonRole(casdoorRole);
            if (commonRole == null) {
                log.warn("역할정보변환실패, roleId: {}", roleId);
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "역할정보변환실패");
            }

            log.debug("조회역할완료, roleId: {}, roleName: {}", roleId, commonRole.getName());
            return AppResponse.success(commonRole);
        } catch (IOException e) {
            log.error("조회역할실패, roleId: {}", dto != null ? dto.getId() : "null", e);
            return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "조회역할실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("조회역할예외, roleId: {}", dto != null ? dto.getId() : "null", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회역할예외: " + e.getMessage());
        }
    }

    /**
     * 추가역할
     * @param createRoleDto 추가역할DTO
     * @param request HTTP요청 
     * @return 결과
     */
    @Override
    public AppResponse<String> addRole(CreateRoleDto createRoleDto, HttpServletRequest request) throws IOException {
        try {
            log.debug("열기 추가역할");

            // 매개변수검증
            if (createRoleDto == null) {
                log.warn("추가역할실패: 매개변수가 비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "추가역할정보비워 둘 수 없습니다");
            }
            if (StringUtils.isBlank(createRoleDto.getName())) {
                log.warn("추가역할실패: 역할이름비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "역할 이름은 비워 둘 수 없습니다");
            }

            String roleName = createRoleDto.getName().trim();
            String roleCode = createRoleDto.getCode().trim();
            log.debug("추가역할매개변수, name: {}, code: {}", roleName, roleCode);

            // Casdoor중사용name로일식별자, 사용역할코드code로Casdoor의name
            org.casbin.casdoor.entity.Role existing = roleService.getRole(roleCode);
            if (existing != null) {
                log.warn("추가역할실패: 역할코드완료저장에서, code: {}", roleCode);
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "역할코드완료저장에서");
            }

            // 그룹설치Casdoor Role
            org.casbin.casdoor.entity.Role casdoorRole = new org.casbin.casdoor.entity.Role();
            // owner가져오기현재사용자의테넌트
            String currentTenantOwner = getCurrentTenantOwner(request);
            casdoorRole.owner = currentTenantOwner;
            casdoorRole.name = roleCode;
            casdoorRole.displayName = roleName;
            casdoorRole.description = createRoleDto.getRemark();
            casdoorRole.isEnabled = createRoleDto.getStatus() == null || createRoleDto.getStatus() == 1;

            log.debug(
                    "호출Casdoor API추가역할, owner: {}, name: {}, displayName: {}",
                    casdoorRole.owner,
                    casdoorRole.name,
                    casdoorRole.displayName);
            CasdoorResponse<String, Object> addRoleResponse = roleService.addRole(casdoorRole);

            if (addRoleResponse == null) {
                log.error("추가역할실패: Casdoor API반환비어 있습니다, code: {}", roleCode);
                return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "추가역할실패: API반환비어 있습니다");
            }

            if (addRoleResponse.getStatus() != null && !"ok".equals(addRoleResponse.getStatus())) {
                log.error(
                        "추가역할실패: Casdoor API반환오류, code: {}, status: {}, msg: {}",
                        roleCode,
                        addRoleResponse.getStatus(),
                        addRoleResponse.getMsg());
                return AppResponse.error(
                        ErrorCodeEnum.E_API_EXCEPTION,
                        "추가역할실패: " + (addRoleResponse.getMsg() != null ? addRoleResponse.getMsg() : "지원하지 않는오류"));
            }

            log.debug("추가역할성공, code: {}, name: {}", roleCode, roleName);
            return AppResponse.success("추가역할성공");
        } catch (IOException e) {
            log.error("추가역할실패(IO예외)", e);
            return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "추가역할실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("추가역할예외", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "추가역할예외: " + e.getMessage());
        }
    }

    /**
     * 역할
     * @param updateRoleDto 업데이트역할DTO
     * @param request HTTP요청 
     * @return 결과
     */
    @Override
    public AppResponse<String> updateRole(UpdateRoleDto updateRoleDto, HttpServletRequest request) throws IOException {
        try {
            log.debug("열기 역할");

            // 매개변수검증
            if (updateRoleDto == null) {
                log.warn("역할실패: 매개변수가 비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "업데이트역할정보비워 둘 수 없습니다");
            }
            if (StringUtils.isBlank(updateRoleDto.getId())) {
                log.warn("역할실패: 역할ID비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "역할 ID는 비워 둘 수 없습니다");
            }

            String roleId = updateRoleDto.getId().trim();
            log.debug("역할매개변수, id: {}, name: {}, code: {}", roleId, updateRoleDto.getName(), updateRoleDto.getCode());

            // Casdoor중사용name로일식별자, 지정: Role.id 저장의예 Casdoor 의 role.name(code)
            org.casbin.casdoor.entity.Role existingRole = roleService.getRole(roleId);
            if (existingRole == null) {
                log.warn("역할실패: 역할찾을 수 없습니다, id: {}", roleId);
                return AppResponse.error(ErrorCodeEnum.E_NO_ACCOUNT, "역할찾을 수 없습니다");
            }

            // 에서아니요수정변수Casdoor역할name의전아래, 업데이트이름, 설명, 사용상태대기정보
            org.casbin.casdoor.entity.Role casdoorRole = new org.casbin.casdoor.entity.Role();
            casdoorRole.owner = existingRole.owner;
            casdoorRole.name = existingRole.name; // 보관기존있음일식별자아니요변수

            // 이름: 사용업데이트이름, 아니요이면보관기존값
            if (StringUtils.isNotBlank(updateRoleDto.getName())) {
                casdoorRole.displayName = updateRoleDto.getName().trim();
            } else {
                casdoorRole.displayName = existingRole.displayName;
            }

            // 설명
            if (StringUtils.isNotBlank(updateRoleDto.getRemark())) {
                casdoorRole.description = updateRoleDto.getRemark().trim();
            } else {
                casdoorRole.description = existingRole.description;
            }

            // 사용상태: status (1사용 -> isEnabled=true, 0중지사용 -> isEnabled=false)
            if (updateRoleDto.getStatus() != null) {
                casdoorRole.isEnabled = (updateRoleDto.getStatus() == 1);
            } else {
                casdoorRole.isEnabled = existingRole.isEnabled;
            }

            // 보관기존있음지정닫기시스템
            casdoorRole.users = existingRole.users;
            casdoorRole.roles = existingRole.roles;

            log.debug("호출Casdoor API업데이트역할, name: {}, displayName: {}", casdoorRole.name, casdoorRole.displayName);
            CasdoorResponse<String, Object> updateRoleResponse = roleService.updateRole(casdoorRole);

            if (updateRoleResponse == null) {
                log.error("역할실패: Casdoor API반환비어 있습니다, id: {}", roleId);
                return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "역할실패: API반환비어 있습니다");
            }

            if (updateRoleResponse.getStatus() != null && !"ok".equals(updateRoleResponse.getStatus())) {
                log.error(
                        "역할실패: Casdoor API반환오류, id: {}, status: {}, msg: {}",
                        roleId,
                        updateRoleResponse.getStatus(),
                        updateRoleResponse.getMsg());
                return AppResponse.error(
                        ErrorCodeEnum.E_API_EXCEPTION,
                        "역할실패: " + (updateRoleResponse.getMsg() != null ? updateRoleResponse.getMsg() : "지원하지 않는오류"));
            }

            log.debug("역할성공, id: {}, name: {}", roleId, casdoorRole.displayName);
            return AppResponse.success("역할성공");
        } catch (IOException e) {
            log.error("역할실패(IO예외)", e);
            return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "역할실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("역할예외", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "역할예외: " + e.getMessage());
        }
    }

    /**
     * 삭제역할
     * @param deleteCommonDto 삭제역할DTO
     * @param request HTTP요청 
     * @return 삭제결과
     */
    @Override
    public AppResponse<String> deleteRole(DeleteCommonDto deleteCommonDto, HttpServletRequest request)
            throws IOException {
        try {
            log.debug("열기 삭제역할");

            // 매개변수검증
            if (deleteCommonDto == null || StringUtils.isBlank(deleteCommonDto.getId())) {
                log.warn("삭제역할실패: 역할ID비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "역할 ID는 비워 둘 수 없습니다");
            }

            String roleId = deleteCommonDto.getId().trim();
            log.debug("준비삭제역할, id: {}", roleId);

            // 조회역할여부저장에서
            org.casbin.casdoor.entity.Role existingRole = roleService.getRole(roleId);
            if (existingRole == null) {
                log.warn("삭제역할실패: 역할찾을 수 없습니다, id: {}", roleId);
                return AppResponse.error(ErrorCodeEnum.E_NO_ACCOUNT, "역할찾을 수 없습니다");
            }

            // 호출Casdoor삭제연결
            log.debug("호출Casdoor API삭제역할, name: {}", existingRole.name);
            CasdoorResponse<String, Object> deleteRoleResponse = roleService.deleteRole(existingRole);

            if (deleteRoleResponse == null) {
                log.error("삭제역할실패: Casdoor API반환비어 있습니다, id: {}", roleId);
                return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "삭제역할실패: API반환비어 있습니다");
            }

            if (deleteRoleResponse.getStatus() != null && !"ok".equals(deleteRoleResponse.getStatus())) {
                log.error(
                        "삭제역할실패: Casdoor API반환오류, id: {}, status: {}, msg: {}",
                        roleId,
                        deleteRoleResponse.getStatus(),
                        deleteRoleResponse.getMsg());
                return AppResponse.error(
                        ErrorCodeEnum.E_API_EXCEPTION,
                        "삭제역할실패: " + (deleteRoleResponse.getMsg() != null ? deleteRoleResponse.getMsg() : "지원하지 않는오류"));
            }

            log.debug("삭제역할성공, id: {}", roleId);
            return AppResponse.success("삭제역할성공");
        } catch (IOException e) {
            log.error("삭제역할실패(IO예외)", e);
            return AppResponse.error(ErrorCodeEnum.E_API_EXCEPTION, "삭제역할실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("삭제역할예외", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "삭제역할예외: " + e.getMessage());
        }
    }

    /**
     * 근거이름조회역할
     * @param listRoleDto 조회파일
     * @param request HTTP요청 
     * @return 분결과
     */
    @Override
    public AppResponse<PageDto<Role>> searchRole(ListRoleDto listRoleDto, HttpServletRequest request) {
        try {
            // 매개변수검증
            if (listRoleDto == null
                    || listRoleDto.getRoleName() == null
                    || listRoleDto.getRoleName().trim().isEmpty()) {
                log.warn("근거이름조회역할실패: 역할이름비어 있습니다");
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "역할 이름은 비워 둘 수 없습니다");
            }

            String keyword = listRoleDto.getRoleName().trim();
            log.debug("열기 근거이름조회역할, keyword: {}", keyword);

            // 호출DAO조회Casdoor역할목록(아니요분, 다중1000)
            String currentTenantOwner = getCurrentTenantOwner(request);
            List<org.casbin.casdoor.entity.Role> casdoorRoles =
                    casdoorRoleDao.searchRoleByName(keyword, currentTenantOwner, databaseName);

            if (casdoorRoles == null || casdoorRoles.isEmpty()) {
                log.debug("근거이름조회역할결과비어 있습니다, keyword: {}", keyword);
                PageDto<Role> emptyPage = new PageDto<>();
                emptyPage.setResult(Collections.emptyList());
                emptyPage.setCurrentPageNo(listRoleDto.getPageNum());
                emptyPage.setPageSize(listRoleDto.getPageSize());
                emptyPage.setTotalCount(0L);
                return AppResponse.success(emptyPage);
            }

            log.debug("근거이름조회까지 {} 개Casdoor역할, keyword: {}", casdoorRoles.size(), keyword);

            // Casdoor Role 변환통신사용 Role
            List<Role> allRoles = casdoorRoles.stream()
                    .filter(role -> role != null)
                    .map(role -> {
                        try {
                            return casdoorRoleMapper.toCommonRole(role);
                        } catch (Exception e) {
                            log.warn("역할정보변환실패, roleName: {}", role != null ? role.name : "null", e);
                            return null;
                        }
                    })
                    .filter(role -> role != null)
                    .collect(Collectors.toList());

            log.debug("성공변환 {} 개역할로통신사용역할객체, keyword: {}", allRoles.size(), keyword);

            // Java 분
            int pageNum = listRoleDto.getPageNum() == null ? 1 : listRoleDto.getPageNum();
            int pageSize = listRoleDto.getPageSize() == null ? 10 : listRoleDto.getPageSize();
            if (pageNum < 1) {
                pageNum = 1;
            }
            if (pageSize <= 0) {
                pageSize = 10;
            }

            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, allRoles.size());

            List<Role> pageResult;
            if (fromIndex >= allRoles.size()) {
                pageResult = Collections.emptyList();
            } else {
                pageResult = allRoles.subList(fromIndex, toIndex);
            }

            PageDto<Role> pageDto = new PageDto<>();
            pageDto.setResult(pageResult);
            pageDto.setCurrentPageNo(pageNum);
            pageDto.setPageSize(pageSize);
            pageDto.setTotalCount((long) allRoles.size());

            log.debug(
                    "근거이름조회역할성공, keyword: {}, 데이터: {}, 현재: {}, 매: {}, 현재수: {}",
                    keyword,
                    allRoles.size(),
                    pageNum,
                    pageSize,
                    pageResult.size());

            return AppResponse.success(pageDto);
        } catch (Exception e) {
            log.error("근거이름조회역할예외", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "근거이름조회역할예외: " + e.getMessage());
        }
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
}