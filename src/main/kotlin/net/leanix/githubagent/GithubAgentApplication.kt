package net.leanix.githubagent

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients(value = ["net.leanix.githubagent.client"])
@EnableConfigurationProperties(value = [net.leanix.githubagent.config.GithubEnterpriseProperties::class])
class GithubAgentApplication

fun main() {
    runApplication<GithubAgentApplication>()
}
