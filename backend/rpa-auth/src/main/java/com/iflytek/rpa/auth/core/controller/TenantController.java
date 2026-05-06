package com.iflytek.rpa.auth.core.controller;

import com.iflytek.rpa.auth.core.entity.*;
import com.iflytek.rpa.auth.core.service.TenantService;
import com.iflytek.rpa.auth.utils.AppResponse;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 테넌트
 */
@RestController
@RequestMapping("/tenant")
public class TenantController {

    @Autowired
    TenantService tenantService;

    /**
     * 현재로그인사용자에서사용의테넌트목록
     * @param request
     * @return
     */
    @GetMapping("/getTenantListInApp")
    public AppResponse<List<Tenant>> getTenantListInApp(HttpServletRequest request) {
        return tenantService.getTenantListInApp(request);
    }

    /**
     * 정보조회
     * @param request
     * @return
     */
    @GetMapping("/getTenantInfo")
    public AppResponse<TenantInfoDto> getTenantInfo(HttpServletRequest request) throws Exception {
        return tenantService.getTenantInfo(request);
    }

    /**
     * 가져오기테넌트ID
     * @param request
     * @return
     */
    @GetMapping("/getTenantId")
    public AppResponse<String> getTenantId(HttpServletRequest request) {
        return tenantService.getTenantId(request);
    }

    /**
     * 변경수정관리관리원(지원하지 않음)
     * @param request
     * @return
     */
    @GetMapping("/changeManager")
    public AppResponse<String> changeManager(@RequestParam("id") String id, HttpServletRequest request) {
        return tenantService.changeManager(id, request);
    }

    @PostMapping("/all-user")
    public AppResponse<List<UserVo>> getAllUser(@RequestParam String userName) throws Exception {
        return tenantService.getAllUser(userName);
    }

    /**
     * 근거테넌트id가져오기모든조직목록
     * @param tenantId
     * @param request
     * @return
     */
    @GetMapping("/getAllOrgList")
    public AppResponse<List<Org>> getAllOrgList(@RequestParam("tenantId") String tenantId, HttpServletRequest request)
            throws IOException {
        return tenantService.getAllOrgList(tenantId, request);
    }

    /**
     * 가져오기현재로그인의테넌트ID
     * @param request HTTP요청 
     * @return 현재로그인의테넌트ID
     */
    @GetMapping("/current/id")
    public AppResponse<String> getCurrentTenantId(HttpServletRequest request) {
        return tenantService.getCurrentTenantId(request);
    }

    /**
     * 가져오기현재로그인의테넌트이름
     * @param request HTTP요청 
     * @return 현재로그인의테넌트이름
     */
    @GetMapping("/current/name")
    public AppResponse<String> getCurrentTenantName(HttpServletRequest request) {
        return tenantService.getCurrentTenantName(request);
    }

    /**
     * 근거테넌트ID조회테넌트 정보
     * @param tenantId 테넌트ID
     * @param request HTTP요청 
     * @return 테넌트 정보
     */
    @GetMapping("/info")
    public AppResponse<Tenant> queryTenantInfoById(
            @RequestParam("tenantId") String tenantId, HttpServletRequest request) throws IOException {
        return tenantService.queryTenantInfoById(tenantId, request);
    }

    /**
     * 테넌트
     * @param tenantId 테넌트id
     * @param request HTTP요청 
     * @return 결과
     */
    @PostMapping("/switch")
    public AppResponse<String> switchTenant(@RequestParam("tenantId") String tenantId, HttpServletRequest request) {
        return tenantService.switchTenant(tenantId, request);
    }

    /**
     * 가져오기모든테넌트ID
     * @return 모든테넌트ID목록
     */
    @GetMapping("/getAllTenantId")
    public AppResponse<List<String>> getAllTenantId() {
        return tenantService.getAllTenantId();
    }

    /**
     * 가져오기테넌트관리관리원ID목록
     * @param tenantId 테넌트ID
     * @return 테넌트관리관리원ID목록
     */
    @GetMapping("/getTenantManagerIds")
    public AppResponse<List<String>> getTenantManagerIds(@RequestParam("tenantId") String tenantId) {
        return tenantService.getTenantManagerIds(tenantId);
    }

    /**
     * 가져오기테넌트통신사용자ID목록
     * @param tenantId 테넌트ID
     * @return 테넌트통신사용자ID목록
     */
    @GetMapping("/getTenantNormalUserIds")
    public AppResponse<List<String>> getTenantNormalUserIds(@RequestParam("tenantId") String tenantId) {
        return tenantService.getTenantNormalUserIds(tenantId);
    }

    @GetMapping("/getNoClassifyTenantIds")
    public AppResponse<List<String>> getNoClassifyTenantIds() {
        return tenantService.getNoClassifyTenantIds();
    }

    @PostMapping("/updateTenantClassifyCompleted")
    public AppResponse<Integer> updateTenantClassifyCompleted(@RequestBody List<String> ids) throws Exception {
        return tenantService.updateTenantClassifyCompleted(ids);
    }

    /**
     * 가져오기모든테넌트ID목록(테넌트코드으로ep_또는es_열기 )
     * @return 테넌트ID목록
     */
    @GetMapping("/getAllEnterpriseTenantId")
    public AppResponse<List<String>> getAllEnterpriseTenantId() {
        return tenantService.getAllEnterpriseTenantId();
    }

    /**
     * 가져오기테넌트사용자유형(1테이블테넌트관리관리원, 테이블통신사용자)
     * @param userId 사용자ID
     * @param tenantId 테넌트ID
     * @return 테넌트사용자유형(가능로null)
     */
    @GetMapping("/getTenantUserType")
    public AppResponse<Integer> getTenantUserType(
            @RequestParam("userId") String userId, @RequestParam("tenantId") String tenantId) {
        return tenantService.getTenantUserType(userId, tenantId);
    }

    /**
     * 조회테넌트까지정보
     * 통신경과token가져오기테넌트, 반환까지시간, 시간(), 여부까지, 여부안내까지
     * @param request HTTP요청 
     * @return 테넌트까지정보
     */
    @GetMapping("/expiration")
    public AppResponse<TenantExpirationDto> getTenantExpiration(HttpServletRequest request) {
        return tenantService.getTenantExpiration(request);
    }
}