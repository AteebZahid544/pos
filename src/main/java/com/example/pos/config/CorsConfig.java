package com.example.pos.config;

import com.example.pos.util.Constant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList(
                "ACL", "CANCELUPLOAD", "CHECKIN", "CHECKOUT", "COPY", "DELETE", "GET",
                "HEAD", "LOCK", "MKCALENDAR", "MKCOL", "MOVE", "OPTIONS", "POST",
                "PROPFIND", "PROPPATCH", "PUT", "REPORT", "SEARCH", "UNCHECKOUT",
                "UNLOCK", "UPDATE", "VERSION-CONTROL"
        ));

        config.setAllowedHeaders(Arrays.asList(
                "Content-Type", "X-Requested-With", "accept", "Authorization", "Origin",
                "Access-Control-Request-Method", "Access-Control-Request-Headers","X-TENANTID"
        ));
        config.setAllowCredentials(false);
        config.setMaxAge(Constant.CORS_MAX_AGE);
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
