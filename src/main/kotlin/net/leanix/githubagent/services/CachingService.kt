package net.leanix.githubagent.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import jakarta.annotation.PostConstruct
import net.leanix.githubagent.config.GitHubEnterpriseProperties
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class CachingService(
    private val gitHubEnterpriseProperties: GitHubEnterpriseProperties
) {

    data class CacheValue(val value: Any, val expiry: Long?)

    private val cache: Cache<String, CacheValue> = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfter(object : Expiry<String, CacheValue> {
            override fun expireAfterCreate(
                key: String,
                value: CacheValue,
                currentTime: Long
            ): Long {
                return TimeUnit.MILLISECONDS.toNanos(value.expiry ?: Long.MAX_VALUE)
            }

            override fun expireAfterUpdate(
                key: String,
                value: CacheValue,
                currentTime: Long,
                currentDuration: Long
            ): Long {
                return TimeUnit.MILLISECONDS.toNanos(value.expiry ?: Long.MAX_VALUE)
            }

            override fun expireAfterRead(
                key: String,
                value: CacheValue,
                currentTime: Long,
                currentDuration: Long
            ): Long {
                return currentDuration
            }
        })
        .build()

    fun set(key: String, value: Any, expiry: Long?) {
        cache.put(key, CacheValue(value, expiry))
    }

    fun get(key: String): Any? {
        return cache.getIfPresent(key)?.value
    }

    fun remove(key: String) {
        cache.invalidate(key)
    }

    @PostConstruct
    @Suppress("UnusedPrivateMember")
    private fun init() {
        set("baseUrl", gitHubEnterpriseProperties.baseUrl, null)
        set("githubAppId", gitHubEnterpriseProperties.gitHubAppId, null)
    }
}
