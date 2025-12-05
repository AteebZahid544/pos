package com.example.pos.config;

import com.example.pos.filter.TenantFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilter() {
        FilterRegistrationBean<TenantFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new TenantFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(1); // make sure it runs first
        return reg;
    }

}
