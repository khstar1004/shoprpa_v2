package com.iflytek.rpa.auth.idp.config;

import com.iflytek.rpa.auth.core.entity.enums.DeploymentMode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 인증매칭속성
 */
@Data
@Component
@ConfigurationProperties(prefix = "rpa.auth")
public class AuthenticationProperties {

    /**
     * 모듈방식: saas | private-enterprise | private-uap | casdoor
     * 로 saas
     */
    private String deploymentMode = "casdoor";

    /**
     * 가져오기모듈방식
     */
    public DeploymentMode getDeploymentModeEnum() {
        return DeploymentMode.fromCode(deploymentMode);
    }
}