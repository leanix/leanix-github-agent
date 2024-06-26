package net.leanix.githubagent.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "github-enterprise")
data class GithubEnterpriseProperties(
    val baseUrl: String,
    val githubAppId: String,
    val pemFile: String,
)
