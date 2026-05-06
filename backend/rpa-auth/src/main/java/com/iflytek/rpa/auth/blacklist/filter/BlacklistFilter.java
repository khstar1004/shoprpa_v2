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
// import org.springframework.stereotype.Component; // 완료사용 안 함, 아니요사용

/**
 * 이름단일필터링기기
 * 완료사용 안 함: 및 BlacklistAspect 공가능재복사
 * 이름단일조회시스템일 BlacklistAspect 에서 /login-status 연결관리
 *
 * @author system
 * @date 2025-12-16
 */
@Slf4j
// @Component  // 완료사용 안 함, 아니요사용
@Order(2)
@RequiredArgsConstructor
public class BlacklistFilter implements Filter {

    private final BlackListService blackListService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 아니요필요의경로
     */
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
        "/favicon.ico"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();

        // 조회여부예아니요필요의경로
        if (isExcludePath(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // 가져오기현재로그인사용자
            UapUser loginUser = UapUserInfoAPI.getLoginUser(httpRequest);

            if (loginUser != null) {
                String userId = loginUser.getId();

                // 조회사용자여부에서이름단일중
                BlacklistCacheDto blacklist = blackListService.isBlocked(userId);

                if (blacklist != null) {
                    // 사용자에서이름단일중, 요청 
                    log.warn(
                            "이름단일사용자시도방문시스템, userId: {}, username: {}, reason: {}",
                            userId,
                            loginUser.getLoginName(),
                            blacklist.getReason());

                    // 강함제어비고판매(Filter 완료사용 안 함, 코드아니요실행)
                    // 비고: Filter 완료사용 안 함, 비고판매 BlacklistAspect 관리
                    // blackListService.forceLogout(httpRequest, httpResponse);

                    // 반환오류 
                    sendBlockedResponse(httpResponse, blacklist);
                    return;
                }
            }

            // 계속관리요청 
            chain.doFilter(request, response);

        } catch (Exception e) {
            log.error("이름단일필터링기기예외", e);
            // 예외시행, 정상일반서비스
            chain.doFilter(request, response);
        }
    }

    /**
     * 조회여부예정렬제거경로
     */
    private boolean isExcludePath(String requestURI) {
        for (String path : EXCLUDE_PATHS) {
            if (requestURI.contains(path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 전송
     */
    private void sendBlockedResponse(HttpServletResponse response, BlacklistCacheDto blacklist) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        // 를시간변환로 LocalDateTime
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
        log.info("이름단일필터링기기");
    }

    @Override
    public void destroy() {
        log.info("이름단일필터링기기판매");
    }
}