package com.iflytek.rpa.auth.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.iflytek.rpa.auth.core.entity.*;
import com.iflytek.rpa.auth.utils.AppResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 * 모듈서비스
 */
public interface DeptService {

    /**
     * 조회모듈, 사람데이터, 사람
     * @param request HTTP요청 
     * @return 모듈및사람원정보
     */
    AppResponse<?> treeAndPerson(HttpServletRequest request);

    /**
     * 버전의모듈및사람원조회(격식제한제어)
     * 패키지으로아래: 
     * 1. 반환필요필드(name, userNum, userName, id, orgId, pid)
     * 2. 사용Redis저장, 저장시간1시간
     * 3. 격식제한제어반환단계모듈및단계모듈(결과)
     * 4. 행조회
     *
     * @param request HTTP요청 
     * @return 후의결과, 다중패키지모듈결과
     */
    AppResponse<java.util.Map<String, Object>> treeAndPersonOptimized(HttpServletRequest request);

    /**
     * 추가모듈
     * @param createUapOrgDto 생성모듈DTO
     * @param request HTTP요청 
     * @return 결과
     */
    AppResponse<String> addDept(CreateUapOrgDto createUapOrgDto, HttpServletRequest request);

    //    PageDto<Org> queryOrgPageList(String tenantId, OrgListDto orgListDto, HttpServletRequest request);

    /**
     * 가져오기모듈 todo 반환있음권한의
     * @param request HTTP요청 
     * @return 모듈
     * @throws Exception 예외
     */
    AppResponse<TreeNode> queryTreeList(HttpServletRequest request) throws Exception;

    /**
     * 통신경과모듈의id조회모든모듈
     * @param dto 조회매개변수
     * @param request HTTP요청 
     * @return 모듈목록
     * @throws Exception 예외
     */
    AppResponse<List<DeptTreeNodeVo>> queryDeptTreeByPid(QueryDeptNodeDto dto, HttpServletRequest request)
            throws Exception;

    /**
     * 모듈
     * @param editOrgDto 모듈DTO
     * @param request HTTP요청 
     * @return 결과
     */
    AppResponse<String> editDept(EditOrgDto editOrgDto, HttpServletRequest request);

    /**
     * 삭제모듈
     * @param deleteCommonDto 삭제모듈DTO
     * @param request HTTP요청 
     * @return 삭제결과
     */
    AppResponse<String> deleteDept(DeleteCommonDto deleteCommonDto, HttpServletRequest request);

    /**
     * 통신경과deptId조회모듈이름
     * @param dto 조회매개변수
     * @param request HTTP요청 
     * @return 모듈이름
     */
    AppResponse<DeptNameVo> queryDeptNameByDeptId(QueryDeptIdDto dto, HttpServletRequest request);

    /**
     * 가져오기테넌트이름
     * @param request HTTP요청 
     * @return 테넌트이름
     */
    AppResponse<String> queryTenantName(HttpServletRequest request);

    /**
     * 모듈사람데이터정보조회
     * @param dto 조회매개변수
     * @param request HTTP요청 
     * @return 모듈사람데이터목록
     * @throws JsonProcessingException JSON관리예외
     */
    AppResponse<List<DeptPersonTreeNodeVo>> queryDeptPersonNodeByPid(QueryDeptNodeDto dto, HttpServletRequest request)
            throws JsonProcessingException;

    /**
     * 조회현재기기의모든사용자
     * @param dto 조회매개변수
     * @param request HTTP요청 
     * @return 사용자목록
     * @throws Exception 예외
     */
    AppResponse<List<UserVo>> queryAllUserByDeptId(QueryDeptIdDto dto, HttpServletRequest request) throws Exception;

    /**
     * 가져오기현재로그인사용자의모듈levelCode, deptIdPath
     * @param request HTTP요청 
     * @return 모듈levelCode
     */
    AppResponse<String> getCurrentLevelCode(HttpServletRequest request);

    /**
     * 가져오기현재로그인사용자의모듈ID
     * @param request HTTP요청 
     * @return 모듈ID
     */
    AppResponse<String> getCurrentDeptId(HttpServletRequest request);

    /**
     * 가져오기현재로그인사용자의모듈정보
     * @param request HTTP요청 
     * @return 모듈정보
     */
    AppResponse<Org> getCurrentDeptInfo(HttpServletRequest request);

    /**
     * 근거모듈ID조회모듈정보
     * @param id 모듈ID
     * @param request HTTP요청 
     * @return 모듈정보
     */
    AppResponse<Org> getDeptInfoByDeptId(String id, HttpServletRequest request);

    /**
     * 조회모듈ID의levelCode
     * @param id 모듈ID
     * @param request HTTP요청 
     * @return levelCode
     */
    AppResponse<String> getLevelCodeByDeptId(String id, HttpServletRequest request);

    /**
     * 조회지정기기모든기기의사용자수
     * @param id 모듈ID
     * @param request HTTP요청 
     * @return 사용자수
     */
    AppResponse<Long> getUserNumByDeptId(String id, HttpServletRequest request);

    /**
     * 근거모듈ID목록가져오기모듈정보목록
     * @param orgIdList 모듈ID목록
     * @param request HTTP요청 
     * @return 모듈정보목록
     */
    AppResponse<List<Org>> queryOrgListByIds(List<String> orgIdList, HttpServletRequest request);

    /**
     * 근거사용자ID가져오기모듈ID
     * @param userId 사용자ID
     * @param tenantId 테넌트ID
     * @param request HTTP요청 
     * @return 모듈ID
     */
    AppResponse<String> getDeptIdByUserId(String userId, String tenantId, HttpServletRequest request);

    /**
     * 조회데이터권한, 예일개모듈목록
     * @param request HTTP요청 
     * @return 데이터권한
     */
    AppResponse<DataAuthDetailDo> getDataAuthWithDeptList(HttpServletRequest request);
}