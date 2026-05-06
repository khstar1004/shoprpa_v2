package com.iflytek.rpa.auth.core.service;

import com.iflytek.rpa.auth.core.entity.*;
import com.iflytek.rpa.auth.utils.AppResponse;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 * 역할서비스연결
 */
public interface RoleService {

    /**
     * 조회사용내부전체역할목록
     *
     * @param request HTTP요청 
     * @return 역할목록
     */
    AppResponse<List<Role>> getUserRoleListInApp(HttpServletRequest request) throws IOException;

    /**
     * 조회현재사용자역할목록
     *
     * @param request HTTP요청 
     * @return 역할목록
     */
    AppResponse<List<Role>> getUserRoleList(HttpServletRequest request) throws IOException;

    /**
     * 조회역할
     *
     * @param dto     조회매개변수
     * @param request HTTP요청 
     * @return 역할
     */
    AppResponse<Role> queryRoleDetail(GetRoleDto dto, HttpServletRequest request) throws IOException;

    /**
     * 추가역할
     *
     * @param createRoleDto 추가역할DTO
     * @param request       HTTP요청 
     * @return 결과
     */
    AppResponse<String> addRole(CreateRoleDto createRoleDto, HttpServletRequest request) throws IOException;

    /**
     * 역할
     *
     * @param updateRoleDto 업데이트역할DTO
     * @param request       HTTP요청 
     * @return 결과
     */
    AppResponse<String> updateRole(UpdateRoleDto updateRoleDto, HttpServletRequest request) throws IOException;

    /**
     * 삭제역할
     *
     * @param deleteCommonDto 삭제역할DTO
     * @param request         HTTP요청 
     * @return 삭제결과
     */
    AppResponse<String> deleteRole(DeleteCommonDto deleteCommonDto, HttpServletRequest request) throws IOException;

    /**
     * 근거이름조회역할
     *
     * @param listRoleDto 조회파일
     * @param request     HTTP요청 
     * @return 분결과
     */
    AppResponse<PageDto<Role>> searchRole(ListRoleDto listRoleDto, HttpServletRequest request);
}