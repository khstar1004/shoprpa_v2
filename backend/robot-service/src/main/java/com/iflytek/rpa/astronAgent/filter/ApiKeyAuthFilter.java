package com.iflytek.rpa.astronAgent.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * API Key인증필터링기기
 * 사용astronAgent연결의단일API Key인증
 */
@Slf4j
@Component
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    @Value("${astron.agent.api.key:}")
    private String validApiKey;

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String ASTRON_AGENT_PATH_PREFIX = "/astron-agent";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        // 가져오기경로후, 제거context-path전(예/api/robot)
        String pathSuffix = extractPathSuffix(requestPath);

        // astron-agent경로 행API Key인증(지요소전및아니요전의경로)
        if (pathSuffix != null && pathSuffix.startsWith(ASTRON_AGENT_PATH_PREFIX)) {
            String apiKey = request.getHeader(API_KEY_HEADER);

            // 결과가매칭완료API Key, 이면행인증
            if (StringUtils.isNotBlank(validApiKey)) {
                if (StringUtils.isBlank(apiKey) || !validApiKey.equals(apiKey)) {
                    log.warn("API Key 인증 실패, 요청 경로: {}", requestPath);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"API Key 인증 실패\"}");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 에서URI중가져오기경로후, 제거context-path전
     * 예: /api/robot/astron-agent/copy-robot -> /astron-agent/copy-robot
     * /astron-agent/copy-robot -> /astron-agent/copy-robot
     */
    private String extractPathSuffix(String requestUri) {
        if (StringUtils.isBlank(requestUri)) {
            return null;
        }

        // 제거일반의API전
        String[] prefixes = {"/api/robot/", "/api/v1/", "/api/", "/robot/"};
        for (String prefix : prefixes) {
            if (requestUri.startsWith(prefix)) {
                String suffix = requestUri.substring(prefix.length());
                // 확인으로/열기 
                if (!suffix.startsWith("/")) {
                    suffix = "/" + suffix;
                }
                return suffix;
            }
        }

        // 결과가있음매칭까지전, 반환기존경로
        return requestUri;
    }
}
