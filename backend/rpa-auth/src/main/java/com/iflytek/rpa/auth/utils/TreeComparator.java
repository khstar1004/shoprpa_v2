package com.iflytek.rpa.auth.utils;

import static com.iflytek.rpa.auth.sp.uap.constants.AuthConstant.NODE_TYPE_MENU;
import static com.iflytek.rpa.auth.sp.uap.constants.AuthConstant.NODE_TYPE_RESOURCE;

import com.iflytek.rpa.auth.conf.condition.ConditionalOnSaaSOrUAP;
import com.iflytek.rpa.auth.exception.ServiceException;
import com.iflytek.sec.uap.client.core.dto.TreeNode;
import com.iflytek.sec.uap.client.core.dto.authority.BindAuthorityResourceDto;
import java.util.*;
import lombok.Data;

/**
 * @author mjren
 * @date 2025-03-05 11:05
 * @copyright Copyright (c) 2025 mjren
 */
@ConditionalOnSaaSOrUAP
public class TreeComparator {

    // 결과내용기기
    private List<String> resourceCancel = new ArrayList<>();
    private List<String> menuCancel = new ArrayList<>();
    private Map<String, BindAuthorityResourceDto> authMap = new HashMap<>();

    // ID -> 객체의(사용빠름조회)
    private Map<String, TreeNode> modifiedNodeMap = new HashMap<>();

    /**
     * TreeNode, 일예사용자전의데이터, 이예사용자메뉴및가져오기 또는추가선택후의데이터, 
     * 결과전체, 예매개중의checked속성값(true,false)아니요, 
     * TreeNodechecked속성값, 를이중일, 
     * checked로false의까지resourceCancel목록 , 
     * checked로false의까지menuCancel목록 , 
     * 일checked로true의및의직선연결까지Map<String, BindAuthorityResourceDto> authMap
     */
    public CompareResult compareTrees(TreeNode originalRoot, TreeNode modifiedRoot) {
        // 1: 생성수정의
        buildNodeMap(modifiedRoot);

        // 2: 
        traverseAndCompare(originalRoot, modifiedRoot);
        return new CompareResult(resourceCancel, menuCancel, authMap);
    }

    // 생성테이블
    private void buildNodeMap(TreeNode root) {
        Deque<TreeNode> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            TreeNode node = stack.pop();
            modifiedNodeMap.put(node.getId(), node);
            for (TreeNode child : node.getNodes()) {
                stack.push(child);
            }
        }
    }

    private void traverseAndCompare(TreeNode originalNode, TreeNode modifiedNode) {
        if (originalNode == null || modifiedNode == null) return;

        // checked상태변수
        boolean originalChecked = Boolean.TRUE.equals(originalNode.getChecked());
        boolean modifiedChecked = Boolean.TRUE.equals(modifiedNode.getChecked());

        // 관리가져오기 선택의
        if (originalChecked && !modifiedChecked) {
            if (isLeaf(modifiedNode) && NODE_TYPE_RESOURCE.equals(modifiedNode.getValue())) {
                resourceCancel.add(modifiedNode.getId());
            } else {
                menuCancel.add(modifiedNode.getId());
            }
        }

        // 관리추가선택의
        if (!originalChecked && modifiedChecked) {
            if (isLeaf(modifiedNode)) {
                processNewCheckedLeaf(modifiedNode);
            }
        }

        // 
        List<TreeNode> originalChildren = originalNode.getNodes();
        List<TreeNode> modifiedChildren = modifiedNode.getNodes();
        for (int i = 0; i < modifiedChildren.size(); i++) {
            traverseAndCompare(i < originalChildren.size() ? originalChildren.get(i) : null, modifiedChildren.get(i));
        }
    }

    private boolean isLeaf(TreeNode node) {
        return node.getNodes() == null || node.getNodes().isEmpty();
    }

    private void processNewCheckedLeaf(TreeNode leafNode) {
        if (NODE_TYPE_MENU.equals(leafNode.getValue())) {
            BindAuthorityResourceDto bindAuthorityResourceDto = new BindAuthorityResourceDto();
            bindAuthorityResourceDto.setAuthId(leafNode.getId());
            bindAuthorityResourceDto.setResourceIds(new ArrayList<>());
            authMap.put(leafNode.getId(), bindAuthorityResourceDto);
        } else if (NODE_TYPE_RESOURCE.equals(leafNode.getValue())) {
            TreeNode parent = findNearestMenuParent(leafNode);
            if (parent != null) {
                BindAuthorityResourceDto dto =
                        authMap.computeIfAbsent(parent.getId(), k -> new BindAuthorityResourceDto());
                dto.setAuthId(parent.getId());
                if (dto.getResourceIds() == null) {
                    dto.setResourceIds(new ArrayList<>());
                }
                dto.getResourceIds().add(leafNode.getId());
            }
        } else {
            throw new ServiceException("지원하지 않는메뉴유형: " + leafNode.getValue());
        }
    }

    private TreeNode findNearestMenuParent(TreeNode node) {
        TreeNode current = node;
        while (current != null) {
            if (NODE_TYPE_MENU.equals(current.getValue())) {
                return current;
            }
            current = modifiedNodeMap.get(current.getPid());
        }
        return null;
    }

    // 데이터결과지정
    @Data
    public static class CompareResult {
        private List<String> resourceCancel;
        private List<String> menuCancel;
        private Map<String, BindAuthorityResourceDto> authMap;

        public CompareResult(
                List<String> resourceCancel, List<String> menuCancel, Map<String, BindAuthorityResourceDto> authMap) {
            this.resourceCancel = resourceCancel;
            this.menuCancel = menuCancel;
            this.authMap = authMap;
        }
    }
}