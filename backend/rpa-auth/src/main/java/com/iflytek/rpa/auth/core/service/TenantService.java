package com.iflytek.rpa.auth.core.service;

import com.iflytek.rpa.auth.core.entity.*;
import com.iflytek.rpa.auth.utils.AppResponse;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 * 테넌트서비스
 */
public interface TenantService {

    /**
     * 테넌트아래모든사용자
     * @param userName
     * @return
     * @throws Exception
     */
    AppResponse<List<UserVo>> getAllUser(String userName) throws Exception;

    /**
     * 현재로그인사용자에서사용의테넌트목록
     * @param request
     * @return
     */
    AppResponse<List<Tenant>> getTenantListInApp(HttpServletRequest request);

    /**
     * 정보조회
     * @param request
     * @return
     * @throws Exception
     */
    AppResponse<TenantInfoDto> getTenantInfo(HttpServletRequest request) throws Exception;

    /**
     * 가져오기테넌트ID
     * @param request
     * @return
     */
    AppResponse<String> getTenantId(HttpServletRequest request);

    /**
     * 가져오기현재로그인의테넌트ID
     * @param request HTTP요청 
     * @return 현재로그인의테넌트ID
     */
    AppResponse<String> getCurrentTenantId(HttpServletRequest request);

    /**
     * 가져오기현재로그인의테넌트이름
     * @param request HTTP요청 
     * @return 현재로그인의테넌트이름
     */
    AppResponse<String> getCurrentTenantName(HttpServletRequest request);

    /**
     * 근거테넌트ID조회테넌트 정보
     * @param tenantId 테넌트ID
     * @param request HTTP요청 
     * @return 테넌트 정보
     */
    AppResponse<Tenant> queryTenantInfoById(String tenantId, HttpServletRequest request) throws IOException;

    /**
     * 변경수정관리관리원(지원하지 않음)
     * @param id
     * @param request
     * @return
     */
    AppResponse<String> changeManager(String id, HttpServletRequest request);

    /**
     * 근거테넌트id가져오기모든조직목록
     * @param tenantId
     * @param request
     * @return
     */
    AppResponse<List<Org>> getAllOrgList(String tenantId, HttpServletRequest request) throws IOException;

    /**
     * 테넌트
     * @param tenantId 테넌트ID
     * @param request HTTP요청 
     * @return 결과
     */
    AppResponse<String> switchTenant(String tenantId, HttpServletRequest request);

    /**
     * 근거휴대폰 번호조회사용자의테넌트목록
     * @param phoneOrLoginName 휴대폰 번호또는로그인이름
     * @param request HTTP요청 
     * @return 테넌트목록
     */
    AppResponse<List<Tenant>> getTenantList(String phoneOrLoginName, HttpServletRequest request);

    /**
     * 가져오기모든테넌트ID
     * @return 모든테넌트ID목록
     */
    AppResponse<List<String>> getAllTenantId();

    /**
     * 가져오기테넌트관리관리원ID목록
     * @param tenantId 테넌트ID
     * @return 테넌트관리관리원ID목록
     */
    AppResponse<List<String>> getTenantManagerIds(String tenantId);

    /**
     * 가져오기테넌트통신사용자ID목록
     * @param tenantId 테넌트ID
     * @return 테넌트통신사용자ID목록
     */
    AppResponse<List<String>> getTenantNormalUserIds(String tenantId);

    AppResponse<List<String>> getNoClassifyTenantIds();

    AppResponse<Integer> updateTenantClassifyCompleted(List<String> ids);

    /**
     * 가져오기모든테넌트ID목록(테넌트코드으로ep_또는es_열기 )
     * @return 테넌트ID목록
     */
    AppResponse<List<String>> getAllEnterpriseTenantId();

    /**
     * 가져오기테넌트사용자유형(1테이블테넌트관리관리원, 테이블통신사용자)
     * @param userId 사용자ID
     * @param tenantId 테넌트ID
     * @return 테넌트사용자유형(가능로null)
     */
    AppResponse<Integer> getTenantUserType(String userId, String tenantId);

    /**
     * 조회테넌트까지정보
     * 통신경과token가져오기테넌트, 반환까지시간, 시간(), 여부까지, 여부안내까지
     * @param request HTTP요청 
     * @return 테넌트까지정보
     */
    AppResponse<TenantExpirationDto> getTenantExpiration(HttpServletRequest request);

    /**
     * 조회테넌트빈여부까지
     * @param request HTTP요청 
     * @return true테이블빈완료까지, false테이블빈찾을 수 없는 
     */
    boolean checkSpaceExpired(HttpServletRequest request);

    /**
     * 로테넌트까지정보
     * @param tenant 테넌트객체
     */
    void fillTenantExpirationInfo(Tenant tenant);
}