package com.iflytek.rpa.auth.sp.uap.service.impl;

import com.iflytek.rpa.auth.conf.condition.ConditionalOnSaaSOrUAP;
import com.iflytek.rpa.auth.core.entity.Authority;
import com.iflytek.rpa.auth.core.entity.BindRoleDataAuthDto;
import com.iflytek.rpa.auth.core.service.DataAuthService;
import com.iflytek.rpa.auth.sp.uap.mapper.AuthorityMapper;
import com.iflytek.rpa.auth.sp.uap.mapper.DataAuthorityWithDimDictDtoMapper;
import com.iflytek.rpa.auth.sp.uap.utils.UapManagementClientUtil;
import com.iflytek.rpa.auth.utils.AppResponse;
import com.iflytek.rpa.auth.utils.ErrorCodeEnum;
import com.iflytek.sec.uap.client.api.ClientManagementAPI;
import com.iflytek.sec.uap.client.api.UapUserInfoAPI;
import com.iflytek.sec.uap.client.core.client.ManagementClient;
import com.iflytek.sec.uap.client.core.dto.ResponseDto;
import com.iflytek.sec.uap.client.core.dto.authority.UapAuthority;
import com.iflytek.sec.uap.client.core.dto.dataauthority.DataAuthorityWithDimDictDto;
import com.iflytek.sec.uap.client.core.dto.role.BindDataAuthDto;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * 데이터권한서비스
 */
@Slf4j
@Service("dataAuthService")
@ConditionalOnSaaSOrUAP
public class DataAuthServiceImpl implements DataAuthService {

    @Autowired
    private AuthorityMapper authorityMapper;

    @Autowired
    private DataAuthorityWithDimDictDtoMapper dataAuthorityWithDimDictDtoMapper;

    @Override
    public AppResponse<List<com.iflytek.rpa.auth.core.entity.DataAuthorityWithDimDictDto>> getCheckedDataAuth(
            String roleId, HttpServletRequest request) {
        String tenantId = UapUserInfoAPI.getTenantId(request);
        // 가져오기전체데이터권한
        //        List<DataAuthorityWithDimDictDto> dataAuthList =  UapUserInfoAPI.getDataAuthList(request);
        ResponseDto<Object> allDataAuthListResponse = UapManagementClientUtil.dataAuthSearchPage(tenantId, request);
        if (!allDataAuthListResponse.isFlag() || allDataAuthListResponse.getData() == null) {
            log.error("연결호출예외 {}", allDataAuthListResponse.getMessage());
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "데이터 권한 조회 실패: " + allDataAuthListResponse.getMessage());
        }
        LinkedHashMap<String, Object> data = (LinkedHashMap<String, Object>) allDataAuthListResponse.getData();
        if (null == data || null == data.get("list")) {
            log.error("데이터 권한 조회 실패: 반환 데이터가 비어 있습니다");
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "데이터 권한 조회 실패: 반환 데이터가 비어 있습니다");
        }
        List<Object> dataAuthObjectList = (List<Object>) data.get("list");
        List<DataAuthorityWithDimDictDto> dataAuthList = new ArrayList<>();
        for (Object dataAuthObj : dataAuthObjectList) {
            if (null == dataAuthObj) {
                continue;
            }
            LinkedHashMap<String, Object> dataAuthMap = (LinkedHashMap<String, Object>) dataAuthObj;
            DataAuthorityWithDimDictDto dataAuthorityWithDimDictDto = new DataAuthorityWithDimDictDto();
            dataAuthorityWithDimDictDto.setDataAuthId((String) dataAuthMap.get("id"));
            dataAuthorityWithDimDictDto.setDataAuthName((String) dataAuthMap.get("name"));
            dataAuthList.add(dataAuthorityWithDimDictDto);
        }
        // 가져오기 선택의데이터권한
        List<DataAuthorityWithDimDictDto> checkedDataAuthList =
                UapManagementClientUtil.queryDataAuthByRoleId(tenantId, roleId, request);
        Boolean haveChecked = false;
        if (!CollectionUtils.isEmpty(checkedDataAuthList)) {
            if (checkedDataAuthList.size() > 1) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "일모듈지정완료다중개데이터권한");
            }
            // 가져오기 전지정의모듈권한
            DataAuthorityWithDimDictDto checkedDataAuth = checkedDataAuthList.get(0);
            for (DataAuthorityWithDimDictDto dataAuth : dataAuthList) {
                if (dataAuth.getDataAuthId().equals(checkedDataAuth.getDataAuthId())) {
                    dataAuth.setChecked(true);
                    haveChecked = true;
                } else {
                    dataAuth.setChecked(false);
                }
            }
        }
        if (Boolean.FALSE.equals(haveChecked)) {
            // 전체
            for (DataAuthorityWithDimDictDto dataAuth : dataAuthList) {
                if ("전체".equals(dataAuth.getDataAuthName())) {
                    dataAuth.setChecked(true);
                }
            }
        }
        List<com.iflytek.rpa.auth.core.entity.DataAuthorityWithDimDictDto> dataAuthorityWithDimDictDtos =
                dataAuthorityWithDimDictDtoMapper.fromUapDataAuthorityWithDimDictDtos(dataAuthList);
        return AppResponse.success(dataAuthorityWithDimDictDtos);
    }

    @Override
    public AppResponse<String> bindDataAuth(BindRoleDataAuthDto bindRoleDataAuthDto, HttpServletRequest request) {
        if (StringUtils.isBlank(bindRoleDataAuthDto.getRoleId())
                || StringUtils.isBlank(bindRoleDataAuthDto.getDataAuthId())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        String roleId = bindRoleDataAuthDto.getRoleId();
        ManagementClient managementClient = UapManagementClientUtil.getManagementClient(request);
        // 결과가새데이터권한id아니요이면필요해제지정
        // 조회역할전지정의데이터권한
        String tenantId = UapUserInfoAPI.getTenantId(request);
        List<DataAuthorityWithDimDictDto> oldBindDataAuthList =
                UapManagementClientUtil.queryDataAuthByRoleId(tenantId, roleId, request);
        if (!CollectionUtils.isEmpty(oldBindDataAuthList)) {
            // 빈요소
            oldBindDataAuthList.removeIf(Objects::isNull);
            if (oldBindDataAuthList.size() > 1) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "일모듈지정완료다중개데이터권한");
            }

            // 가져오기 전지정의데이터권한
            DataAuthorityWithDimDictDto oldDataAuth = oldBindDataAuthList.get(0);
            if (null != oldDataAuth) {
                if (!bindRoleDataAuthDto.getDataAuthId().equals(oldDataAuth.getDataAuthId())) {
                    // 결과가변수변경완료데이터권한, 이면해제의데이터권한
                    BindDataAuthDto bindDataAuthDto = new BindDataAuthDto();
                    bindDataAuthDto.setRoleId(roleId);
                    bindDataAuthDto.setDataAuthIdList(Collections.singletonList(oldDataAuth.getDataAuthId()));
                    ResponseDto<Object> unbindResponse = managementClient.unbindRoleDataAuth(bindDataAuthDto);
                    if (!unbindResponse.isFlag()) {
                        return AppResponse.error(ErrorCodeEnum.E_SERVICE, unbindResponse.getMessage());
                    }
                } else {
                    // 새데이터권한, 설명있음변수변경, 직선연결반환
                    return AppResponse.success("저장성공");
                }
            }
        }
        // 데이터권한있음변수변경, 지정새데이터권한
        BindDataAuthDto bindDataAuthDto = new BindDataAuthDto();
        bindDataAuthDto.setRoleId(roleId);
        bindDataAuthDto.setDataAuthIdList(Collections.singletonList(bindRoleDataAuthDto.getDataAuthId()));
        ResponseDto<Object> bindResponse = managementClient.bindRoleDataAuth(bindDataAuthDto);
        if (!bindResponse.isFlag()) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, bindResponse.getMessage());
        }
        return AppResponse.success("저장성공");
    }

    @Override
    public AppResponse<List<Authority>> getAuthorityListByRoleId(
            String tenantId, String roleId, HttpServletRequest request) {
        List<UapAuthority> uapAuthorities = ClientManagementAPI.queryAuthorityListByRoleId(tenantId, roleId);
        List<Authority> authorities = authorityMapper.fromUapAuthorities(uapAuthorities);
        return AppResponse.success(authorities);
    }
}