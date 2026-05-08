package com.iflytek.rpa.common.filter;

import com.iflytek.rpa.base.entity.dto.ResourceConfigDto;
import com.iflytek.rpa.base.service.TenantResourceService;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.utils.response.AppResponse;
import java.io.IOException;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 테넌트매칭금액검증Filter
 * 요청 , 근거URL매칭, 행공가능권한및수제한제어의검증
 */
@Slf4j
@Component
@Order() // 단계낮음
public class TenantResourceFilter extends OncePerRequestFilter {

    private static final String QUOTA_ATTRIBUTE_PREFIX = "QUOTA_";
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    private TenantResourceService tenantResourceService;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    /**
     * 이름단일경로, 아니요필요행검증
     */
    private static final String[] EXCLUDE_PATHS = {
        "/login", "/logout", "/error", "/health", "/actuator", "/swagger", "/v2/api-docs", "/favicon.ico"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // OPTIONS요청 아니요
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        // 조회이름단일
        for (String excludePath : EXCLUDE_PATHS) {
            if (uri.contains(excludePath)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 가져오기테넌트ID
            String tenantId = getTenantId(request);
            if (StringUtils.isBlank(tenantId)) {
                log.debug("불가가져오기테넌트ID, 건너뛰기검증");
                filterChain.doFilter(request, response);
                return;
            }

            // 가져오기요청 URI
            String requestUri = request.getRequestURI();

            // 가져오기테넌트매칭
            Map<String, ResourceConfigDto> resourceConfigMap = tenantResourceService.getTenantResourceConfig(tenantId);
            if (resourceConfigMap.isEmpty()) {
                log.debug("테넌트매칭비어 있습니다, 건너뛰기검증, tenantId: {}", tenantId);
                filterChain.doFilter(request, response);
                return;
            }

            // 조회매칭의
            ResourceConfigDto matchedResource = findMatchingResource(requestUri, resourceConfigMap);
            if (matchedResource == null) {
                // 있음매칭의, 직선연결행
                filterChain.doFilter(request, response);
                return;
            }

            // 가져오기 코드
            String resourceCode = getResourceCodeByConfig(matchedResource, resourceConfigMap);
            if (StringUtils.isBlank(resourceCode)) {
                log.warn("불가가져오기 코드, 건너뛰기검증");
                filterChain.doFilter(request, response);
                return;
            }

            // 단계검증: 조회단계여부있음
            if (!checkParentResource(resourceCode, matchedResource, resourceConfigMap)) {
                log.warn("단계없음, 방문, tenantId: {}, resourceCode: {}", tenantId, resourceCode);
                sendForbiddenResponse(response, "단계사용할 수 없습니다");
                return;
            }

            // 권한검증
            String resourceType = matchedResource.getType();
            if ("SWITCH".equals(resourceType)) {
                // 열기닫기유형: final로0이면반환403
                Integer finalValue = matchedResource.getFinalValue();
                if (finalValue == null || finalValue == 0) {
                    log.warn("열기닫기완료닫기, 방문, tenantId: {}, resourceCode: {}", tenantId, resourceCode);
                    sendForbiddenResponse(response, "해당공가능사용할 수 없습니다");
                    return;
                }
            }
            //            else if ("QUOTA".equals(resourceType)) {
            //                // 매칭금액유형: 를final제한제어값저장입력request속성, 후서비스사용
            //                Integer finalValue = matchedResource.getFinalValue();
            //                if (finalValue != null) {
            //                    String attributeName = QUOTA_ATTRIBUTE_PREFIX + resourceCode;
            //                    request.setAttribute(attributeName, finalValue);
            //                    log.debug("매칭금액제한제어, tenantId: {}, resourceCode: {}, quota: {}", tenantId, resourceCode,
            // finalValue);
            //                }
            //            }

            // 검증통신경과, 계속실행
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("검증경과중발송예외", e);
            // 발송예외시, 로완료아니요시스템정상일반실행, 가능으로선택행또는반환오류
            // 선택행, 기록오류로그
            filterChain.doFilter(request, response);
        }
    }

    /**
     * 가져오기테넌트ID
     */
    private String getTenantId(HttpServletRequest request) {
        try {
            // 시도에서인증 서비스가져오기테넌트ID
            AppResponse<String> response = rpaAuthFeign.getTenantId();
            if (response != null && response.ok() && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.debug("에서인증 서비스가져오기테넌트ID실패", e);
        }

        // 결과가에서인증 서비스가져오기실패, 가능으로시도에서header또는session중가져오기
        String tenantId = request.getHeader("tenantId");
        if (StringUtils.isNotBlank(tenantId)) {
            return tenantId;
        }

        return null;
    }

    /**
     * 조회매칭의
     * 지원다중매칭방식: 
     * 1. 직선연결매칭경로
     * 2. 매칭경로후(제거/api/robot대기전)
     * 3. 지원AntPathMatcher통신매칭기호
     */
    private ResourceConfigDto findMatchingResource(
            String requestUri, Map<String, ResourceConfigDto> resourceConfigMap) {
        for (Map.Entry<String, ResourceConfigDto> entry : resourceConfigMap.entrySet()) {
            ResourceConfigDto config = entry.getValue();
            if (config.getUrls() != null && !config.getUrls().isEmpty()) {
                for (String urlPattern : config.getUrls()) {
                    // 1. 직선연결매칭경로
                    if (pathMatcher.match(urlPattern, requestUri)) {
                        log.debug(
                                "매칭까지(경로), requestUri: {}, urlPattern: {}, resourceCode: {}",
                                requestUri,
                                urlPattern,
                                entry.getKey());
                        return config;
                    }

                    // 2. 결과가urlPattern아니요으로/열기 , 시도매칭경로후
                    if (!urlPattern.startsWith("/")) {
                        // 에서requestUri중가져오기경로모듈분행매칭
                        String normalizedPattern = "/" + urlPattern;
                        if (pathMatcher.match(normalizedPattern, requestUri)) {
                            log.debug(
                                    "매칭까지(경로), requestUri: {}, urlPattern: {}, resourceCode: {}",
                                    requestUri,
                                    normalizedPattern,
                                    entry.getKey());
                            return config;
                        }
                    }

                    // 3. 시도에서requestUri중가져오기경로후행매칭
                    // 예: /api/robot/market-invite/generate-invite-link -> /market-invite/generate-invite-link
                    String pathSuffix = extractPathSuffix(requestUri);
                    if (pathSuffix != null) {
                        // 매칭경로방식
                        if (pathMatcher.match(urlPattern, pathSuffix)) {
                            log.debug(
                                    "매칭까지(경로후), requestUri: {}, pathSuffix: {}, urlPattern: {}, resourceCode: {}",
                                    requestUri,
                                    pathSuffix,
                                    urlPattern,
                                    entry.getKey());
                            return config;
                        }
                        // 결과가urlPattern아니요으로/열기 , 시도매칭
                        if (!urlPattern.startsWith("/")) {
                            String normalizedPattern = "/" + urlPattern;
                            if (pathMatcher.match(normalizedPattern, pathSuffix)) {
                                log.debug(
                                        "매칭까지(경로후), requestUri: {}, pathSuffix: {}, urlPattern: {}, resourceCode: {}",
                                        requestUri,
                                        pathSuffix,
                                        normalizedPattern,
                                        entry.getKey());
                                return config;
                            }
                        }
                    }

                    // 4. 지원패키지매칭(결과가urlPattern예경로의일모듈분)
                    if (requestUri.contains(urlPattern) || requestUri.endsWith(urlPattern)) {
                        log.debug(
                                "매칭까지(패키지매칭), requestUri: {}, urlPattern: {}, resourceCode: {}",
                                requestUri,
                                urlPattern,
                                entry.getKey());
                        return config;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 에서URI중가져오기경로후
     * 예: /api/robot/market-invite/generate-invite-link -> /market-invite/generate-invite-link
     * /api/v1/design/create -> /design/create
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

    /**
     * 근거매칭가져오기 코드
     */
    private String getResourceCodeByConfig(ResourceConfigDto config, Map<String, ResourceConfigDto> resourceConfigMap) {
        for (Map.Entry<String, ResourceConfigDto> entry : resourceConfigMap.entrySet()) {
            if (entry.getValue() == config) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 조회단계여부있음
     * 결과가있음단계, 이면조회단계의final값(SWITCH유형, final로1)
     */
    private boolean checkParentResource(
            String resourceCode, ResourceConfigDto resourceConfig, Map<String, ResourceConfigDto> resourceConfigMap) {
        String parentCode = resourceConfig.getParent();
        if (StringUtils.isBlank(parentCode)) {
            // 있음단계, 직선연결반환true
            return true;
        }

        ResourceConfigDto parentConfig = resourceConfigMap.get(parentCode);
        if (parentConfig == null) {
            log.warn("단계매칭찾을 수 없습니다, parentCode: {}", parentCode);
            return false;
        }

        // 조회단계의단계
        if (!checkParentResource(parentCode, parentConfig, resourceConfigMap)) {
            return false;
        }

        // 조회단계의final값
        Integer parentFinalValue = parentConfig.getFinalValue();
        String parentType = parentConfig.getType();

        if ("SWITCH".equals(parentType)) {
            // SWITCH유형, final로1있음
            return parentFinalValue != null && parentFinalValue == 1;
        } else if ("QUOTA".equals(parentType)) {
            // QUOTA유형, final대0있음
            return parentFinalValue != null && parentFinalValue > 0;
        }

        return true;
    }

    /**
     * 전송403 Forbidden
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json;charset=UTF-8");
        String jsonResponse = String.format("{\"code\":\"403\",\"message\":\"%s\",\"data\":null}", message);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
