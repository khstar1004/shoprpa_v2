package com.iflytek.rpa.auth.sp.uap.service.impl;

import static com.iflytek.rpa.auth.sp.uap.constants.AuthConstant.ORG_TYPE_DEPT;
import static com.iflytek.rpa.auth.sp.uap.constants.RedisKeyConstant.*;
import static com.iflytek.rpa.auth.utils.RedisUtil.deleteRedisKeysByPrefix;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.rpa.auth.conf.condition.ConditionalOnSaaSOrUAP;
import com.iflytek.rpa.auth.core.entity.*;
import com.iflytek.rpa.auth.core.service.DeptService;
import com.iflytek.rpa.auth.sp.uap.dao.DeptDao;
import com.iflytek.rpa.auth.sp.uap.mapper.*;
import com.iflytek.rpa.auth.sp.uap.utils.DeptUtils;
import com.iflytek.rpa.auth.sp.uap.utils.TenantUtils;
import com.iflytek.rpa.auth.sp.uap.utils.UapManagementClientUtil;
import com.iflytek.rpa.auth.utils.AppResponse;
import com.iflytek.rpa.auth.utils.ErrorCodeEnum;
import com.iflytek.rpa.auth.utils.RedisUtils;
import com.iflytek.sec.uap.client.api.ClientManagementAPI;
import com.iflytek.sec.uap.client.api.UapUserInfoAPI;
import com.iflytek.sec.uap.client.core.client.ManagementClient;
import com.iflytek.sec.uap.client.core.dto.PageDto;
import com.iflytek.sec.uap.client.core.dto.ResponseDto;
import com.iflytek.sec.uap.client.core.dto.TreeNode;
import com.iflytek.sec.uap.client.core.dto.extand.UapExtendPropertyDto;
import com.iflytek.sec.uap.client.core.dto.org.GetOrgTreeDto;
import com.iflytek.sec.uap.client.core.dto.org.UapOrg;
import com.iflytek.sec.uap.client.core.dto.org.UpdateOrgDto;
import com.iflytek.sec.uap.client.core.dto.org.UpdateUapOrgDto;
import com.iflytek.sec.uap.client.core.dto.tenant.UapTenant;
import com.iflytek.sec.uap.client.core.dto.user.ListUserDto;
import com.iflytek.sec.uap.client.core.dto.user.UapUser;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * @author mjren
 * @date 2025-03-11 9:43
 * @copyright Copyright (c) 2025 mjren
 */
@Slf4j
@Service("deptService")
@ConditionalOnSaaSOrUAP
public class DeptServiceImpl implements DeptService {
    @Value("${uap.database.name:uap_db}")
    private String databaseName;

    @Autowired
    DeptDao deptDao;

    @Autowired
    private DeleteCommonDtoMapper deleteCommonDtoMapper;

    @Autowired
    private OrgMapper orgMapper;

    @Autowired
    private TreeNodeMapper treeNodeMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UpdateOrgDtoMapper updateOrgDtoMapper;

    @Autowired
    private UapExtendPropertyDtoMapper uapExtendPropertyDtoMapper;

    //    @Override
    //    public PageDto<UapOrg> queryOrgPageList(String tenantId, OrgListDto dto, HttpServletRequest request) {
    ////        ManagementClient managementClient = UapManagementClientUtil.getManagementClient(request);
    ////        ResponseDto<PageDto<UapOrg>> orgPageResponse = managementClient.queryOrgPageList(dto);
    //        ResponseDto<PageDto<UapOrg>> orgPageResponse = UapManagementClientUtil.queryOrgPageList(tenantId, dto,
    // request);
    //        if (!orgPageResponse.isFlag()) {
    //            log.error("queryOrgPageList error, msg:{}", orgPageResponse.getMessage());
    //            throw new ServiceException(orgPageResponse.getMessage());
    //        }
    //        return orgPageResponse.getData();
    //    }

    /**
     * 조회모듈, 사람데이터, 사람
     * @param request HTTP요청 
     * @return 모듈및사람원정보
     */
    @Override
    public AppResponse<?> treeAndPerson(HttpServletRequest request) {
        String tenantId = UapUserInfoAPI.getTenantId(request);
        UapUser uapUser = UapUserInfoAPI.getLoginUser(request);
        UapOrg uapOrg =
                ClientManagementAPI.queryOrgByLoginName(tenantId, null == uapUser ? null : uapUser.getLoginName());
        if (null == uapOrg) {
            log.info("treeAndPerson, 사용자 소속 부서 정보를 찾을 수 없습니다");
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "사용자 소속 부서 정보를 찾을 수 없습니다");
        }
        String firstLevelId = uapOrg.getFirstLevelId();
        // 근거id조회모듈정보
        List<UapOrg> firstDeptList =
                ClientManagementAPI.queryOrgListByOrgIds(tenantId, Collections.singletonList(firstLevelId));
        List<UapOrg> deptList = ClientManagementAPI.queryOrgListByParentOrgId(tenantId, firstLevelId);
        deptList.addAll(firstDeptList);
        Map<String, Long> deptPersonNumMap = new HashMap<>();
        List<String> deptUserIdList = new ArrayList<>();
        Map<String, UapUser> deptLeaderMap = new HashMap<>();
        // 조회매개모듈있음다중적음사람 다중
        //        분조회사용자본정보목록
        deptList.parallelStream().forEach(dept -> {
            ListUserDto listUserDto = new ListUserDto();
            listUserDto.setStatus(null);
            listUserDto.setOrgId(dept.getId());
            PageDto<UapUser> userListPage = ClientManagementAPI.queryUserPageList(tenantId, listUserDto);
            deptPersonNumMap.put(dept.getId(), userListPage.getTotalCount());

            // 가져오기모듈사람id
            String deptUserId = dept.getRemark();
            if (StringUtils.isNotBlank(deptUserId)) {
                deptUserIdList.add(deptUserId);
            }
        });

        // 조회매개모듈의사람이름
        ListUserDto listUserDto = new ListUserDto();
        listUserDto.setStatus(null);
        listUserDto.setUserIds(deptUserIdList);
        PageDto<UapUser> userListPage =
                ClientManagementAPI.queryUserPageList(UapUserInfoAPI.getTenantId(request), listUserDto);
        if (!CollectionUtils.isEmpty(userListPage.getResult())) {
            for (UapUser deptLeader : userListPage.getResult()) {
                deptLeaderMap.put(deptLeader.getId(), deptLeader);
            }
        }

        // 생성모듈, 를사람데이터및사람이름그룹설치까지모듈중
        List<DeptTreeNodeDto> treeNodeList = buildMenuTree("0", deptList, deptPersonNumMap, deptLeaderMap);

        // 조회테넌트이름
        UapTenant tenantInfo = UapUserInfoAPI.getTenant(request);
        if (null == tenantInfo) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "테넌트 정보가 없습니다");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("tenantName", tenantInfo.getName());
        result.put("deptTree", treeNodeList);
        return AppResponse.success(result);
    }

    /**
     * 버전의모듈및사람원조회
     * 패키지으로아래: 
     * 1. 반환필요필드(name, userNum, userName, id, orgId, pid)
     * 2. 사용Redis저장, 저장시간1시간
     * 3. 조회단계모듈및단계모듈
     * 4. 행조회
     *
     * @param request HTTP요청 
     * @return 후의결과
     */
    public AppResponse<Map<String, Object>> treeAndPersonOptimized(HttpServletRequest request) {
        String tenantId = UapUserInfoAPI.getTenantId(request);
        UapUser uapUser = UapUserInfoAPI.getLoginUser(request);

        // 생성저장key(제한제어버전)
        String cacheKey =
                "dept:tree:two-level:" + tenantId + ":" + (uapUser != null ? uapUser.getLoginName() : "anonymous");

        // 시도에서Redis가져오기 저장데이터
        try {
            Object cachedObj = RedisUtils.get(cacheKey);
            if (cachedObj != null) {
                log.info("에서Redis저장가져오기 모듈데이터: {}", cacheKey);
                ObjectMapper objectMapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> cachedResult = objectMapper.readValue(cachedObj.toString(), Map.class);
                return AppResponse.success(cachedResult);
            }
        } catch (Exception e) {
            log.warn("Redis저장가져오기실패, 계속조회데이터베이스: {}", e.getMessage());
        }

        UapOrg uapOrg =
                ClientManagementAPI.queryOrgByLoginName(tenantId, null == uapUser ? null : uapUser.getLoginName());
        if (null == uapOrg) {
            log.info("treeAndPersonOptimized, 사용자 소속 부서 정보를 찾을 수 없습니다");
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "사용자 소속 부서 정보를 찾을 수 없습니다");
        }

        String firstLevelId = uapOrg.getFirstLevelId();

        // 조회테넌트이름
        UapTenant tenantInfo = UapUserInfoAPI.getTenant(request);
        if (null == tenantInfo) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "테넌트 정보가 없습니다");
        }

        // 조회단계모듈및단계모듈, 적음수
        List<UapOrg> topLevelDepts =
                ClientManagementAPI.queryOrgListByOrgIds(tenantId, Collections.singletonList(firstLevelId));
        List<UapOrg> secondLevelDepts = ClientManagementAPI.queryOrgListByParentOrgId(tenantId, firstLevelId);

        // 병합단계및단계모듈
        List<UapOrg> deptList = new ArrayList<>();
        deptList.addAll(topLevelDepts);
        deptList.addAll(secondLevelDepts);

        // 의행조회: 조회사람데이터및사람정보
        Map<String, Long> deptPersonNumMap = new HashMap<>();
        List<String> deptUserIdList = Collections.synchronizedList(new ArrayList<>());
        Map<String, UapUser> deptLeaderMap = new HashMap<>();

        // 행조회매개모듈의사람데이터및사람ID
        deptList.parallelStream().forEach(dept -> {
            // 조회모듈사람데이터
            ListUserDto listUserDto = new ListUserDto();
            listUserDto.setStatus(null);
            listUserDto.setOrgId(dept.getId());
            PageDto<UapUser> userListPage = ClientManagementAPI.queryUserPageList(tenantId, listUserDto);

            synchronized (deptPersonNumMap) {
                deptPersonNumMap.put(dept.getId(), userListPage.getTotalCount());
            }

            // 모듈사람ID
            String deptUserId = dept.getRemark();
            if (StringUtils.isNotBlank(deptUserId)) {
                deptUserIdList.add(deptUserId);
            }
        });

        // 량조회모든모듈사람정보
        if (!deptUserIdList.isEmpty()) {
            ListUserDto listUserDto = new ListUserDto();
            listUserDto.setStatus(null);
            listUserDto.setUserIds(deptUserIdList);
            PageDto<UapUser> userListPage = ClientManagementAPI.queryUserPageList(tenantId, listUserDto);
            if (!CollectionUtils.isEmpty(userListPage.getResult())) {
                for (UapUser deptLeader : userListPage.getResult()) {
                    deptLeaderMap.put(deptLeader.getId(), deptLeader);
                }
            }
        }

        // 생성의모듈(제한제어로)
        List<SimpleDeptTreeNodeDto> treeNodeList =
                buildSimpleTwoLevelDeptTree(firstLevelId, deptList, deptPersonNumMap, deptLeaderMap);

        // 생성결과
        Map<String, Object> result = new HashMap<>();
        result.put("tenantName", tenantInfo.getName());
        result.put("deptTree", treeNodeList);

        // 를결과저장까지Redis, 저장1시간
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String resultJson = objectMapper.writeValueAsString(result);
            RedisUtils.set(cacheKey, resultJson, 3600); // 1시간저장
            log.info("모듈데이터완료저장까지Redis: {}", cacheKey);
        } catch (Exception e) {
            log.warn("Redis저장입력실패: {}", e.getMessage());
        }

        return AppResponse.success(result);
    }

    public List<DeptTreeNodeDto> buildMenuTree(
            String rootId,
            List<UapOrg> deptList,
            Map<String, Long> deptPersonNumMap,
            Map<String, UapUser> deptLeaderMap) {

        Map<String, DeptTreeNodeDto> nodeMap = new HashMap<>(deptList.size() * 2);
        List<DeptTreeNodeDto> allNodes = new ArrayList<>();
        // 일: 생성모든까지
        for (UapOrg dept : deptList) {
            DeptTreeNodeDto node = convertToTreeNode(dept, deptPersonNumMap, deptLeaderMap);
            allNodes.add(node);
            nodeMap.put(node.getId(), node);
        }
        // 이: 생성결과
        // 생성결과: 를까지아래
        List<DeptTreeNodeDto> rootNodes = new ArrayList<>();
        for (DeptTreeNodeDto node : allNodes) {
            String parentId = node.getPid();
            if (rootId.equals(parentId)) {
                // 결과가예(parentId로0), 직선연결추가입력목록
                rootNodes.add(node);
            } else {
                // 까지, 를현재추가입력의목록
                DeptTreeNodeDto parent = nodeMap.get(parentId);
                if (parent != null) {
                    parent.getNodes().add(node);
                }
            }
        }
        // 3. 매개의목록  sort 필드정렬
        // 비고: 모든, 있음패키지의실행정렬
        for (DeptTreeNodeDto node : allNodes) {
            List<DeptTreeNodeDto> children = node.getNodes();
            if (!CollectionUtil.isEmpty(children)) {
                // 사용 sort 필드상승순서정렬
                children.sort(Comparator.comparing(
                        DeptTreeNodeDto::getSort, Comparator.nullsFirst(Comparator.naturalOrder())));
            }
        }
        return rootNodes;
    }

    /**
     * 생성의모듈(단계모듈 + 단계모듈)
     */
    public List<SimpleDeptTreeNodeDto> buildSimpleTwoLevelDeptTree(
            String topLevelId,
            List<UapOrg> deptList,
            Map<String, Long> deptPersonNumMap,
            Map<String, UapUser> deptLeaderMap) {
        List<SimpleDeptTreeNodeDto> rootNodes = new ArrayList<>();

        // 까지단계모듈
        UapOrg topDept = deptList.stream()
                .filter(dept -> topLevelId.equals(dept.getId()))
                .findFirst()
                .orElse(null);

        if (topDept == null) {
            log.warn("찾을 수 없는 단계모듈: {}", topLevelId);
            return rootNodes;
        }

        // 생성단계모듈
        SimpleDeptTreeNodeDto topNode = convertToSimpleTreeNode(topDept, deptPersonNumMap, deptLeaderMap);

        // 까지모든단계모듈(모듈로topLevelId의모듈)
        List<SimpleDeptTreeNodeDto> secondLevelNodes = deptList.stream()
                .filter(dept -> topLevelId.equals(dept.getHigherOrg()))
                .map(dept -> convertToSimpleTreeNode(dept, deptPersonNumMap, deptLeaderMap))
                .sorted(Comparator.comparing(SimpleDeptTreeNodeDto::getName))
                .collect(Collectors.toList());

        // 를단계모듈까지단계모듈아래
        topNode.setNodes(secondLevelNodes);
        rootNodes.add(topNode);

        return rootNodes;
    }

    /**
     * 생성의모듈(패키지필요필드)
     */
    public List<SimpleDeptTreeNodeDto> buildSimpleDeptTree(
            String rootId,
            List<UapOrg> deptList,
            Map<String, Long> deptPersonNumMap,
            Map<String, UapUser> deptLeaderMap) {
        Map<String, SimpleDeptTreeNodeDto> nodeMap = new HashMap<>(deptList.size() * 2);
        List<SimpleDeptTreeNodeDto> allNodes = new ArrayList<>();

        // 일: 생성모든
        for (UapOrg dept : deptList) {
            SimpleDeptTreeNodeDto node = convertToSimpleTreeNode(dept, deptPersonNumMap, deptLeaderMap);
            allNodes.add(node);
            nodeMap.put(node.getId(), node);
        }

        // 이: 생성결과
        List<SimpleDeptTreeNodeDto> rootNodes = new ArrayList<>();
        for (SimpleDeptTreeNodeDto node : allNodes) {
            String parentId = node.getPid();
            if (rootId.equals(parentId)) {
                // 단계
                rootNodes.add(node);
            } else {
                // 까지, 를현재추가입력의목록
                SimpleDeptTreeNodeDto parent = nodeMap.get(parentId);
                if (parent != null) {
                    parent.getNodes().add(node);
                }
            }
        }

        // 매개의정렬필드정렬(결과가필요의, 관리)
        for (SimpleDeptTreeNodeDto node : allNodes) {
            List<SimpleDeptTreeNodeDto> children = node.getNodes();
            if (!CollectionUtil.isEmpty(children)) {
                // 단일이름정렬
                children.sort(Comparator.comparing(SimpleDeptTreeNodeDto::getName));
            }
        }

        return rootNodes;
    }

    /**
     * 를 UapOrg 변환로 SimpleDeptTreeNodeDto
     */
    private SimpleDeptTreeNodeDto convertToSimpleTreeNode(
            UapOrg dept, Map<String, Long> deptPersonNumMap, Map<String, UapUser> deptLeaderMap) {
        SimpleDeptTreeNodeDto node = new SimpleDeptTreeNodeDto();
        node.setId(dept.getId());
        node.setOrgId(dept.getId()); // orgId및id보관일
        node.setName(dept.getName());
        node.setPid(dept.getHigherOrg());
        node.setUserNum(deptPersonNumMap.getOrDefault(dept.getId(), 0L));

        // 모듈사람이름
        UapUser leaderInfo = deptLeaderMap.get(dept.getRemark());
        if (leaderInfo != null) {
            node.setUserName(leaderInfo.getName());
        }

        return node;
    }

    // 방법법: 를 UapAuthority 변환로 DeptTreeNodeDto
    private DeptTreeNodeDto convertToTreeNode(
            UapOrg dept, Map<String, Long> deptPersonNumMap, Map<String, UapUser> deptLeaderMap) {
        DeptTreeNodeDto node = new DeptTreeNodeDto();
        node.setId(dept.getId());
        node.setName(dept.getName());
        node.setPid(dept.getHigherOrg());
        node.setSort(dept.getSort());
        node.setUserNum(deptPersonNumMap.getOrDefault(dept.getId(), 0L));
        UapUser leaderInfo = deptLeaderMap.get(dept.getRemark());
        if (null != leaderInfo) {
            node.setUserId(leaderInfo.getId());
            node.setUserName(leaderInfo.getName());
        }
        return node;
    }

    /**
     * 추가모듈
     * @param createUapOrgDto 생성모듈DTO
     * @param request HTTP요청 
     * @return 결과
     */
    @Override
    public AppResponse<String> addDept(CreateUapOrgDto createUapOrgDto, HttpServletRequest request) {
        if (null == createUapOrgDto.getUapOrg()
                || StringUtils.isBlank(createUapOrgDto.getUapOrg().getName())
                || StringUtils.isBlank(createUapOrgDto.getUapOrg().getHigherOrg())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        createUapOrgDto.getUapOrg().setOrgType(ORG_TYPE_DEPT);
        ManagementClient managementClient = UapManagementClientUtil.getManagementClient(request);

        // 통신사용유형변환까지uap유형
        CreateUapOrgDtoMapper createUapOrgDtoMapper = new CreateUapOrgDtoMapper();
        com.iflytek.sec.uap.client.core.dto.org.CreateUapOrgDto uapCreateUapOrgDto =
                createUapOrgDtoMapper.toUapCreateUapOrgDto(createUapOrgDto);

        ResponseDto<String> addResponse = managementClient.addOrg(uapCreateUapOrgDto);
        if (!addResponse.isFlag()) {
            log.error("addOrg error, msg:{}", addResponse.getMessage());
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, addResponse.getMessage());
        }

        // 삭제모든으로"dept:"로전의Redis데이터
        deleteRedisKeysByPrefix(REDIS_KEY_DEPT_PREFIX);
        return AppResponse.success(addResponse.getData());
    }

    /**
     * 가져오기모듈 todo 반환있음권한의
     * @param request HTTP요청 
     * @return 모듈
     * @throws Exception 예외
     */
    @Override
    public AppResponse<com.iflytek.rpa.auth.core.entity.TreeNode> queryTreeList(HttpServletRequest request)
            throws Exception {
        String tenantId = UapUserInfoAPI.getTenantId(request);
        String key = REDIS_KEY_DEPT_PREFIX + tenantId;
        log.info("redis조회모듈정보[dept:tenantId]: " + key);
        Object cachedObj = RedisUtils.get(key);
        String cached = cachedObj != null ? cachedObj.toString() : null;
        if (StringUtils.isNotBlank(cached)) {
            ObjectMapper objectMapper = new ObjectMapper();
            TreeNode uapTreeNode = objectMapper.readValue(cached, TreeNode.class);
            // 를UAP의TreeNode변환로core의TreeNode
            com.iflytek.rpa.auth.core.entity.TreeNode treeNode = treeNodeMapper.fromUapTreeNode(uapTreeNode);
            return AppResponse.success(treeNode);
        }
        String firstLevelId = "0";
        GetOrgTreeDto getOrgTreeDto = new GetOrgTreeDto();
        getOrgTreeDto.setParentId(firstLevelId);
        ManagementClient managementClient = UapManagementClientUtil.getManagementClient(request);
        ResponseDto<TreeNode> responseDto = managementClient.queryOrgTree(getOrgTreeDto);
        if (!responseDto.isFlag()) {
            log.error("queryOrgTree error, msg:{}", responseDto.getMessage());
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "모듈조회실패");
        }
        TreeNode uapData = responseDto.getData();
        // 를UAP의TreeNode변환로core의TreeNode
        com.iflytek.rpa.auth.core.entity.TreeNode coreData = treeNodeMapper.fromUapTreeNode(uapData);
        ObjectMapper objectMapper = new ObjectMapper();
        // 저장시사용UAP의TreeNode(원인로가능코드에서사용)
        RedisUtils.set(key, objectMapper.writeValueAsString(uapData), 3600);
        return AppResponse.success(coreData);
    }

    /**
     * 통신경과모듈의id조회모든모듈
     * @param dto 조회매개변수
     * @param request HTTP요청 
     * @return 모듈목록
     * @throws Exception 예외
     */
    @Override
    public AppResponse<List<DeptTreeNodeVo>> queryDeptTreeByPid(QueryDeptNodeDto dto, HttpServletRequest request)
            throws Exception {
        String tenantId = UapUserInfoAPI.getTenantId(request);
        // 단계
        String pid = dto.getPid();
        // 를 deptTreeNodeVos 저장까지 redis
        String redisKey = REDIS_KEY_DEPT_CHILD_NODES_PREFIX + tenantId + ":" + pid;
        // 조회 redis 여부있음저장, 결과가있음이면직선연결에서저장중가져오기
        Object cachedDeptTree = RedisUtils.get(redisKey);
        if (cachedDeptTree != null && StringUtils.isNotBlank(cachedDeptTree.toString())) {
            ObjectMapper objectMapper = new ObjectMapper();
            List<DeptTreeNodeVo> cachedList = objectMapper.readValue(
                    cachedDeptTree.toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, DeptTreeNodeVo.class));
            // 반환저장데이터
            return AppResponse.success(cachedList);
        }
        if (pid.equals("1")) {
            pid = deptDao.queryByHigherDeptId("0", tenantId, databaseName);
            if (StringUtils.isBlank(pid)) {
                return AppResponse.success(new ArrayList<>());
            }
        }
        List<DeptTreeNodeVo> deptTreeNodeVos = deptDao.queryChildrenOrgList(pid, tenantId, databaseName);
        // hasNodes필드
        if (!CollectionUtil.isEmpty(deptTreeNodeVos)) {
            List<String> childrenIds =
                    deptTreeNodeVos.stream().map(DeptTreeNodeVo::getId).collect(Collectors.toList());
            List<String> deptIdsWithChildren = deptDao.queryDeptIdsWithChildren(childrenIds, tenantId, databaseName);
            Set<String> hasChildrenSet = new HashSet<>(deptIdsWithChildren);
            for (DeptTreeNodeVo deptNode : deptTreeNodeVos) {
                deptNode.setHasNodes(hasChildrenSet.contains(deptNode.getId()));
            }
        } else {
            for (DeptTreeNodeVo deptNode : deptTreeNodeVos) {
                deptNode.setHasNodes(false);
            }
        }
        RedisUtils.set(redisKey, new ObjectMapper().writeValueAsString(deptTreeNodeVos), 3600);
        return AppResponse.success(deptTreeNodeVos);
    }

    @Override
    public AppResponse<String> editDept(EditOrgDto editOrgDto, HttpServletRequest request) {
        String userId = editOrgDto.getUserId();
        com.iflytek.rpa.auth.core.entity.UpdateOrgDto updateOrgDto = editOrgDto.getUapOrg();
        if (null == updateOrgDto || StringUtils.isBlank(updateOrgDto.getId())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        // 업데이트모듈사람
        updateOrgDto.setRemark(userId);
        editOrgDto.setUapOrg(updateOrgDto);

        // 변환UpdateUapOrgDto로uap유형
        UpdateUapOrgDto updateUapOrgDto = new UpdateUapOrgDto();
        UpdateOrgDto uapUpdateOrgDto = updateOrgDtoMapper.toUapUpdateOrgDto(updateOrgDto);
        updateUapOrgDto.setUapOrg(uapUpdateOrgDto);
        List<UapExtendPropertyDto> uapExtendPropertyDtoList =
                uapExtendPropertyDtoMapper.toUapExtendPropertyDtoList(editOrgDto.getExtands());
        updateUapOrgDto.setExtands(uapExtendPropertyDtoList);

        ManagementClient managementClient = UapManagementClientUtil.getManagementClient(request);
        ResponseDto<String> editResponse = managementClient.updateOrg(updateUapOrgDto);
        if (!editResponse.isFlag()) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, editResponse.getMessage());
        }
        // 삭제모든으로"dept:"로전의Redis데이터
        deleteRedisKeysByPrefix(REDIS_KEY_DEPT_PREFIX);
        return AppResponse.success(editResponse.getData());
    }

    @Override
    public AppResponse<String> deleteDept(DeleteCommonDto deleteCommonDto, HttpServletRequest request) {
        if (StringUtils.isBlank(deleteCommonDto.getId())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        ManagementClient managementClient = UapManagementClientUtil.getManagementClient(request);

        com.iflytek.sec.uap.client.core.dto.DeleteCommonDto uapDeleteCommonDto =
                deleteCommonDtoMapper.toUapDeleteCommonDto(deleteCommonDto);
        ResponseDto<String> deleteResponse = managementClient.deleteOrg(uapDeleteCommonDto);
        if (!deleteResponse.isFlag()) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, deleteResponse.getMessage());
        }
        // 삭제모든으로"dept:"로전의Redis데이터
        deleteRedisKeysByPrefix(REDIS_KEY_DEPT_PREFIX);
        return AppResponse.success(deleteResponse.getData());
    }

    /**
     * 통신경과deptId조회모듈이름
     * @param dto 조회매개변수
     * @param request HTTP요청 
     * @return 모듈이름
     */
    @Override
    public AppResponse<DeptNameVo> queryDeptNameByDeptId(QueryDeptIdDto dto, HttpServletRequest request) {
        String deptId = dto.getDeptId();
        if (StringUtils.isBlank(deptId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        String tenantId = UapUserInfoAPI.getTenantId(request);
        String deptName = deptDao.queryDeptNameByDeptId(deptId, tenantId, databaseName);
        DeptNameVo deptNameVo = new DeptNameVo();
        deptNameVo.setName(deptName);
        return AppResponse.success(deptNameVo);
    }

    @Override
    public AppResponse<String> queryTenantName(HttpServletRequest request) {
        try {
            String tenantName = TenantUtils.getTenantName();
            if (StringUtils.isBlank(tenantName)) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기테넌트이름실패");
            }
            return AppResponse.success(tenantName);
        } catch (Exception e) {
            log.error("가져오기테넌트이름실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기테넌트이름실패: " + e.getMessage());
        }
    }

    /**
     * 모듈사람데이터정보조회
     * @param dto 조회매개변수
     * @param request HTTP요청 
     * @return 모듈사람데이터목록
     * @throws JsonProcessingException JSON관리예외
     */
    @Override
    public AppResponse<List<DeptPersonTreeNodeVo>> queryDeptPersonNodeByPid(
            QueryDeptNodeDto dto, HttpServletRequest request) throws JsonProcessingException {
        String tenantId = UapUserInfoAPI.getTenantId(request);
        // 단계
        String pid = dto.getPid();
        // 를 deptTreeNodeVos 저장까지 redis
        String redisKey = REDIS_KEY_DEPT_PERSON_CHILD_NODES_PREFIX + tenantId + ":" + pid;
        // 조회 redis 여부있음저장, 결과가있음이면직선연결에서저장중가져오기
        Object cachedDeptTree = RedisUtils.get(redisKey);
        if (cachedDeptTree != null && StringUtils.isNotBlank(cachedDeptTree.toString())) {
            ObjectMapper objectMapper = new ObjectMapper();
            List<DeptPersonTreeNodeVo> cachedList = objectMapper.readValue(
                    cachedDeptTree.toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, DeptPersonTreeNodeVo.class));
            // 반환저장데이터
            return AppResponse.success(cachedList);
        }

        if (pid.equals("1")) {
            pid = deptDao.queryByHigherDeptId("0", tenantId, databaseName);
            if (StringUtils.isBlank(pid)) {
                return AppResponse.success(new ArrayList<>());
            }
        }
        List<DeptTreeNodeVo> deptTreeNodeVos = deptDao.queryChildrenOrgList(pid, tenantId, databaseName);

        List<DeptPersonTreeNodeVo> deptPersonTreeNodeVos = new ArrayList<>();

        if (!CollectionUtil.isEmpty(deptTreeNodeVos)) {
            // hasNodes필드
            List<String> childrenIds =
                    deptTreeNodeVos.stream().map(DeptTreeNodeVo::getId).collect(Collectors.toList());
            List<String> deptIdsWithChildren = deptDao.queryDeptIdsWithChildren(childrenIds, tenantId, databaseName);
            Set<String> hasChildrenSet = new HashSet<>(deptIdsWithChildren);
            for (DeptTreeNodeVo deptNode : deptTreeNodeVos) {
                deptNode.setHasNodes(hasChildrenSet.contains(deptNode.getId()));
            }

            Map<String, DeptPersonInfoBo> deptIdToPersonInfo = packageDeptPersonNum(childrenIds, tenantId);

            // 그룹설치 DeptPersonTreeNodeVo 목록
            for (DeptTreeNodeVo deptNode : deptTreeNodeVos) {
                DeptPersonTreeNodeVo personNode = new DeptPersonTreeNodeVo();
                personNode.setId(deptNode.getId());
                personNode.setName(deptNode.getName());
                personNode.setPid(deptNode.getPid());
                personNode.setHasNodes(Boolean.TRUE.equals(deptNode.getHasNodes()));
                DeptPersonInfoBo info = deptIdToPersonInfo.get(deptNode.getId());
                personNode.setUserNum(info != null && info.getUserNum() != null ? info.getUserNum() : 0);
                personNode.setUserName(info != null ? info.getUserName() : null);
                deptPersonTreeNodeVos.add(personNode);
            }
        } else {
            for (DeptTreeNodeVo deptNode : deptTreeNodeVos) {
                deptNode.setHasNodes(false);
            }
        }
        RedisUtils.set(redisKey, new ObjectMapper().writeValueAsString(deptPersonTreeNodeVos), 3600);
        return AppResponse.success(deptPersonTreeNodeVos);
    }

    @NotNull
    private Map<String, DeptPersonInfoBo> packageDeptPersonNum(List<String> deptIds, String tenantId) {
        // 를사람데이터정보변환로map, key로모듈id
        Map<String, DeptPersonInfoBo> deptIdToPersonInfo = new HashMap<>();
        for (String deptId : deptIds) {
            List<String> matchedIds = deptDao.getMatchedIds(deptId, databaseName);
            //  모듈사람데이터
            List<DeptPersonInfoBo> deptPersonInfoBos = deptDao.queryUserNumByOrgIds(matchedIds, tenantId, databaseName);
            // 추가사람데이터
            Integer totalUserNum = deptPersonInfoBos.stream()
                    .filter(Objects::nonNull)
                    .mapToInt(info -> info.getUserNum() != null ? info.getUserNum() : 0)
                    .sum();
            DeptPersonInfoBo deptPersonInfoBo = new DeptPersonInfoBo();
            deptPersonInfoBo.setUserNum(totalUserNum);
            deptIdToPersonInfo.put(deptId, deptPersonInfoBo);
        }
        return deptIdToPersonInfo;
    }

    /**
     * 조회현재기기의모든사용자
     * @param dto 조회매개변수
     * @param request HTTP요청 
     * @return 사용자목록
     * @throws Exception 예외
     */
    @Override
    public AppResponse<List<UserVo>> queryAllUserByDeptId(QueryDeptIdDto dto, HttpServletRequest request)
            throws Exception {
        String deptId = dto.getDeptId();
        String name = dto.getName();
        if (StringUtils.isBlank(deptId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        String tenantId = UapUserInfoAPI.getTenantId(request);
        // Redis 저장 key
        String redisKey = REDIS_KEY_DEPT_ALL_USER_PREFIX + deptId + ":" + (name == null ? "" : name);

        Object cachedObj = RedisUtils.get(redisKey);
        if (cachedObj != null && StringUtils.isNotBlank(cachedObj.toString())) {
            ObjectMapper objectMapper = new ObjectMapper();
            List<UserVo> cachedList = objectMapper.readValue(
                    cachedObj.toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, UserVo.class));
            return AppResponse.success(cachedList);
        }

        List<UserVo> result = deptDao.queryUserListByDeptId(name, deptId, tenantId, databaseName);

        ObjectMapper objectMapper = new ObjectMapper();
        RedisUtils.set(redisKey, objectMapper.writeValueAsString(result), 3600);
        return AppResponse.success(result);
    }

    @Override
    public AppResponse<String> getCurrentLevelCode(HttpServletRequest request) {
        try {
            String levelCode = DeptUtils.getLevelCode();
            //            if (StringUtils.isBlank(levelCode)) {
            //                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기모듈levelCode실패");
            //            }
            return AppResponse.success(levelCode);
        } catch (Exception e) {
            log.error("가져오기현재로그인사용자의모듈levelCode실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기모듈levelCode실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<String> getCurrentDeptId(HttpServletRequest request) {
        try {
            String deptId = DeptUtils.getDeptId();
            if (StringUtils.isBlank(deptId)) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기모듈ID실패");
            }
            return AppResponse.success(deptId);
        } catch (Exception e) {
            log.error("가져오기현재로그인사용자의모듈ID실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기모듈ID실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<Org> getCurrentDeptInfo(HttpServletRequest request) {
        try {
            UapOrg uapOrg = DeptUtils.getDeptInfo();
            if (uapOrg == null) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기모듈정보실패");
            }
            Org org = orgMapper.fromUapOrg(uapOrg);
            return AppResponse.success(org);
        } catch (Exception e) {
            log.error("가져오기현재로그인사용자의모듈정보실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기모듈정보실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<Org> getDeptInfoByDeptId(String id, HttpServletRequest request) {
        try {
            if (StringUtils.isBlank(id)) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "부서 ID는 비워 둘 수 없습니다");
            }
            UapOrg uapOrg = DeptUtils.getDeptInfoByDeptId(id);
            if (uapOrg == null) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "찾을 수 없는 모듈정보");
            }
            Org org = orgMapper.fromUapOrg(uapOrg);
            return AppResponse.success(org);
        } catch (Exception e) {
            log.error("근거모듈ID조회모듈정보실패, deptId: {}", id, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회모듈정보실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<String> getLevelCodeByDeptId(String id, HttpServletRequest request) {
        try {
            if (StringUtils.isBlank(id)) {
                return AppResponse.success("");
            }
            String levelCode = DeptUtils.getLevelCodeByDeptId(id);
            if (StringUtils.isBlank(levelCode)) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기모듈levelCode실패");
            }
            return AppResponse.success(levelCode);
        } catch (Exception e) {
            log.error("조회모듈ID의levelCode실패, deptId: {}", id, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회levelCode실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<Long> getUserNumByDeptId(String id, HttpServletRequest request) {
        try {
            if (StringUtils.isBlank(id)) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "모듈ID비워 둘 수 없습니다");
            }
            Long userNum = DeptUtils.getUserNumByDeptId(id);
            if (userNum == null) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "사용자 수 조회 실패");
            }
            return AppResponse.success(userNum);
        } catch (Exception e) {
            log.error("조회지정기기모든기기의사용자수실패, deptId: {}", id, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "사용자 수 조회 실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<List<Org>> queryOrgListByIds(List<String> orgIdList, HttpServletRequest request) {
        try {
            if (CollectionUtil.isEmpty(orgIdList)) {
                return AppResponse.success(Collections.emptyList());
            }
            String tenantId = UapUserInfoAPI.getTenantId(request);
            List<UapOrg> uapOrgs = DeptUtils.queryOrgPageList(tenantId, orgIdList);
            List<Org> orgs = orgMapper.fromUapOrgs(uapOrgs);
            return AppResponse.success(orgs);
        } catch (Exception e) {
            log.error("근거모듈ID목록가져오기모듈정보목록실패, orgIds: {}", orgIdList, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회모듈정보목록실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<String> getDeptIdByUserId(String userId, String tenantId, HttpServletRequest request) {
        try {
            if (StringUtils.isBlank(userId)) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "사용자 ID는 비워 둘 수 없습니다");
            }
            if (StringUtils.isBlank(tenantId)) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "테넌트 ID는 비워 둘 수 없습니다");
            }
            String deptId = DeptUtils.getDeptIdByUserId(userId, tenantId);
            if (StringUtils.isBlank(deptId)) {
                return AppResponse.success("");
            }
            return AppResponse.success(deptId);
        } catch (Exception e) {
            log.error("근거사용자ID가져오기모듈ID실패, userId: {}", userId, e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기모듈ID실패: " + e.getMessage());
        }
    }

    @Override
    public AppResponse<DataAuthDetailDo> getDataAuthWithDeptList(HttpServletRequest request) {
        try {
            DataAuthDetailDo dataAuthDetailDo = DeptUtils.getDataAuthWithDeptList();
            return AppResponse.success(dataAuthDetailDo);
        } catch (Exception e) {
            log.error("조회데이터권한실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회데이터권한실패: " + e.getMessage());
        }
    }
}
