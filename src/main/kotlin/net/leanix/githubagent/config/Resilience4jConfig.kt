package net.leanix.githubagent.config

import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*

@Configuration
class Resilience4jConfig {

    private val logger = LoggerFactory.getLogger(Resilience4jConfig::class.java)

    @Bean
    fun retryRegistry(): RetryRegistry {
        val waitDuration = Duration.ofSeconds(20)
        val retryConfig = RetryConfig.custom<Any>()
            .maxAttempts(5)
            .waitDuration(waitDuration)
            .retryOnException { throwable ->
                logger.debug("Retrying due to exception: ${throwable.message}")
                true
            }
            .retryOnResult { false }
            .build()

        val registry = RetryRegistry.of(retryConfig)

        registry.retry("secondary_rate_limit").eventPublisher
            .onRetry { event ->
                val readableWaitTime = String.format(
                    Locale.getDefault(),
                    "%d minutes, %d seconds",
                    waitDuration.toMinutes(),
                    waitDuration.minus(waitDuration.toMinutes(), ChronoUnit.MINUTES).seconds
                )
                logger.info(
                    "Retrying call due to ${event.name}, attempt: ${event.numberOfRetryAttempts}, " +
                        "wait time: $readableWaitTime"
                )
            }
            .onError { event ->
                logger.error(
                    "Call failed due to ${event.name}, after attempts: ${event.numberOfRetryAttempts}, " +
                        "last exception: ${event.lastThrowable.message}"
                )
            }

        return registry
    }
}
