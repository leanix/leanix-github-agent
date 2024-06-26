package net.leanix.githubagent.services

import jakarta.annotation.PostConstruct
import net.leanix.githubagent.config.GithubEnterpriseProperties
import org.springframework.stereotype.Service

@Service
class CachingService(
    private val githubEnterpriseProperties: GithubEnterpriseProperties
) {
    private val cache = HashMap<String, String?>()

    @PostConstruct
    private fun init() {
        cache["baseUrl"] = githubEnterpriseProperties.baseUrl
        cache["githubAppId"] = githubEnterpriseProperties.githubAppId
    }

    fun set(key: String, value: String) {
        cache[key] = value
    }

    fun get(key: String): String? {
        return cache[key]
    }
}
