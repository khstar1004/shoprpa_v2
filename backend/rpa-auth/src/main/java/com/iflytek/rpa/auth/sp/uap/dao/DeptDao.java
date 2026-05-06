package com.iflytek.rpa.auth.sp.uap.dao;

import com.iflytek.rpa.auth.core.entity.DeptPersonInfoBo;
import com.iflytek.rpa.auth.core.entity.DeptTreeNodeVo;
import com.iflytek.rpa.auth.core.entity.UserRoleDto;
import com.iflytek.rpa.auth.core.entity.UserVo;
import com.iflytek.sec.uap.client.core.dto.org.UapOrg;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 *
 * @author jqfang3
 * @since 2025-08-04
 */
@Mapper
public interface DeptDao {
    List<DeptTreeNodeVo> queryChildrenOrgList(String pid, String tenantId, String databaseName);

    List<String> queryDeptIdsWithChildren(List<String> deptIds, String tenantId, String databaseName);

    String queryDeptNameByDeptId(String deptId, String tenantId, String databaseName);

    String queryByHigherDeptId(String higherDeptId, String tenantId, String databaseName);

    List<DeptPersonInfoBo> queryUserNumByOrgIds(List<String> childrenIds, String tenantId, String databaseName);

    List<String> getMatchedIds(String deptId, String databaseName);

    List<UserVo> queryUserListByDeptId(String name, String deptId, String tenantId, String databaseName);

    /**
     * 근거로그인이름조회사용자의단계기기ID
     */
    String queryFirstLevelOrgIdByLoginName(String loginName, String tenantId, String databaseName);

    /**
     * 조회지정parent_org의모든직선모듈
     */
    List<UserVo> queryChildOrgsByParentOrgId(String parentOrgId, String tenantId, String databaseName);

    /**
     * 조회지정사용자ID목록중있음역할의사용자ID및역할이름
     */
    List<UserRoleDto> queryUserIdsWithRoles(List<String> userIds, String tenantId, String databaseName);

    List<UapOrg> queryUapOrgByName(String name, String tenantId, String databaseName);
}