package com.iflytek.rpa.auth.core.controller;

import com.iflytek.rpa.auth.auditRecord.constants.AuditLog;
import com.iflytek.rpa.auth.core.entity.Authority;
import com.iflytek.rpa.auth.core.entity.BindRoleDataAuthDto;
import com.iflytek.rpa.auth.core.entity.DataAuthorityWithDimDictDto;
import com.iflytek.rpa.auth.core.service.DataAuthService;
import com.iflytek.rpa.auth.utils.AppResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 데이터권한
 */
@Slf4j
@RestController
@RequestMapping("/dataAuth")
public class DataAuthController {

    @Autowired
    private DataAuthService dataAuthService;

    /**
     * 조회선택의데이터권한
     * @param roleId 역할ID
     * @param request HTTP요청 
     * @return 데이터권한목록
     */
    @GetMapping("/queryCheckedDataAuth")
    public AppResponse<List<DataAuthorityWithDimDictDto>> getCheckedDataAuth(
            @RequestParam("roleId") String roleId, HttpServletRequest request) {
        return dataAuthService.getCheckedDataAuth(roleId, request);
    }

    /**
     * 역할지정데이터권한
     * @param bindRoleDataAuthDto 지정역할데이터권한DTO
     * @param request HTTP요청 
     * @return 결과
     */
    @PostMapping("/bindDataAuth")
    @AuditLog(moduleName = "관리자 권한", typeName = "권한")
    public AppResponse<String> bindDataAuth(
            @RequestBody BindRoleDataAuthDto bindRoleDataAuthDto, HttpServletRequest request) {
        return dataAuthService.bindDataAuth(bindRoleDataAuthDto, request);
    }

    /**
     * 근거역할ID조회권한목록
     * @param tenantId 테넌트ID
     * @param roleId 역할ID
     * @param request HTTP요청 
     * @return 권한목록
     */
    @GetMapping("/getAuthorityListByRoleId")
    public AppResponse<List<Authority>> getAuthorityListByRoleId(
            @RequestParam("tenantId") String tenantId,
            @RequestParam("roleId") String roleId,
            HttpServletRequest request) {
        return dataAuthService.getAuthorityListByRoleId(tenantId, roleId, request);
    }
}