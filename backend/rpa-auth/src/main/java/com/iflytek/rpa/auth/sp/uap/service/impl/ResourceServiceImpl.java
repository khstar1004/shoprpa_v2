package com.iflytek.rpa.auth.sp.uap.service.impl;

import com.iflytek.rpa.auth.conf.condition.ConditionalOnSaaSOrUAP;
import com.iflytek.rpa.auth.core.entity.Resource;
import com.iflytek.rpa.auth.core.service.ResourceService;
import com.iflytek.rpa.auth.sp.uap.mapper.ResourceMapper;
import com.iflytek.rpa.auth.utils.AppResponse;
import com.iflytek.sec.uap.client.api.UapUserInfoAPI;
import com.iflytek.sec.uap.client.core.dto.resource.UapResource;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 권한서비스
 */
@Slf4j
@Service("resourceService")
@ConditionalOnSaaSOrUAP
public class ResourceServiceImpl implements ResourceService {

    @Autowired
    private ResourceMapper resourceMapper;

    @Override
    public AppResponse<List<Resource>> getUserResourceList(HttpServletRequest request) {
        try {
            List<UapResource> uapResources = UapUserInfoAPI.getUserResourceList(request);
            List<Resource> resources = resourceMapper.fromUapResources(uapResources);
            return AppResponse.success(resources);
        } catch (Exception e) {
            log.error("가져오기사용자목록실패", e);
            return AppResponse.error(
                    com.iflytek.rpa.auth.utils.ErrorCodeEnum.E_SERVICE, "가져오기사용자목록실패: " + e.getMessage());
        }
    }
}