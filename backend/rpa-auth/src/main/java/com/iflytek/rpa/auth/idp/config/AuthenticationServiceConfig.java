package com.iflytek.rpa.auth.idp.config;

import com.iflytek.rpa.auth.idp.AuthenticationService;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * 인증 서비스매칭유형
 * 근거매칭로드의 AuthenticationService 
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AuthenticationServiceConfig {

    private final AuthenticationProperties authProperties;

    @Autowired(required = false)
    private AuthenticationService authenticationService;

    @PostConstruct
    public void init() {
        if (authenticationService == null) {
            log.error("찾을 수 없는 매칭의 AuthenticationService !현재모듈방식: {}", authProperties.getDeploymentMode());
            throw new IllegalStateException("찾을 수 없는 매칭의 AuthenticationService , 확인하세요매칭: rpa.auth.deployment-mode="
                    + authProperties.getDeploymentMode());
        }

        log.info("=================================================");
        log.info("인증 서비스완료");
        log.info(
                "모듈방식: {} ({})",
                authProperties.getDeploymentModeEnum().getCode(),
                authProperties.getDeploymentModeEnum().getDescription());
        log.info("인증 서비스: {}", authenticationService.getClass().getSimpleName());
        log.info("=================================================");
    }
}