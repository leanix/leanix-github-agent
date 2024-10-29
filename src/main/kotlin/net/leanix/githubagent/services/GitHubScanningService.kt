package net.leanix.githubagent.services

import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.dto.Installation
import net.leanix.githubagent.dto.ItemResponse
import net.leanix.githubagent.dto.LogLevel
import net.leanix.githubagent.dto.ManifestFileDTO
import net.leanix.githubagent.dto.ManifestFilesDTO
import net.leanix.githubagent.dto.Organization
import net.leanix.githubagent.dto.OrganizationDto
import net.leanix.githubagent.dto.RepositoryDto
import net.leanix.githubagent.dto.Trigger
import net.leanix.githubagent.exceptions.JwtTokenNotFound
import net.leanix.githubagent.exceptions.ManifestFileNotFoundException
import net.leanix.githubagent.shared.ManifestFileName
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GitHubScanningService(
    private val gitHubClient: GitHubClient,
    private val cachingService: CachingService,
    private val webSocketService: WebSocketService,
    private val gitHubGraphQLService: GitHubGraphQLService,
    private val gitHubAuthenticationService: GitHubAuthenticationService,
    private val syncLogService: SyncLogService
) {

    private val logger = LoggerFactory.getLogger(GitHubScanningService::class.java)

    fun scanGitHubResources() {
        cachingService.set("runId", UUID.randomUUID(), null)
        runCatching {
            syncLogService.sendSyncLog(
                trigger = Trigger.START_FULL_SYNC,
                logLevel = LogLevel.INFO,
            )
            val jwtToken = cachingService.get("jwtToken") ?: throw JwtTokenNotFound()
            val installations = getInstallations(jwtToken.toString())
            fetchAndSendOrganisationsData(installations)
            installations.forEach { installation ->
                fetchAndSendRepositoriesData(installation)
                    .forEach { repository ->
                        fetchManifestFilesAndSend(installation, repository)
                    }
            }
            syncLogService.sendSyncLog(
                trigger = Trigger.FINISH_FULL_SYNC,
                logLevel = LogLevel.INFO,
            )
        }.onFailure {
            val message = "Error while scanning GitHub resources"
            syncLogService.sendSyncLog(
                trigger = Trigger.FINISH_FULL_SYNC,
                logLevel = LogLevel.ERROR,
                message = message
            )
            cachingService.remove("runId")
            logger.error(message)
            throw it
        }
    }

    private fun getInstallations(jwtToken: String): List<Installation> {
        val installations = gitHubClient.getInstallations("Bearer $jwtToken")
        gitHubAuthenticationService.generateAndCacheInstallationTokens(installations, jwtToken)
        return installations
    }

    private fun fetchAndSendOrganisationsData(
        installations: List<Installation>
    ) {
        if (installations.isEmpty()) {
            logger.warn("No installations found, please install the GitHub App on an organization")
            webSocketService.sendMessage("${cachingService.get("runId")}/organizations", emptyList<Organization>())
            return
        }
        val installationToken = cachingService.get("installationToken:${installations.first().id}")
        val organizations = gitHubClient.getOrganizations("Bearer $installationToken")
            .map { organization ->
                if (installations.find { it.account.login == organization.login } != null) {
                    OrganizationDto(organization.id, organization.login, true)
                } else {
                    OrganizationDto(organization.id, organization.login, false)
                }
            }
        logger.info("Sending organizations data")
        syncLogService.sendInfoLog("The connector found ${organizations.size} available organizations.")
        webSocketService.sendMessage("${cachingService.get("runId")}/organizations", organizations)
    }

    private fun fetchAndSendRepositoriesData(installation: Installation): List<RepositoryDto> {
        val installationToken = cachingService.get("installationToken:${installation.id}").toString()
        var cursor: String? = null
        var totalRepos = 0
        var page = 1
        val repositories = mutableListOf<RepositoryDto>()
        do {
            val repositoriesPage = gitHubGraphQLService.getRepositories(
                token = installationToken,
                cursor = cursor
            )
            webSocketService.sendMessage(
                "${cachingService.get("runId")}/repositories",
                repositoriesPage.repositories
            )
            repositories.addAll(repositoriesPage.repositories)
            cursor = repositoriesPage.cursor
            totalRepos += repositoriesPage.repositories.size
            page++
        } while (repositoriesPage.hasNextPage)
        logger.info("Fetched $totalRepos repositories data from organisation ${installation.account.login}")
        return repositories
    }

    private fun fetchManifestFilesAndSend(installation: Installation, repository: RepositoryDto) {
        val manifestFiles = fetchManifestFiles(installation, repository.name).getOrThrow().items
        val manifestFilesContents = fetchManifestContents(installation, manifestFiles, repository.name).getOrThrow()

        webSocketService.sendMessage(
            "${cachingService.get("runId")}/manifestFiles",
            ManifestFilesDTO(
                repositoryId = repository.id,
                repositoryFullName = repository.fullName,
                manifestFiles = manifestFilesContents,
            )
        )
    }

    private fun fetchManifestFiles(installation: Installation, repositoryName: String) = runCatching {
        val installationToken = cachingService.get("installationToken:${installation.id}").toString()
        gitHubClient.searchManifestFiles(
            "Bearer $installationToken",
            "" +
                "repo:${installation.account.login}/$repositoryName filename:${ManifestFileName.YAML.fileName}"
        )
    }
    private fun fetchManifestContents(
        installation: Installation,
        items: List<ItemResponse>,
        repositoryName: String
    ) = runCatching {
        val installationToken = cachingService.get("installationToken:${installation.id}").toString()
        syncLogService.sendInfoLog("Scanning repository $repositoryName for manifest files")
        var numOfManifestFilesFound = 0
        items.map { manifestFile ->
            val content = gitHubGraphQLService.getManifestFileContent(
                owner = installation.account.login,
                repositoryName = repositoryName,
                filePath = manifestFile.path,
                token = installationToken
            )
            if (content != null) {
                numOfManifestFilesFound++
                syncLogService.sendInfoLog("Fetched manifest file ${manifestFile.path} from repository $repositoryName")
                ManifestFileDTO(
                    path = manifestFile.path.replace("/${ManifestFileName.YAML.fileName}", ""),
                    content = content
                )
            } else {
                throw ManifestFileNotFoundException()
            }
        }.also {
            syncLogService.sendInfoLog("Found $numOfManifestFilesFound manifest files in repository $repositoryName")
        }
    }
}
