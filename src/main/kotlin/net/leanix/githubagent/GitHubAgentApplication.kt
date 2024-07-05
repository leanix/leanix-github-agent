package net.leanix.githubagent

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties(value = [net.leanix.githubagent.config.GitHubEnterpriseProperties::class])
class GitHubAgentApplication

fun main() {
    runApplication<GitHubAgentApplication>()
}
