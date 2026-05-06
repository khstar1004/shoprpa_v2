package com.iflytek.rpa.auth.core.service;

import com.iflytek.rpa.auth.core.entity.Resource;
import com.iflytek.rpa.auth.utils.AppResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 * 권한서비스
 */
public interface ResourceService {

    /**
     * 현재로그인사용자에서사용중의정보
     * @param request HTTP요청 
     * @return 목록
     */
    AppResponse<List<Resource>> getUserResourceList(HttpServletRequest request);
}