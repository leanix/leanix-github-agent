package net.leanix.githubagent.interceptor

import feign.InvocationContext
import feign.ResponseInterceptor
import net.leanix.githubagent.shared.RateLimitMonitor

class RateLimitResponseInterceptor : ResponseInterceptor {

    override fun intercept(
        invocationContext: InvocationContext,
        chain: ResponseInterceptor.Chain
    ): Any {
        val result = chain.next(invocationContext)

        val response = invocationContext.response()
        if (response != null) {
            val headers = response.headers().mapKeys { it.key.lowercase() }
            val rateLimitRemaining = headers["x-ratelimit-remaining"]?.firstOrNull()?.toIntOrNull()
            val rateLimitReset = headers["x-ratelimit-reset"]?.firstOrNull()?.toLongOrNull()

            if (rateLimitRemaining != null && rateLimitReset != null) {
                RateLimitMonitor.updateRateLimitInfo(rateLimitRemaining, rateLimitReset)
            }
        }

        return result
    }
}
