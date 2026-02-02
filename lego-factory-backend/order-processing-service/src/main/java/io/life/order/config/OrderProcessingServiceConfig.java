package io.life.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.Collections;

/**
 * Configuration class for the Order Processing Service.
 * Sets up beans and configurations for REST communication, async processing, etc.
 */
@Configuration
@EnableAsync
public class OrderProcessingServiceConfig {

    /**
     * RestTemplate bean for making HTTP requests to other microservices.
     * Configured with:
     * - Apache HttpClient for PATCH support (required for SimAL task updates)
     * - Interceptor to forward JWT authentication headers
     */
    @Bean
    public RestTemplate restTemplate() {
        // Use HttpComponentsClientHttpRequestFactory for PATCH support
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setConnectionRequestTimeout(5000);
        
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setInterceptors(Collections.singletonList(new ClientHttpRequestInterceptor() {
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                // Get current HTTP request context
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest currentRequest = attributes.getRequest();
                    
                    // Forward JWT token if present
                    String authHeader = currentRequest.getHeader("Authorization");
                    if (authHeader != null && !authHeader.isEmpty()) {
                        request.getHeaders().set("Authorization", authHeader);
                    }
                    
                    // Forward user headers from API Gateway
                    String userHeader = currentRequest.getHeader("X-Authenticated-User");
                    if (userHeader != null && !userHeader.isEmpty()) {
                        request.getHeaders().set("X-Authenticated-User", userHeader);
                    }
                    
                    String roleHeader = currentRequest.getHeader("X-Authenticated-Role");
                    if (roleHeader != null && !roleHeader.isEmpty()) {
                        request.getHeaders().set("X-Authenticated-Role", roleHeader);
                    }
                }
                
                return execution.execute(request, body);
            }
        }));
        return restTemplate;
    }

    /**
     * Async executor for processing long-running tasks
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("order-processing-");
        executor.initialize();
        return executor;
    }
}
