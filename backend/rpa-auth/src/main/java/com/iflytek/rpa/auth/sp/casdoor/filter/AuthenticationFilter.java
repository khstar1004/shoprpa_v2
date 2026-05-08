package com.iflytek.rpa.auth.sp.casdoor.filter;

import com.iflytek.rpa.auth.sp.casdoor.utils.ResponseUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @desc: 인증필터링기기, Spring Security의인증공가능
 * @author: weilai <laiwei3@iflytek.com>
 * @create: 2025/12/11
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "casdoor", matchIfMissing = true)
public class AuthenticationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    /**
     * 열기단말목록(아니요필요인증의경로)
     */
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/rpa-auth/pre-authenticate",
            "/api/rpa-auth/tenant/list",
            "/api/rpa-auth/login",
            "/api/rpa-auth/verification-code/send",
            "/api/rpa-auth/register",
            "/api/rpa-auth/password/set",
            "/api/rpa-auth/logout",
            "/api/rpa-auth/health",
            "/api/rpa-auth/login-status",
            "/api/rpa-auth/tenant/getTenantId",
            "/api/rpa-auth/user/info",
            "/api/rpa-auth/dept/current/levelCode",
            "/api/rpa-auth/refresh-token",
            "/api/rpa-auth/user/search/name",
            "/api/rpa-auth/user/history");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();

        // 조회여부예열기경로
        if (isPublicPath(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        // 조회사용자여부완료로그인
        javax.servlet.http.HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            logger.debug("로그인되지 않았습니다사용자시도방문보관: {}", requestURI);
            ResponseUtils.fail(httpResponse, "unauthorized");
            return;
        }

        org.casbin.casdoor.entity.User user = (org.casbin.casdoor.entity.User) session.getAttribute("user");
        if (user == null) {
            logger.debug("session중없음사용자 정보, 방문: {}", requestURI);
            ResponseUtils.fail(httpResponse, "unauthorized");
            return;
        }

        // 사용자완료로그인, 계속관리요청 
        chain.doFilter(request, response);
    }

    /**
     * 조회요청 경로여부예열기경로
     *
     * @param requestURI 요청 URI
     * @return 결과가예열기경로반환true, 아니요이면반환false
     */
    private boolean isPublicPath(String requestURI) {
        if (requestURI == null || requestURI.isEmpty()) {
            return false;
        }

        // 제거조회매개변수
        String path = requestURI.split("\\?")[0];

        // 조회여부매칭열기경로
        for (String publicPath : PUBLIC_PATHS) {
            if (path.equals(publicPath) || path.startsWith(publicPath + "/")) {
                return true;
            }
        }

        // 경로
        if (path.startsWith("/static/")
                || path.startsWith("/public/")
                || path.startsWith("/error")
                || path.equals("/favicon.ico")) {
            return true;
        }

        return false;
    }
}
