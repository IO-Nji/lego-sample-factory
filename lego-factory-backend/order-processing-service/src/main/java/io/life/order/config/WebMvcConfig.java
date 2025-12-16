package io.life.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ensure actuator endpoints are not treated as static resources
        // Only configure static resources for actual static content
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}
