package com.iflytek.rpa.conf;

import static com.iflytek.rpa.conf.ApiContext.*;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfiguration implements RequestInterceptor {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // 완료있음의서비스 header
            template.header(CURRENT_USER_ID_KEY, request.getHeader(CURRENT_USER_ID_KEY));
            template.header(CURRENT_TENANT_ID_KEY, request.getHeader(CURRENT_TENANT_ID_KEY));
            template.header(CURRENT_TERMINAL_MAC_KEY, request.getHeader(CURRENT_TERMINAL_MAC_KEY));
            template.header(CURRENT_TERMINAL_NAME_KEY, request.getHeader(CURRENT_TERMINAL_NAME_KEY));

            // 인증닫기의 header
            String ssoSessionId = request.getHeader("ssoSessionId");
            if (ssoSessionId != null) {
                template.header("ssoSessionId", ssoSessionId);
            }

            String globalToken = request.getHeader("global-token");
            if (globalToken != null) {
                template.header("global-token", globalToken);
            }

            String accountId = request.getHeader("account_id");
            if (accountId != null) {
                template.header("account_id", accountId);
            }

            String authorization = request.getHeader("Authorization");
            if (authorization != null) {
                template.header("Authorization", authorization);
            }

            String xUserId = request.getHeader("X-User-Id");
            if (xUserId != null) {
                template.header("X-User-Id", xUserId);
            }

            String terminalType = request.getHeader("terminalType");
            if (terminalType != null) {
                template.header("terminalType", terminalType);
            }

            String appId = request.getHeader("appId");
            if (appId != null) {
                template.header("appId", appId);
            }

            String ipAddress = request.getHeader("ip-address");
            if (ipAddress != null) {
                template.header("ip-address", ipAddress);
            }

            //  Cookie(재필요: 사용 Session 인증)
            // 사용기존 Cookie header, 결과가찾을 수 없습니다이면에서 cookies 배열생성
            String cookieHeader = request.getHeader("Cookie");
            if (cookieHeader != null && cookieHeader.trim().length() > 0) {
                template.header("Cookie", cookieHeader);
            } else {
                // 결과가기존 Cookie header 찾을 수 없습니다, 에서 cookies 배열생성
                Cookie[] cookies = request.getCookies();
                if (cookies != null && cookies.length > 0) {
                    StringBuilder cookieBuilder = new StringBuilder();
                    for (Cookie cookie : cookies) {
                        if (cookieBuilder.length() > 0) {
                            cookieBuilder.append("; ");
                        }
                        cookieBuilder.append(cookie.getName()).append("=").append(cookie.getValue());
                    }
                    template.header("Cookie", cookieBuilder.toString());
                }
            }
        }
    }
}