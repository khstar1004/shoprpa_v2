package com.iflytek.rpa.conf;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();

        //        // 요청 의페이지대대후,  true조정돌아가기까지, false 계속요청   false
        //        paginationInterceptor.setOverflow(true);
        //        // 대단일제한제어수,  500 , -1 아니요제한제어
        //        paginationInterceptor.setLimit(100);

        return paginationInterceptor;
    }
}