package net.leanix.githubagent.interceptor

import net.leanix.githubagent.shared.RateLimitMonitor
import net.leanix.githubagent.shared.determineRateLimitType
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

class RateLimitInterceptor : ExchangeFilterFunction {

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
        return next.exchange(request).flatMap { response ->
            val headers = response.headers().asHttpHeaders()
            val rateLimitRemaining = headers["X-RateLimit-Remaining"]?.firstOrNull()?.toIntOrNull()
            val rateLimitReset = headers["X-RateLimit-Reset"]?.firstOrNull()?.toLongOrNull()

            if (rateLimitRemaining != null && rateLimitReset != null) {
                val rateLimitType = determineRateLimitType(request.url().toString())
                RateLimitMonitor.updateRateLimitInfo(rateLimitType, rateLimitRemaining, rateLimitReset)
            }
            Mono.just(response)
        }
    }
}
