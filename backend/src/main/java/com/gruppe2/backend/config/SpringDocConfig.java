package com.gruppe2.backend.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    @Bean
    public OpenApiCustomizer removeExtraneousHttpMethods() {
        return openApi -> {
            openApi.getPaths().values().forEach(pathItem -> {
                pathItem.setOptions(null);
                pathItem.setHead(null);
                pathItem.setPatch(null);
            });
        };
    }
}
