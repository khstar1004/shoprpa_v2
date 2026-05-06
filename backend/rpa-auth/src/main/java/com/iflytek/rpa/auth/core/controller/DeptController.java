package com.iflytek.rpa.auth.core.controller;

import com.iflytek.rpa.auth.core.entity.*;
import com.iflytek.rpa.auth.core.service.DeptService;
import com.iflytek.rpa.auth.utils.AppResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 모듈
 */
@RestController
@RequestMapping("/dept")
@Slf4j
public class DeptController {

    @Autowired
    private DeptService deptService;

    /**
     * 가져오기모듈 todo 반환있음권한의
     * @param
     * @param request
     * @return
     */
    @GetMapping("/queryTreeList")
    public AppResponse<TreeNode> queryTreeList(HttpServletRequest request) throws Exception {
        return deptService.queryTreeList(request);
    }

    /**
     * 통신경과모듈의id조회모든모듈
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("queryDeptNodeByPid")
    public AppResponse<List<DeptTreeNodeVo>> queryDeptTreeByPid(
            @RequestBody QueryDeptNodeDto dto, HttpServletRequest request) throws Exception {
        return deptService.queryDeptTreeByPid(dto, request);
    }

    /**
     * 가져오기테넌트이름 (내용사용)
     * @param request
     * @return
     * @throws Exception
     */
    @GetMapping("queryTenantName")
    public AppResponse<String> queryTenantName(HttpServletRequest request) throws Exception {
        return deptService.queryTenantName(request);
    }
    /**
     * 통신경과deptId조회모듈이름
     */
    @PostMapping("queryDeptNameByDeptId")
    public AppResponse<DeptNameVo> queryDeptNameByDeptId(@RequestBody QueryDeptIdDto dto, HttpServletRequest request)
            throws Exception {
        return deptService.queryDeptNameByDeptId(dto, request);
    }

    /**
     * 추가모듈
     * @param
     * @param request
     * @return
     */
    @PostMapping("/add")
    public AppResponse<String> addDept(@RequestBody CreateUapOrgDto createUapOrgDto, HttpServletRequest request) {
        return deptService.addDept(createUapOrgDto, request);
    }

    /**
     * 모듈
     * @param
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public AppResponse<String> editDept(@RequestBody EditOrgDto editOrgDto, HttpServletRequest request) {
        return deptService.editDept(editOrgDto, request);
    }

    /**
     * 삭제모듈
     * @param
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public AppResponse<String> deleteDept(@RequestBody DeleteCommonDto deleteCommonDto, HttpServletRequest request) {
        return deptService.deleteDept(deleteCommonDto, request);
    }

    /**
     * 조회모듈, 사람데이터, 사람
     * @param
     * @param request
     * @return
     */
    @GetMapping("/treeAndPerson")
    public AppResponse<java.util.Map<String, Object>> treeAndPerson(HttpServletRequest request) {
        return deptService.treeAndPersonOptimized(request);
    }

    /**
     * 모듈사람데이터정보조회
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("queryDeptPersonNodeByPid")
    public AppResponse<List<DeptPersonTreeNodeVo>> queryDeptPersonNodeByPid(
            @RequestBody QueryDeptNodeDto dto, HttpServletRequest request) throws Exception {

        return deptService.queryDeptPersonNodeByPid(dto, request);
    }

    /**
     * 조회현재기기의모든사용자
     * @param
     * @param request
     * @return
     */
    @PostMapping("/queryUserListByDeptId")
    public AppResponse<List<UserVo>> queryAllUserByDeptId(@RequestBody QueryDeptIdDto dto, HttpServletRequest request)
            throws Exception {
        return deptService.queryAllUserByDeptId(dto, request);
    }

    /**
     * 가져오기현재로그인사용자의모듈levelCode, deptIdPath
     * @param request HTTP요청 
     * @return 모듈levelCode
     */
    @GetMapping("/current/levelCode")
    public AppResponse<String> getCurrentLevelCode(HttpServletRequest request) {
        return deptService.getCurrentLevelCode(request);
    }

    /**
     * 가져오기현재로그인사용자의모듈ID
     * @param request HTTP요청 
     * @return 모듈ID
     */
    @GetMapping("/current/id")
    public AppResponse<String> getCurrentDeptId(HttpServletRequest request) {
        return deptService.getCurrentDeptId(request);
    }

    /**
     * 가져오기현재로그인사용자의모듈정보
     * @param request HTTP요청 
     * @return 모듈정보
     */
    @GetMapping("/current")
    public AppResponse<Org> getCurrentDeptInfo(HttpServletRequest request) {
        return deptService.getCurrentDeptInfo(request);
    }

    /**
     * 근거모듈ID조회모듈정보
     * @param id 모듈ID
     * @param request HTTP요청 
     * @return 모듈정보
     */
    @GetMapping("/info")
    public AppResponse<Org> getDeptInfoByDeptId(@RequestParam("id") String id, HttpServletRequest request) {
        return deptService.getDeptInfoByDeptId(id, request);
    }

    /**
     * 조회모듈ID의levelCode
     * @param id 모듈ID
     * @param request HTTP요청 
     * @return levelCode
     */
    @GetMapping("/levelCode")
    public AppResponse<String> getLevelCodeByDeptId(@RequestParam("id") String id, HttpServletRequest request) {
        return deptService.getLevelCodeByDeptId(id, request);
    }

    /**
     * 조회지정기기모든기기의사용자수
     * @param id 모듈ID
     * @param request HTTP요청 
     * @return 사용자수
     */
    @GetMapping("/userNum")
    public AppResponse<Long> getUserNumByDeptId(@RequestParam("id") String id, HttpServletRequest request) {
        return deptService.getUserNumByDeptId(id, request);
    }

    /**
     * 근거모듈ID목록가져오기모듈정보목록
     * @param orgIdList 모듈ID목록
     * @param request HTTP요청 
     * @return 모듈정보목록
     */
    @PostMapping("/queryByIds")
    public AppResponse<List<Org>> queryOrgListByIds(@RequestBody List<String> orgIdList, HttpServletRequest request) {
        return deptService.queryOrgListByIds(orgIdList, request);
    }

    /**
     * 근거사용자ID가져오기모듈ID
     * @param userId 사용자ID
     * @param tenantId 테넌트ID
     * @param request HTTP요청 
     * @return 모듈ID
     */
    @GetMapping("/user/deptId")
    public AppResponse<String> getDeptIdByUserId(
            @RequestParam("userId") String userId,
            @RequestParam("tenantId") String tenantId,
            HttpServletRequest request) {
        return deptService.getDeptIdByUserId(userId, tenantId, request);
    }

    /**
     * 조회데이터권한, 예일개모듈목록
     * @param request HTTP요청 
     * @return 데이터권한
     */
    @GetMapping("/dataAuth")
    public AppResponse<DataAuthDetailDo> getDataAuthWithDeptList(HttpServletRequest request) {
        return deptService.getDataAuthWithDeptList(request);
    }
}