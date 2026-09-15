package cl.duoc.bankxyz.migracion.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(BffSecurityProperties.class)
public class BffWebMvcConfig implements WebMvcConfigurer {

    private final BffTokenInterceptor bffTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(bffTokenInterceptor)
                .addPathPatterns("/api/bff/**");
    }
}
