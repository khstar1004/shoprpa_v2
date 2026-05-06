package com.iflytek.rpa.auth.core.controller;

import com.iflytek.rpa.auth.auditRecord.constants.AuditLog;
import com.iflytek.rpa.auth.core.entity.*;
import com.iflytek.rpa.auth.core.service.RoleService;
import com.iflytek.rpa.auth.utils.AppResponse;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 역할
 */
@RestController
@RequestMapping("/role")
@Slf4j
public class RoleController {

    @Autowired
    private RoleService roleService;

    /**
     * 조회사용내부전체역할목록
     *
     * @param request 요청 
     * @return 역할목록
     */
    @GetMapping("/getUserRoleListInApp")
    public AppResponse<List<Role>> queryTreeList(HttpServletRequest request) throws IOException {
        return roleService.getUserRoleListInApp(request);
    }

    /**
     * 조회역할목록
     *
     * @param request 요청 
     * @return 역할목록
     */
    @GetMapping("/getUserRoleList")
    public AppResponse<List<Role>> queryRoleList(HttpServletRequest request) throws IOException {
        return roleService.getUserRoleList(request);
    }

    /**
     * 조회역할
     *
     * @param dto     조회매개변수
     * @param request 요청 
     * @return 역할
     */
    @PostMapping("/queryDetail")
    public AppResponse<Role> queryRoleDetail(@RequestBody GetRoleDto dto, HttpServletRequest request)
            throws IOException {
        return roleService.queryRoleDetail(dto, request);
    }

    /**
     * 추가역할
     *
     * @param createRoleDto 추가역할DTO
     * @param request       요청 
     * @return 결과
     */
    @AuditLog(moduleName = "관리자 권한", typeName = "생성역할")
    @PostMapping("/add")
    public AppResponse<String> addRole(@RequestBody CreateRoleDto createRoleDto, HttpServletRequest request)
            throws IOException {
        return roleService.addRole(createRoleDto, request);
    }

    /**
     * 역할
     *
     * @param updateRoleDto 업데이트역할DTO
     * @param request       요청 
     * @return 결과
     */
    @PostMapping("/update")
    public AppResponse<String> updateRole(@RequestBody UpdateRoleDto updateRoleDto, HttpServletRequest request)
            throws IOException {
        return roleService.updateRole(updateRoleDto, request);
    }

    /**
     * 삭제역할
     *
     * @param deleteCommonDto 삭제역할DTO
     * @param request         HTTP요청 
     * @return 삭제결과
     */
    @AuditLog(moduleName = "관리자 권한", typeName = "삭제역할")
    @PostMapping("/delete")
    public AppResponse<String> deleteRole(@RequestBody DeleteCommonDto deleteCommonDto, HttpServletRequest request)
            throws IOException {
        return roleService.deleteRole(deleteCommonDto, request);
    }

    /**
     * 근거이름조회역할
     *
     * @param listRoleDto 조회파일
     * @param request     요청 
     * @return 분결과
     */
    @PostMapping("/search")
    public AppResponse<PageDto<Role>> searchRole(@RequestBody ListRoleDto listRoleDto, HttpServletRequest request) {
        return roleService.searchRole(listRoleDto, request);
    }
}