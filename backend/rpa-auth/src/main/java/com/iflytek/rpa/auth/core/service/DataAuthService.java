package com.iflytek.rpa.auth.core.service;

import com.iflytek.rpa.auth.core.entity.Authority;
import com.iflytek.rpa.auth.core.entity.BindRoleDataAuthDto;
import com.iflytek.rpa.auth.core.entity.DataAuthorityWithDimDictDto;
import com.iflytek.rpa.auth.utils.AppResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 * 데이터권한서비스
 */
public interface DataAuthService {

    /**
     * 조회선택의데이터권한
     * @param roleId 역할ID
     * @param request HTTP요청 
     * @return 데이터권한목록
     */
    AppResponse<List<DataAuthorityWithDimDictDto>> getCheckedDataAuth(String roleId, HttpServletRequest request);

    /**
     * 역할지정데이터권한
     * @param bindRoleDataAuthDto 지정역할데이터권한DTO
     * @param request HTTP요청 
     * @return 결과
     */
    AppResponse<String> bindDataAuth(BindRoleDataAuthDto bindRoleDataAuthDto, HttpServletRequest request);

    /**
     * 근거역할ID조회권한목록
     * @param tenantId 테넌트ID
     * @param roleId 역할ID
     * @param request HTTP요청 
     * @return 권한목록
     */
    AppResponse<List<Authority>> getAuthorityListByRoleId(String tenantId, String roleId, HttpServletRequest request);
}