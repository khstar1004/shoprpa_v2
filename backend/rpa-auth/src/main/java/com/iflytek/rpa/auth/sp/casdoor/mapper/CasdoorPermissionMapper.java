package com.iflytek.rpa.auth.sp.casdoor.mapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @desc: Casdoor Permission 및통신사용 Permission 유형의기기, 에서casdoor profile아래
 * @author: Auto Generated
 * @create: 2025/12/11
 */
@Component
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "casdoor", matchIfMissing = true)
public class CasdoorPermissionMapper {

    /**
     * 를 Casdoor Permission 변환로통신사용 Permission
     * 개유형결과, 직선연결행필드복사
     *
     * @param casdoorPermission Casdoor 권한객체
     * @return 통신사용권한객체
     */
    public com.iflytek.rpa.auth.core.entity.Permission toCommonPermission(
            org.casbin.casdoor.entity.Permission casdoorPermission) {
        if (casdoorPermission == null) {
            return null;
        }

        com.iflytek.rpa.auth.core.entity.Permission permission = new com.iflytek.rpa.auth.core.entity.Permission();

        // 본필드(결과, 직선연결복사)
        permission.owner = casdoorPermission.owner;
        permission.name = casdoorPermission.name;
        permission.createdTime = casdoorPermission.createdTime;
        permission.displayName = casdoorPermission.displayName;
        permission.description = casdoorPermission.description;

        // 배열필드
        permission.users = casdoorPermission.users;
        permission.roles = casdoorPermission.roles;
        permission.domains = casdoorPermission.domains;

        // 유형및필드
        permission.model = casdoorPermission.model;
        permission.adapter = casdoorPermission.adapter;
        permission.resourceType = casdoorPermission.resourceType;
        permission.resources = casdoorPermission.resources;
        permission.actions = casdoorPermission.actions;
        permission.effect = casdoorPermission.effect;

        // 상태필드
        permission.isEnabled = casdoorPermission.isEnabled;

        // 닫기필드
        permission.submitter = casdoorPermission.submitter;
        permission.approver = casdoorPermission.approver;
        permission.approveTime = casdoorPermission.approveTime;
        permission.state = casdoorPermission.state;

        return permission;
    }

    /**
     * 를통신사용 Permission 변환로 Casdoor Permission
     * 개유형결과, 직선연결행필드복사
     *
     * @param permission 통신사용권한객체
     * @return Casdoor 권한객체
     */
    public org.casbin.casdoor.entity.Permission toCasdoorPermission(
            com.iflytek.rpa.auth.core.entity.Permission permission) {
        if (permission == null) {
            return null;
        }

        org.casbin.casdoor.entity.Permission casdoorPermission = new org.casbin.casdoor.entity.Permission();

        // 본필드(결과, 직선연결복사)
        casdoorPermission.owner = permission.owner;
        casdoorPermission.name = permission.name;
        casdoorPermission.createdTime = permission.createdTime;
        casdoorPermission.displayName = permission.displayName;
        casdoorPermission.description = permission.description;

        // 배열필드
        casdoorPermission.users = permission.users;
        casdoorPermission.roles = permission.roles;
        casdoorPermission.domains = permission.domains;

        // 유형및필드
        casdoorPermission.model = permission.model;
        casdoorPermission.adapter = permission.adapter;
        casdoorPermission.resourceType = permission.resourceType;
        casdoorPermission.resources = permission.resources;
        casdoorPermission.actions = permission.actions;
        casdoorPermission.effect = permission.effect;

        // 상태필드
        casdoorPermission.isEnabled = permission.isEnabled;

        // 닫기필드
        casdoorPermission.submitter = permission.submitter;
        casdoorPermission.approver = permission.approver;
        casdoorPermission.approveTime = permission.approveTime;
        casdoorPermission.state = permission.state;

        return casdoorPermission;
    }
}