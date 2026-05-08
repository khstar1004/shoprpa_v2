package com.iflytek.rpa.auth.blacklist.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.rpa.auth.blacklist.dto.BlacklistCacheDto;
import com.iflytek.rpa.auth.blacklist.exception.UserBlockedException;
import com.iflytek.rpa.auth.blacklist.service.BlackListService;
import com.iflytek.rpa.auth.utils.AppResponse;
import com.iflytek.rpa.auth.utils.ErrorCodeEnum;
import com.iflytek.sec.uap.client.api.UapUserInfoAPI;
import com.iflytek.sec.uap.client.core.dto.user.UapUser;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
// import org.springframework.stereotype.Component; // 현재는 BlacklistAspect가 로그인 경로 검사를 담당합니다.

/**
 * 사용자 차단 필터입니다.
 * 현재는 Spring 컴포넌트로 등록하지 않고, 로그인 경로 검사는 BlacklistAspect에서 처리합니다.
 *
 * @author system
 * @date 2025-12-16
 */
@Slf4j
// @Component
@Order(2)
@RequiredArgsConstructor
public class BlacklistFilter implements Filter {

    private final BlackListService blackListService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String[] EXCLUDE_PATHS = {
        "/login",
        "/logout",
        "/pre-authenticate",
        "/tenant/list",
        "/verification-code/send",
        "/register",
        "/password/set",
        "/user/exist",
        "/refresh-token",
        "/static/",
        "/public/",
        "/error",
        "/health",
        "/favicon.ico"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();

        if (isExcludePath(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            UapUser loginUser = UapUserInfoAPI.getLoginUser(httpRequest);

            if (loginUser != null) {
                String userId = loginUser.getId();

                BlacklistCacheDto blacklist = blackListService.isBlocked(userId);

                if (blacklist != null) {
                    log.warn(
                            "차단된 사용자의 요청을 차단합니다. userId: {}, username: {}, reason: {}",
                            userId,
                            loginUser.getLoginName(),
                            blacklist.getReason());

                    // blackListService.forceLogout(httpRequest, httpResponse);

                    sendBlockedResponse(httpResponse, blacklist);
                    return;
                }
            }

            chain.doFilter(request, response);

        } catch (Exception e) {
            log.error("사용자 차단 필터 실행 예외", e);
            chain.doFilter(request, response);
        }
    }

    private boolean isExcludePath(String requestURI) {
        for (String path : EXCLUDE_PATHS) {
            if (requestURI.contains(path)) {
                return true;
            }
        }
        return false;
    }

    private void sendBlockedResponse(HttpServletResponse response, BlacklistCacheDto blacklist) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        LocalDateTime endTime = blacklist.getEndTimeMillis() != null
                ? LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(blacklist.getEndTimeMillis()), java.time.ZoneId.systemDefault())
                : null;

        UserBlockedException exception = new UserBlockedException(
                blacklist.getUserId(),
                blacklist.getUsername(),
                blacklist.getReason(),
                endTime,
                blacklist.getRemainingSeconds());

        AppResponse<Object> errorResponse = AppResponse.error(ErrorCodeEnum.E_NO_POWER, exception.getMessage());

        PrintWriter writer = response.getWriter();
        writer.write(objectMapper.writeValueAsString(errorResponse));
        writer.flush();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("사용자 차단 필터 초기화");
    }

    @Override
    public void destroy() {
        log.info("사용자 차단 필터 종료");
    }
}
