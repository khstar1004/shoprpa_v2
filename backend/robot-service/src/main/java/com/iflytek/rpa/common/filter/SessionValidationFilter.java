package com.iflytek.rpa.common.filter;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 통신경과호출인증 서비스의 session 검증연결, 복사사용 UAP 의인증결과.
 * session 있음직선연결행, 없음시를인증 서비스의기존반환프론트엔드.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class SessionValidationFilter extends OncePerRequestFilter {

    /**
     * 인증 서비스주소
     */
    @Value("${auth.base-url:http://localhost:10251}")
    private String authBaseUrl;

    /**
     * 검증 session 의경로,  /api/rpa-auth/check-session
     */
    @Value("${auth.check-session-path:/api/rpa-auth/check-session}")
    private String checkSessionPath;

    /**
     * 설치후의검증주소
     */
    private String checkSessionUrl;

    /**
     * 및 UAP 보관일의이름단일, 명령중이면아니요검증 session.
     */
    @Value("${uap.session-filter-exclude:}")
    private String sessionFilterExclude;

    private final RestTemplate restTemplate;

    public SessionValidationFilter(RestTemplateBuilder restTemplateBuilder) {
        // 금지 302, 방법 auth 의기존반환프론트엔드
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(3000);

        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> requestFactory)
                // 아니요출력예외, 보관상태코드호출방법관리
                .errorHandler(new DefaultResponseErrorHandler() {
                    @Override
                    public boolean hasError(ClientHttpResponse response) throws IOException {
                        return false;
                    }
                })
                .build();
    }

    @javax.annotation.PostConstruct
    public void initCheckSessionUrl() {
        String base = StringUtils.removeEnd(authBaseUrl, "/");
        String path = StringUtils.prependIfMissing(checkSessionPath, "/");
        this.checkSessionUrl = base + path;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();

        // astron-agent연결사용API Key인증, 아니요필요session검증
        if (uri.contains("/astron-agent")) {
            return true;
        }

        if (uri.contains("/health")) {
            return true;
        }

        if (StringUtils.isBlank(sessionFilterExclude)) {
            return false;
        }
        List<String> excludeList = Arrays.stream(sessionFilterExclude.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        return excludeList.stream().anyMatch(uri::contains);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        HttpHeaders headers = new HttpHeaders();
        copyForwardHeaders(request, headers);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> authResponse;
        try {
            authResponse = restTemplate.exchange(checkSessionUrl, HttpMethod.GET, entity, byte[].class);
        } catch (Exception ex) {
            log.error("호출인증 서비스검증 session 실패", ex);
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "session check failed");
            return;
        }

        HttpStatus status = authResponse.getStatusCode();
        byte[] body = authResponse.getBody();

        // 결과가상태코드예2xx, 필요조회여부예재지정JSON또는빈까지
        if (status.is2xxSuccessful()) {
            // 조회여부예패키지 ret:302 의재지정JSON
            if (isRedirectJsonResponse(body)) {
                // 기존반환재지정JSON
                writeBackResponse(response, status.value(), authResponse.getHeaders(), body);
                return;
            }
            // 조회여부예빈까지
            if (isSpaceExpiredResponse(body)) {
                // 기존반환빈까지, 프론트엔드관리출력로그인
                writeBackResponse(response, status.value(), authResponse.getHeaders(), body);
                return;
            }
            // 조회여부예단일로그인실패(계정에서방법로그인)
            if (isSingleSignOnInvalidResponse(body)) {
                // 기존반환단일로그인실패, 프론트엔드관리출력로그인
                writeBackResponse(response, status.value(), authResponse.getHeaders(), body);
                return;
            }
            // 정상일반, 행
            filterChain.doFilter(request, response);
            return;
        }

        // 2xx상태코드, 기존반환
        writeBackResponse(response, status.value(), authResponse.getHeaders(), body);
    }

    private void copyForwardHeaders(HttpServletRequest request, HttpHeaders headers) {
        // 복사 Cookie, 사용복사사용 session
        String cookieHeader = request.getHeader(HttpHeaders.COOKIE);
        if (StringUtils.isNotBlank(cookieHeader)) {
            headers.add(HttpHeaders.COOKIE, cookieHeader);
        } else {
            Cookie[] cookies = request.getCookies();
            if (cookies != null && cookies.length > 0) {
                String cookieString = Arrays.stream(cookies)
                        .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                        .collect(Collectors.joining("; "));
                if (StringUtils.isNotBlank(cookieString)) {
                    headers.add(HttpHeaders.COOKIE, cookieString);
                }
            }
        }

        // 가능매개및인증의 header
        copyHeaderIfPresent(request, headers, "Authorization");
        copyHeaderIfPresent(request, headers, "X-User-Id");

        // 추가 x-requested-with header, 식별자로 AJAX 요청 
        headers.add("x-requested-with", "XMLHttpRequest");
    }

    private void copyHeaderIfPresent(HttpServletRequest request, HttpHeaders headers, String name) {
        String value = request.getHeader(name);
        if (StringUtils.isNotBlank(value)) {
            headers.add(name, value);
        }
    }

    /**
     * 조회여부예패키지 ret:302 의재지정JSON
     * @param body 문자배열
     * @return 결과가예재지정JSON반환true, 아니요이면반환false
     */
    private boolean isRedirectJsonResponse(byte[] body) {
        if (body == null || body.length == 0) {
            return false;
        }
        try {
            String bodyStr = new String(body, java.nio.charset.StandardCharsets.UTF_8).trim();
            // 조회여부패키지 "ret":302 또는 "ret": 302(빈격식)
            return bodyStr.contains("\"ret\":302") || bodyStr.contains("\"ret\": 302");
        } catch (Exception e) {
            log.debug("파싱실패, 아니요로재지정JSON", e);
            return false;
        }
    }

    /**
     * 조회여부예빈까지
     * @param body 문자배열
     * @return 결과가예빈까지반환true, 아니요이면반환false
     */
    private boolean isSpaceExpiredResponse(byte[] body) {
        if (body == null || body.length == 0) {
            return false;
        }
        try {
            String bodyStr = new String(body, java.nio.charset.StandardCharsets.UTF_8).trim();
            // 오류코드900005, 연결반환의오류코드형식로 "code":"900005"
            return bodyStr.contains("\"code\":\"900005\"");
        } catch (Exception e) {
            log.debug("파싱실패, 아니요비어 있습니다까지", e);
            return false;
        }
    }

    /**
     * 조회여부예단일로그인실패(계정에서방법로그인)
     * @param body 문자배열
     * @return 결과가예단일로그인실패반환true, 아니요이면반환false
     */
    private boolean isSingleSignOnInvalidResponse(byte[] body) {
        if (body == null || body.length == 0) {
            return false;
        }
        try {
            String bodyStr = new String(body, java.nio.charset.StandardCharsets.UTF_8).trim();
            // 오류코드900001(E_NOT_LOGIN), 패키지"방법로그인"의안내
            return bodyStr.contains("\"code\":\"900001\"") && (bodyStr.contains("방법로그인") || bodyStr.contains("완료실패"));
        } catch (Exception e) {
            log.debug("파싱실패, 아니요로단일로그인실패", e);
            return false;
        }
    }

    private void writeBackResponse(HttpServletResponse response, int status, HttpHeaders headers, byte[] body)
            throws IOException {
        response.setStatus(status);
        if (headers != null) {
            headers.forEach((name, values) -> {
                if (HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(name)
                        || HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
                    return;
                }
                for (String value : values) {
                    response.addHeader(name, value);
                }
            });
        }
        if (body != null && body.length > 0) {
            StreamUtils.copy(body, response.getOutputStream());
        }
    }
}
