package net.leanix.githubagent.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "github-enterprise")
data class GitHubEnterpriseProperties(
    val baseUrl: String,
    val gitHubAppId: String,
    val pemFile: String,
    val manifestFileDirectory: String,
    val webhookSecret: String
)
