package com.posementor.posementorbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 업로드된 영상 경로
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");

        // 프레임 이미지 경로
        registry.addResourceHandler("/frames/**")
                .addResourceLocations("file:frames/");
    }
}
