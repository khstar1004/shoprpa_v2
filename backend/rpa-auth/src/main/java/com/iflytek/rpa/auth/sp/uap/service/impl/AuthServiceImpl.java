package com.iflytek.rpa.auth.sp.uap.service.impl;

import static com.iflytek.rpa.auth.sp.uap.constants.AuthConstant.NODE_TYPE_MENU;
import static com.iflytek.rpa.auth.sp.uap.constants.AuthConstant.NODE_TYPE_RESOURCE;

import cn.hutool.core.collection.CollectionUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.iflytek.rpa.auth.conf.condition.ConditionalOnSaaSOrUAP;
import com.iflytek.rpa.auth.core.entity.BindResourceDto;
import com.iflytek.rpa.auth.core.entity.RoleAuthResourceDto;
import com.iflytek.rpa.auth.core.service.AuthService;
import com.iflytek.rpa.auth.sp.uap.mapper.TreeNodeMapper;
import com.iflytek.rpa.auth.sp.uap.utils.UapManagementClientUtil;
import com.iflytek.rpa.auth.utils.AppResponse;
import com.iflytek.rpa.auth.utils.ErrorCodeEnum;
import com.iflytek.rpa.auth.utils.TreeComparator;
import com.iflytek.sec.uap.client.api.ClientManagementAPI;
import com.iflytek.sec.uap.client.api.UapUserInfoAPI;
import com.iflytek.sec.uap.client.core.dto.ResponseDto;
import com.iflytek.sec.uap.client.core.dto.TreeNode;
import com.iflytek.sec.uap.client.core.dto.authority.BindAuthorityResourceDto;
import com.iflytek.sec.uap.client.core.dto.authority.UapAuthority;
import com.iflytek.sec.uap.client.core.dto.resource.UapResource;
import com.iflytek.sec.uap.client.core.dto.role.BindAuthDto;
import com.iflytek.sec.uap.client.core.dto.role.BindRoleAuthResourceDto;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 메뉴
 */
@Slf4j
@Service("authService")
@ConditionalOnSaaSOrUAP
public class AuthServiceImpl implements AuthService {

    @Autowired
    private TreeNodeMapper treeNodeMapper;

    /**
     * 현재로그인사용자에서사용중의메뉴정보
     * @param request HTTP요청 
     * @return 메뉴목록
     */
    @Override
    public AppResponse<List<com.iflytek.rpa.auth.core.entity.TreeNode>> getUserAuthTreeInApp(
            HttpServletRequest request) {
        // 현재로그인사용자에서사용중의메뉴정보
        List<UapAuthority> authList = UapUserInfoAPI.getMenuListList(request);
        List<TreeNode> uapTreeNodeList = buildMenuTree("0", authList);
        // 를UAP의TreeNode목록변환로core의TreeNode목록
        List<com.iflytek.rpa.auth.core.entity.TreeNode> treeNodeList = treeNodeMapper.fromUapTreeNodes(uapTreeNodeList);
        return AppResponse.success(treeNodeList);
    }

    public List<TreeNode> buildMenuTree(String rootId, List<UapAuthority> authList) {

        Map<String, TreeNode> nodeMap = new HashMap<>(authList.size() * 2);
        List<TreeNode> allNodes = new ArrayList<>();
        // 일: 생성모든까지
        for (UapAuthority authority : authList) {
            TreeNode node = convertToTreeNode(authority);
            allNodes.add(node);
            nodeMap.put(node.getId(), node);
        }
        // 이: 생성결과
        // 생성결과: 를까지아래
        List<TreeNode> rootNodes = new ArrayList<>();
        for (TreeNode node : allNodes) {
            String parentId = node.getPid();
            if (rootId.equals(parentId)) {
                // 결과가예(parentId로0), 직선연결추가입력목록
                rootNodes.add(node);
            } else {
                // 까지, 를현재추가입력의목록
                TreeNode parent = nodeMap.get(parentId);
                if (parent != null) {
                    parent.getNodes().add(node);
                }
            }
        }
        // 3. 매개의목록  sort 필드정렬
        // 비고: 모든, 있음패키지의실행정렬
        for (TreeNode node : allNodes) {
            List<TreeNode> children = node.getNodes();
            if (!CollectionUtil.isEmpty(children)) {
                // 사용 sort 필드상승순서정렬
                children.sort(
                        Comparator.comparing(TreeNode::getSort, Comparator.nullsFirst(Comparator.naturalOrder())));
            }
        }
        return rootNodes;
    }

    // 방법법: 를 UapAuthority 변환로 TreeNode
    private TreeNode convertToTreeNode(UapAuthority authority) {
        TreeNode node = new TreeNode();
        node.setId(authority.getId());
        node.setName(authority.getName());
        node.setPid(authority.getParentId());
        node.setSort(authority.getSort());
        node.setValue(authority.getUrl());
        return node;
    }

    /**
     * 조회메뉴, 권한
     * @param roleId 역할ID
     * @param request HTTP요청 
     * @return 메뉴권한
     */
    @Override
    public AppResponse<com.iflytek.rpa.auth.core.entity.TreeNode> getAuthResourceTreeInApp(
            String roleId, HttpServletRequest request) {
        if (null == roleId) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "적음역할id");
        }
        // 사용내부전체량메뉴데이터
        List<UapAuthority> menuList = UapUserInfoAPI.getMenuListList(request);
        // 선택의메뉴데이터
        String tenantId = UapUserInfoAPI.getTenantId(request);
        List<UapAuthority> checkedAuthList = ClientManagementAPI.queryAuthorityListByRoleId(tenantId, roleId);
        // 선택한 메뉴 데이터를 메뉴 ID 기준으로 그룹화
        Set<String> checkedAuthIdSet =
                checkedAuthList.stream().map(UapAuthority::getId).collect(Collectors.toSet());
        // 근거id및parentId그룹설치메뉴
        List<String> leafNodeAuthIdList = new ArrayList<>();
        TreeNode treeAuthNode = buildTree("0", menuList, checkedAuthIdSet, leafNodeAuthIdList);
        // 조회선택의데이터
        ResponseDto<List<UapResource>> checkedResourceListResponse =
                UapManagementClientUtil.queryResourceListByRoleId(roleId, request);
        if (!checkedResourceListResponse.isFlag()) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, checkedResourceListResponse.getMessage());
        }
        List<UapResource> checkedResourceList = checkedResourceListResponse.getData();
        // 근거resourceId분그룹
        Set<String> checkedResourceIdSet =
                checkedResourceList.stream().map(UapResource::getId).collect(Collectors.toSet());
        // 다중예외조회권한
        Map<String, List<TreeNode>> authResourceMap = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String leafNodeAuthId : leafNodeAuthIdList) {
            CompletableFuture<Void> future = CompletableFuture.supplyAsync(
                            () -> {
                                // 조회해당아래의목록
                                return ClientManagementAPI.queryResourceListByAuthId(tenantId, leafNodeAuthId);
                            },
                            RESOURCE_QUERY_EXECUTOR)
                    .thenAcceptAsync(
                            checkResourceList -> {
                                // 변환로TreeNode선택상태
                                List<TreeNode> resourceNodeList = new ArrayList<>();
                                for (UapResource resource : checkResourceList) {
                                    TreeNode treeResourceNode =
                                            convertResourceToTreeNode(resource, leafNodeAuthId, NODE_TYPE_RESOURCE);
                                    treeResourceNode.setChecked(
                                            checkedResourceIdSet.contains(treeResourceNode.getId()));
                                    resourceNodeList.add(treeResourceNode);
                                }
                                if (!resourceNodeList.isEmpty()) {
                                    authResourceMap.put(leafNodeAuthId, resourceNodeList);
                                }
                            },
                            RESOURCE_QUERY_EXECUTOR);

            futures.add(future);
        }
        // 대기모든예외작업완료
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Thread.currentThread().interrupt();
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "권한조회시간 초과");
        }

        // 사용BFS
        setAuthResourceViaBFS(treeAuthNode, authResourceMap);

        // 를UAP의TreeNode변환로core의TreeNode
        com.iflytek.rpa.auth.core.entity.TreeNode coreTreeNode = treeNodeMapper.fromUapTreeNode(treeAuthNode);
        return AppResponse.success(coreTreeNode);
    }

    // 정도
    private void setAuthResourceViaBFS(TreeNode root, Map<String, List<TreeNode>> authResourceMap) {
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            List<TreeNode> resources = authResourceMap.get(node.getId());
            if (resources != null && !resources.isEmpty()) {
                node.getNodes().addAll(resources);
                node.setHasNodes(true);
            }
            queue.addAll(node.getNodes());
        }
    }

    public TreeNode buildTree(
            String rootId, List<UapAuthority> menuList, Set<String> checkedAuthIdSet, List<String> leafNodeAuthIdList) {
        Map<String, TreeNode> nodeMap = new HashMap<>(menuList.size() * 2);
        TreeNode rootNode = null;
        // 일: 생성모든까지
        for (UapAuthority authority : menuList) {
            TreeNode node = convertAuthToTreeNode(authority, NODE_TYPE_MENU);
            node.setChecked(checkedAuthIdSet.contains(authority.getId())); // 
            nodeMap.put(authority.getId(), node);

            if (rootId.equals(authority.getParentId())) {
                rootNode = node;
            }
        }
        // 이: 생성결과
        for (UapAuthority authority : menuList) {
            String parentId = authority.getParentId();
            if (rootId.equals(parentId)) continue;

            TreeNode node = nodeMap.get(authority.getId());
            TreeNode parent = nodeMap.get(parentId);

            if (parent != null) {
                parent.getNodes().add(node);
                parent.setHasNodes(true);
            }
        }
        // (로행)
        nodeMap.values().parallelStream()
                .filter(node -> node.getNodes().isEmpty())
                .forEach(node -> leafNodeAuthIdList.add(node.getId()));
        return rootNode;
    }

    private TreeNode convertAuthToTreeNode(UapAuthority authority, String nodeType) {
        TreeNode node = new TreeNode();
        doConvertAuthToTreeNode(node, authority, nodeType);
        return node;
    }

    private void doConvertAuthToTreeNode(TreeNode node, UapAuthority authority, String nodeType) {
        // 재객체상태
        node.setId(authority.getId());
        node.setName(authority.getName());
        node.setPid(authority.getParentId());
        node.setSort(authority.getSort());
        node.setValue(nodeType);
        node.setHasNodes(false); // 로false, 후근거업데이트
    }

    private TreeNode convertResourceToTreeNode(UapResource resource, String authId, String nodeType) {
        TreeNode node = new TreeNode();
        doConvertResourceToTreeNode(node, resource, authId, nodeType);
        return node;
    }

    private void doConvertResourceToTreeNode(TreeNode node, UapResource resource, String authId, String nodeType) {
        // 재객체상태
        node.setId(resource.getId());
        node.setName(resource.getName());
        node.setPid(authId);
        node.setSort(resource.getSort());
        node.setValue(nodeType);
        node.setHasNodes(false); // 로false, 후근거업데이트
    }

    // 근거CPU데이터데이터(생성값: CPU데이터 * 2)
    private static final ExecutorService RESOURCE_QUERY_EXECUTOR = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2, // 근거CPU데이터
            new ThreadFactoryBuilder()
                    .setNameFormat("RoleManage-resource-query-thread-%d")
                    .build());

    @PreDestroy
    public void shutdown() {
        RESOURCE_QUERY_EXECUTOR.shutdownNow();
    }

    /**
     * 저장메뉴, 
     * @param roleAuthResourceDto 역할권한DTO
     * @param request HTTP요청 
     * @return 결과
     */
    @Override
    public AppResponse<String> saveRoleAuth(RoleAuthResourceDto roleAuthResourceDto, HttpServletRequest request) {
        // 가져오기저장전의메뉴권한
        AppResponse<com.iflytek.rpa.auth.core.entity.TreeNode> originalTreeResponse =
                getAuthResourceTreeInApp(roleAuthResourceDto.getRoleId(), request);
        if (!originalTreeResponse.ok()) {
            // 유형변환: 를 AppResponse<TreeNode> 의오류 변환로 AppResponse<String>
            // 오류 중 data 필드아니요재필요, 재필요의예 code 및 message
            return AppResponse.error(originalTreeResponse.getCode(), originalTreeResponse.getMessage());
        }
        com.iflytek.rpa.auth.core.entity.TreeNode originalTree = originalTreeResponse.getData();
        TreeNode uapTreeNode = treeNodeMapper.toUapTreeNode(originalTree);
        TreeComparator comparator = new TreeComparator();
        // core유형변환uap유형
        TreeNode mapperUapTreeNode = treeNodeMapper.toUapTreeNode(roleAuthResourceDto.getTreeNode());
        TreeComparator.CompareResult result = comparator.compareTrees(uapTreeNode, mapperUapTreeNode);
        // 가져오기가져오기 선택의및메뉴
        List<String> canceledResources = result.getResourceCancel();
        List<String> canceledMenus = result.getMenuCancel();
        // 가져오기새선택의메뉴및
        Map<String, BindAuthorityResourceDto> newBindingMap = result.getAuthMap();
        //        log.info("반환: {}, {}",canceledResources,canceledMenus);
        String tenantId = UapUserInfoAPI.getTenantId(request);
        // 저장전가져오기 선택의해제
        // 해제
        if (!CollectionUtil.isEmpty(canceledResources)) {
            BindResourceDto bindResourceDto = new BindResourceDto();

            bindResourceDto.setRoleId(roleAuthResourceDto.getRoleId());
            bindResourceDto.setTenantId(tenantId);
            bindResourceDto.setResourceIds(canceledResources);

            ResponseDto<Object> unBindRoleResourceResponse =
                    UapManagementClientUtil.unBindRoleResource(tenantId, bindResourceDto, request);
            if (!unBindRoleResourceResponse.isFlag()) {
                log.error("해제실패: {}", unBindRoleResourceResponse.getMessage());
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, unBindRoleResourceResponse.getMessage());
            }
        }
        // 해제메뉴
        if (!CollectionUtil.isEmpty(canceledMenus)) {
            BindAuthDto bindAuthDto = new BindAuthDto();
            bindAuthDto.setRoleId(roleAuthResourceDto.getRoleId());
            bindAuthDto.setAuthIdList(canceledMenus);
            ResponseDto<Object> unbindRoleAuthResponse = ClientManagementAPI.unbindRoleAuth(tenantId, bindAuthDto);
            if (!unbindRoleAuthResponse.isFlag()) {
                log.error("해제메뉴실패: {}", unbindRoleAuthResponse.getMessage());
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, unbindRoleAuthResponse.getMessage());
            }
        }
        // 지정사용자새선택의메뉴및
        Collection<BindAuthorityResourceDto> authorityResourceList = newBindingMap.values();
        if (!CollectionUtil.isEmpty(authorityResourceList)) {
            BindRoleAuthResourceDto bindRoleAuthResourceDto = new BindRoleAuthResourceDto();
            bindRoleAuthResourceDto.setRoleId(roleAuthResourceDto.getRoleId());
            bindRoleAuthResourceDto.setAuthorityResources(new ArrayList<>(authorityResourceList));
            ResponseDto<Object> saveResponse = ClientManagementAPI.bindRoleAuthResource(
                    UapUserInfoAPI.getTenantId(request), bindRoleAuthResourceDto);
            if (!saveResponse.isFlag()) {
                log.error("지정메뉴실패: {}", saveResponse.getMessage());
            }
        }

        return AppResponse.success("저장성공");
    }
}