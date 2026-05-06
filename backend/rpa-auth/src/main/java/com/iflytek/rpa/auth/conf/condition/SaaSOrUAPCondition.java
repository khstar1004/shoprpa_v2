package com.iflytek.rpa.auth.conf.condition;

import com.iflytek.rpa.auth.core.entity.enums.DeploymentMode;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * 파일: 모듈방식로 saas 또는 private-uap 시반환 true
 * 결과가매칭실패, 로 saas 방식
 *
 * @author system
 */
public class SaaSOrUAPCondition implements Condition {

    private static final String DEPLOYMENT_MODE_PROPERTY = "rpa.auth.deployment-mode";
    private static final String DEFAULT_MODE = "saas";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String deploymentMode = context.getEnvironment().getProperty(DEPLOYMENT_MODE_PROPERTY);

        // 결과가매칭실패, 로 saas 방식(matchIfMissing = true)
        if (!StringUtils.hasText(deploymentMode)) {
            deploymentMode = DEFAULT_MODE;
        }

        // 여부로 saas 또는 private-uap 방식
        return DeploymentMode.SAAS.getCode().equalsIgnoreCase(deploymentMode)
                || DeploymentMode.PRIVATE_UAP.getCode().equalsIgnoreCase(deploymentMode);
    }
}