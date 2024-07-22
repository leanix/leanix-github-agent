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
    private val webSocketService: WebSocketService,
    private val gitHubGraphQLService: GitHubGraphQLService
) {
    private val logger = LoggerFactory.getLogger(GitHubScanningService::class.java)

    fun scanGitHubResources() {
        runCatching {
            val jwtToken = cachingService.get("jwtToken") ?: throw JwtTokenNotFound()
            val installations = getInstallations(jwtToken.toString())
            fetchAndBroadcastOrganisationsData(installations)
            installations.forEach { installation ->
                logger.info("Fetching repositories for organisation ${installation.account.login}")
                fetchAndBroadcastRepositoriesData(installation)
            }
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

    private fun fetchAndBroadcastOrganisationsData(
        installations: List<Installation>
    ) {
        val installationToken = cachingService.get("installationToken:${installations.first().id}")
        println(installationToken)
        val organizations = gitHubClient.getOrganizations("Bearer $installationToken")
            .map { organization ->
                if (installations.find { it.account.login == organization.login } != null) {
                    OrganizationDto(organization.id, organization.nodeId, organization.login, true)
                } else {
                    OrganizationDto(organization.id, organization.nodeId, organization.login, false)
                }
            }
        webSocketService.sendMessage("/app/ghe/organizations", organizations)
    }

    private fun fetchAndBroadcastRepositoriesData(installation: Installation) {
        val installationToken = cachingService.get("installationToken:${installation.id}").toString()
        var cursor: String? = null
        var totalRepos = 0
        var page = 1
        do {
            val repositoriesPage = gitHubGraphQLService.getRepositories(
                token = installationToken,
                cursor = cursor
            )
            webSocketService.sendMessage("/app/ghe/repositories", repositoriesPage.repositories)
            cursor = repositoriesPage.cursor
            totalRepos += repositoriesPage.repositories.size
            page++
        } while (repositoriesPage.hasNextPage)
        logger.info("Fetched $totalRepos repositories")
    }
}
