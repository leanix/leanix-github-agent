package net.leanix.githubagent.services

import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.dto.Installation
import net.leanix.githubagent.dto.OrganizationDto
import net.leanix.githubagent.exceptions.JwtTokenNotFound
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GitHubScanningService(
    private val gitHubClient: GitHubClient,
    private val cachingService: CachingService,
    private val webSocketService: WebSocketService
) {
    private val logger = LoggerFactory.getLogger(GitHubScanningService::class.java)

    fun scanGitHubResources() {
        runCatching {
            val jwtToken = cachingService.get("jwtToken") ?: throw JwtTokenNotFound()
            val installations = getInstallations(jwtToken.toString())
            val organizations = generateOrganizations(installations)
            webSocketService.sendMessage("/app/ghe/organizations", organizations)
        }.onFailure {
            logger.error("Error while scanning GitHub resources")
            throw it
        }
    }

    private fun getInstallations(jwtToken: String): List<Installation> {
        val installations = gitHubClient.getInstallations("Bearer $jwtToken")
        generateAndCacheInstallationTokens(installations, jwtToken)
        return installations
    }

    private fun generateAndCacheInstallationTokens(
        installations: List<Installation>,
        jwtToken: String
    ) {
        installations.forEach { installation ->
            val installationToken = gitHubClient.createInstallationToken(installation.id, "Bearer $jwtToken").token
            cachingService.set("installationToken:${installation.id}", installationToken, 3600L)
        }
    }

    private fun generateOrganizations(
        installations: List<Installation>
    ): List<OrganizationDto> {
        val installationToken = cachingService.get("installationToken:${installations.first().id}")
        val organizations = gitHubClient.getOrganizations("Bearer $installationToken")
        return organizations.map { organization ->
            if (installations.find { it.account.login == organization.login } != null) {
                OrganizationDto(organization.id, organization.login, true)
            } else {
                OrganizationDto(organization.id, organization.login, false)
            }
        }
    }
}
