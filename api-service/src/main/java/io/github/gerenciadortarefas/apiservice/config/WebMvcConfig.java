package io.github.gerenciadortarefas.apiservice.config;

import io.github.gerenciadortarefas.apiservice.ratelimit.CreateTaskRateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CreateTaskRateLimitInterceptor createTaskRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(createTaskRateLimitInterceptor)
                .addPathPatterns("/api/v1/tasks");
    }
}
