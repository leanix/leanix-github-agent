package net.leanix.githubagent.config

import feign.ResponseInterceptor
import net.leanix.githubagent.interceptor.RateLimitResponseInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FeignClientConfig {

    @Bean
    fun rateLimitResponseInterceptor(): ResponseInterceptor {
        return RateLimitResponseInterceptor()
    }
}
