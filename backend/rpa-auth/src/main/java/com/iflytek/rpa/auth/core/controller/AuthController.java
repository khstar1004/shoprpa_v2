package com.iflytek.rpa.auth.core.controller;

import com.iflytek.rpa.auth.core.entity.RoleAuthResourceDto;
import com.iflytek.rpa.auth.core.entity.TreeNode;
import com.iflytek.rpa.auth.core.service.AuthService;
import com.iflytek.rpa.auth.utils.AppResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 메뉴권한
 */
@Slf4j
@RestController
@RequestMapping("/menu")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 현재로그인사용자에서사용중의메뉴정보
     * @param request
     * @return
     */
    @GetMapping("/getUserAuthTreeInApp")
    public AppResponse<List<TreeNode>> getUserAuthTreeInApp(HttpServletRequest request) {

        return authService.getUserAuthTreeInApp(request);
    }

    /**
     * 조회메뉴, 권한
     * @param roleId
     * @param request
     * @return
     */
    @GetMapping("/getAuthResourceTreeInApp")
    public AppResponse<TreeNode> getAuthResourceTreeInApp(
            @RequestParam("roleId") String roleId, HttpServletRequest request) {
        return authService.getAuthResourceTreeInApp(roleId, request);
    }

    /**
     * 저장메뉴, 
     * @param
     * @return
     */
    @PostMapping("/save")
    public AppResponse<String> saveRoleAuth(
            @RequestBody RoleAuthResourceDto roleAuthResourceDto, HttpServletRequest request) {
        return authService.saveRoleAuth(roleAuthResourceDto, request);
    }
}