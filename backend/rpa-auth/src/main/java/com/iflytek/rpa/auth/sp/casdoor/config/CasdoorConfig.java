package com.iflytek.rpa.auth.sp.casdoor.config;

import org.casbin.casdoor.config.CasdoorConfiguration;
import org.casbin.casdoor.service.GroupService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @desc: Casdoor매칭유형, 에서casdoor profile아래
 * @author: weilai <laiwei3@iflytek.com>
 * @create: 2025/12/11 10:24
 */
@Configuration
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "casdoor", matchIfMissing = true)
public class CasdoorConfig {

    /**
     * 생성Casdoor GroupService Bean
     * 방법Starter있음GroupService, 필요생성
     * 사용@ConditionalOnMissingBean확인아니요재복사생성
     */
    @Bean
    @ConditionalOnMissingBean
    public GroupService getCasdoorGroupService(CasdoorConfiguration config) {
        return new GroupService(config);
    }
}