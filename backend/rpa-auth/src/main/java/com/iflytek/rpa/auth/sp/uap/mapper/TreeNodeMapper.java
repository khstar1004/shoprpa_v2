package com.iflytek.rpa.auth.sp.uap.mapper;

import com.iflytek.rpa.auth.core.entity.TreeNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * TreeNode기기
 * 사용를UAP클라이언트의TreeNode및core패키지아래의TreeNode변환
 *
 * 비고: 개TreeNode유형의nodes필드예의(List&lt;TreeNode&gt;), 
 * 원인변환시필요관리.
 *
 * @author xqcao2
 */
@Component
public class TreeNodeMapper {

    /**
     * 를UAP클라이언트의TreeNode변환로TreeNode
     * 변환nodes목록
     *
     * @param uapTreeNode UAP클라이언트의TreeNode
     * @return core패키지아래의TreeNode
     */
    public TreeNode fromUapTreeNode(com.iflytek.sec.uap.client.core.dto.TreeNode uapTreeNode) {
        if (uapTreeNode == null) {
            return null;
        }

        TreeNode treeNode = new TreeNode();
        // 사용BeanUtils복사본속성
        BeanUtils.copyProperties(uapTreeNode, treeNode);

        // 변환nodes목록
        if (uapTreeNode.getNodes() != null && !uapTreeNode.getNodes().isEmpty()) {
            List<TreeNode> nodes = new ArrayList<>();
            for (com.iflytek.sec.uap.client.core.dto.TreeNode uapNode : uapTreeNode.getNodes()) {
                TreeNode node = fromUapTreeNode(uapNode);
                if (node != null) {
                    nodes.add(node);
                }
            }
            treeNode.setNodes(nodes);
        } else {
            treeNode.setNodes(new ArrayList<>());
        }

        return treeNode;
    }

    /**
     * 량를UAP클라이언트의TreeNode목록변환로TreeNode목록
     *
     * @param uapTreeNodes UAP클라이언트의TreeNode목록
     * @return core패키지아래의TreeNode목록
     */
    public List<TreeNode> fromUapTreeNodes(List<com.iflytek.sec.uap.client.core.dto.TreeNode> uapTreeNodes) {
        if (uapTreeNodes == null || uapTreeNodes.isEmpty()) {
            return Collections.emptyList();
        }

        return uapTreeNodes.stream()
                .map(this::fromUapTreeNode)
                .filter(treeNode -> treeNode != null)
                .collect(Collectors.toList());
    }

    /**
     * 를core패키지아래의TreeNode변환로UAP클라이언트의TreeNode
     * 변환nodes목록
     *
     * @param treeNode core패키지아래의TreeNode
     * @return UAP클라이언트의TreeNode
     */
    public com.iflytek.sec.uap.client.core.dto.TreeNode toUapTreeNode(TreeNode treeNode) {
        if (treeNode == null) {
            return null;
        }

        com.iflytek.sec.uap.client.core.dto.TreeNode uapTreeNode = new com.iflytek.sec.uap.client.core.dto.TreeNode();
        // 사용BeanUtils복사본속성
        BeanUtils.copyProperties(treeNode, uapTreeNode);

        // 변환nodes목록
        if (treeNode.getNodes() != null && !treeNode.getNodes().isEmpty()) {
            List<com.iflytek.sec.uap.client.core.dto.TreeNode> nodes = new ArrayList<>();
            for (TreeNode node : treeNode.getNodes()) {
                com.iflytek.sec.uap.client.core.dto.TreeNode uapNode = toUapTreeNode(node);
                if (uapNode != null) {
                    nodes.add(uapNode);
                }
            }
            uapTreeNode.setNodes(nodes);
        } else {
            uapTreeNode.setNodes(new ArrayList<>());
        }

        return uapTreeNode;
    }

    /**
     * 량를core패키지아래의TreeNode목록변환로UAP클라이언트의TreeNode목록
     *
     * @param treeNodes core패키지아래의TreeNode목록
     * @return UAP클라이언트의TreeNode목록
     */
    public List<com.iflytek.sec.uap.client.core.dto.TreeNode> toUapTreeNodes(List<TreeNode> treeNodes) {
        if (treeNodes == null || treeNodes.isEmpty()) {
            return Collections.emptyList();
        }

        return treeNodes.stream()
                .map(this::toUapTreeNode)
                .filter(uapTreeNode -> uapTreeNode != null)
                .collect(Collectors.toList());
    }
}