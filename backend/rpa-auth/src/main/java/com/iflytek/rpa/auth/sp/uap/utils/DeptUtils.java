package com.iflytek.rpa.auth.sp.uap.utils;

import com.iflytek.rpa.auth.conf.condition.ConditionalOnSaaSOrUAP;
import com.iflytek.rpa.auth.core.entity.DataAuthDetailDo;
import com.iflytek.rpa.auth.core.entity.OrgListDto;
import com.iflytek.rpa.auth.exception.NoLoginException;
import com.iflytek.rpa.auth.utils.HttpUtils;
import com.iflytek.sec.uap.client.api.ClientAuthenticationAPI;
import com.iflytek.sec.uap.client.api.ClientManagementAPI;
import com.iflytek.sec.uap.client.api.UapUserInfoAPI;
import com.iflytek.sec.uap.client.core.client.ManagementClient;
import com.iflytek.sec.uap.client.core.dto.PageDto;
import com.iflytek.sec.uap.client.core.dto.ResponseDto;
import com.iflytek.sec.uap.client.core.dto.dataauthority.DataAuthorityWithDimDictDto;
import com.iflytek.sec.uap.client.core.dto.org.GetOrgDto;
import com.iflytek.sec.uap.client.core.dto.org.OrgExtendDto;
import com.iflytek.sec.uap.client.core.dto.org.UapOrg;
import com.iflytek.sec.uap.client.core.dto.user.*;
import com.iflytek.sec.uap.client.util.Oauth2Util;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

/**
 * @author mjren
 * @date 2025-05-08 11:22
 * @copyright Copyright (c) 2025 mjren
 */
@ConditionalOnSaaSOrUAP
public class DeptUtils {
    private static final Logger log = LoggerFactory.getLogger(DeptUtils.class);

    public DeptUtils() {}

    /*
     UAP필드및RPA필드의: 
     levelCode = deptIdPath
     id = deptId
    */

    /**
     * 가져오기현재로그인사용자의모듈levelCode, deptIdPath
     * @return
     */
    public static String getLevelCode() {
        UapOrg deptInfo = getDeptInfo();
        if (deptInfo != null) {
            return deptInfo.getLevelCode() + "-";
        } else {
            log.error("가져오기모듈id path실패");
            return null;
        }
    }

    /**
     * 가져오기현재로그인사용자의모듈id
     * @return
     */
    public static String getDeptId() {
        UapOrg deptInfo = getDeptInfo();
        if (deptInfo != null) {
            return deptInfo.getId();
        } else {
            log.error("가져오기모듈id실패");
            return null;
        }
    }

    /**
     * 가져오기현재로그인사용자의모듈정보
     * @return
     */
    public static UapOrg getDeptInfo() {
        String accessToken = Oauth2Util.getAccessToken(HttpUtils.getRequest());
        return ClientAuthenticationAPI.getUserOrgInfo(null, null, accessToken);
    }

    /**
     * 근거모듈id조회모듈정보
     * @param id
     * @return
     */
    public static UapOrg getDeptInfoByDeptId(String id) {
        GetOrgDto getOrgDto = new GetOrgDto();
        getOrgDto.setId(id);
        OrgExtendDto orgExtendDto =
                ClientManagementAPI.getOrgExtendInfo(UapUserInfoAPI.getTenantId(HttpUtils.getRequest()), getOrgDto);
        if (orgExtendDto != null) {
            return orgExtendDto.getUapOrg();
        } else {
            log.error("가져오기모듈정보실패");
            return null;
        }
    }

    /**
     * 조회모듈id의levelCode
     * @param id
     * @return
     */
    public static String getLevelCodeByDeptId(String id) {
        GetOrgDto getOrgDto = new GetOrgDto();
        getOrgDto.setId(id);
        OrgExtendDto orgExtendDto =
                ClientManagementAPI.getOrgExtendInfo(UapUserInfoAPI.getTenantId(HttpUtils.getRequest()), getOrgDto);
        if (orgExtendDto != null) {
            return orgExtendDto.getUapOrg().getLevelCode() + "-";
        } else {
            log.error("가져오기모듈정보실패");
            return null;
        }
    }

    /**
     * 조회지정기기모든기기의사용자수
     * @param id
     * @return
     */
    public static Long getUserNumByDeptId(String id) {
        ManagementClient managementClient = UapManagementClientUtil.getManagementClient(HttpUtils.getRequest());
        ListUserByOrgDto listUserByOrgDto = new ListUserByOrgDto();
        listUserByOrgDto.setOrgId(id);
        // 분조회현재기기모든기기의사용자
        ResponseDto<PageDto<UapUser>> userListPageResponse = managementClient.queryUserPageListByOrg(listUserByOrgDto);
        if (userListPageResponse.isFlag()) {
            return userListPageResponse.getData().getTotalCount();
        } else {
            log.error("queryUserPageListByOrg연결호출예외 {}", userListPageResponse.getMessage());
            return null;
        }
    }

    /**
     * 근거모듈id목록가져오기모듈정보목록
     * @param tenantId
     * @param orgIdList
     * @return
     */
    public static List<UapOrg> queryOrgPageList(String tenantId, List<String> orgIdList) {
        OrgListDto orgListDto = new OrgListDto();
        orgListDto.setOrgIds(orgIdList);
        ResponseDto<PageDto<UapOrg>> orgPageResponse =
                UapManagementClientUtil.queryOrgPageList(tenantId, orgListDto, HttpUtils.getRequest());
        if (!orgPageResponse.isFlag()) {
            log.error("queryOrgPageList error, msg:{}", orgPageResponse.getMessage());
            throw new RuntimeException(orgPageResponse.getMessage());
        }
        return orgPageResponse.getData().getResult();
    }

    public static String getDeptIdByUserId(String userId, String tenantId) {
        GetUserDto getUserDto = new GetUserDto();
        getUserDto.setUserId(userId);
        UserExtendDto userExtendDto = ClientManagementAPI.getUserExtendInfo(tenantId, getUserDto);
        UapUser user = userExtendDto.getUser();
        if (null == user) {
            return null;
        }
        return user.getOrgId();
    }

    /**
     * 조회데이터권한, 예일개모듈목록
     * @return
     */
    public static DataAuthDetailDo getDataAuthWithDeptList() throws NoLoginException {
        UapUser uapUser = UserUtils.nowLoginUser();
        // admin 단일관리
        if (uapUser.getLoginName().equals("admin")) {
            DataAuthDetailDo dataAuthDetailDo = new DataAuthDetailDo();
            dataAuthDetailDo.setDataAuthType("all");
            return dataAuthDetailDo;
        }

        DataAuthDetailDo dataAuthDetailDo = new DataAuthDetailDo();
        List<DataAuthorityWithDimDictDto> dataAuthList = UapUserInfoAPI.getDataAuthList(HttpUtils.getRequest());
        if (dataAuthList == null || CollectionUtils.isEmpty(dataAuthList)) {
            dataAuthDetailDo.setDataAuthType("all");
            return dataAuthDetailDo;
        }
        DataAuthorityWithDimDictDto checkedDataAuth = null;
        for (DataAuthorityWithDimDictDto dataAuth : dataAuthList) {
            if (null != dataAuth && dataAuth.isChecked()) {
                checkedDataAuth = dataAuth;
                break;
            }
        }
        if (null == checkedDataAuth) {
            dataAuthDetailDo.setDataAuthType("all");
            return dataAuthDetailDo;
        }
        // 결과가예데이터권한로전체, 반환null
        if ("전체".equals(checkedDataAuth.getDataAuthName())) {
            dataAuthDetailDo.setDataAuthType("all");
            return dataAuthDetailDo;
        }
        // 결과가예데이터권한로에서모듈, 이면반환에서모듈levelCode
        if ("에서모듈".equals(checkedDataAuth.getDataAuthName())) {
            UapOrg deptInfo = getDeptInfo();
            List<String> deptIdList = Collections.singletonList(deptInfo.getId());
            List<String> deptIdPathList = Collections.singletonList(deptInfo.getLevelCode() + "-");
            dataAuthDetailDo.setDataAuthType("in_dept");
            dataAuthDetailDo.setDeptIdList(deptIdList);
            dataAuthDetailDo.setDeptIdPathList(deptIdPathList);
            return dataAuthDetailDo;
        }
        // todo 결과가데이터권한로지정모듈, 이면조회모듈, 근거모듈id가져오기levelCode
        // todo 결과가데이터권한로개사람, 이면반환type, 아니요
        dataAuthDetailDo.setDataAuthType("all");
        return dataAuthDetailDo;
    }
}