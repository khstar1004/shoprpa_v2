package com.iflytek.rpa.auth.core.service;

import com.iflytek.rpa.auth.core.entity.RoleAuthResourceDto;
import com.iflytek.rpa.auth.core.entity.TreeNode;
import com.iflytek.rpa.auth.utils.AppResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 * 권한서비스
 */
public interface AuthService {

    /**
     * 현재로그인사용자에서사용중의메뉴정보
     * @param request HTTP요청 
     * @return 메뉴목록
     */
    AppResponse<List<TreeNode>> getUserAuthTreeInApp(HttpServletRequest request);

    /**
     * 조회메뉴, 권한
     * @param roleId 역할ID
     * @param request HTTP요청 
     * @return 메뉴권한
     */
    AppResponse<TreeNode> getAuthResourceTreeInApp(String roleId, HttpServletRequest request);

    /**
     * 저장메뉴, 
     * @param roleAuthResourceDto 역할권한DTO
     * @param request HTTP요청 
     * @return 결과
     */
    AppResponse<String> saveRoleAuth(RoleAuthResourceDto roleAuthResourceDto, HttpServletRequest request);
}