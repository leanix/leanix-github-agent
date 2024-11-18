package net.leanix.githubagent.services

import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.dto.Installation
import net.leanix.githubagent.dto.ItemResponse
import net.leanix.githubagent.dto.ManifestFileDTO
import net.leanix.githubagent.dto.ManifestFilesDTO
import net.leanix.githubagent.dto.Organization
import net.leanix.githubagent.dto.OrganizationDto
import net.leanix.githubagent.dto.RepositoryDto
import net.leanix.githubagent.exceptions.JwtTokenNotFound
import net.leanix.githubagent.exceptions.ManifestFileNotFoundException
import net.leanix.githubagent.shared.MANIFEST_FILE_NAME
import net.leanix.githubagent.shared.fileNameMatchRegex
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

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
        val jwtToken = cachingService.get("jwtToken") ?: throw JwtTokenNotFound()
        val installations = getInstallations(jwtToken.toString())
        fetchAndSendOrganisationsData(installations)
        installations.forEach { installation ->
            fetchAndSendRepositoriesData(installation)
                .forEach { repository ->
                    fetchManifestFilesAndSend(installation, repository)
                }
            syncLogService.sendInfoLog("Finished initial full scan for organization ${installation.account.login}.")
        }
        syncLogService.sendInfoLog("Finished full scan for all available organizations.")
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
        syncLogService.sendInfoLog(
            "The connector found ${organizations.filter { it.installed }.size} " +
                "organizations with GitHub application installed, out of possible ${organizations.size}."
        )
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
                "repo:${installation.account.login}/$repositoryName filename:$MANIFEST_FILE_NAME"
        )
    }
    private fun fetchManifestContents(
        installation: Installation,
        items: List<ItemResponse>,
        repositoryName: String
    ) = runCatching {
        val installationToken = cachingService.get("installationToken:${installation.id}").toString()
        syncLogService.sendInfoLog("Scanning repository $repositoryName for manifest files.")
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
                syncLogService.sendInfoLog(
                    "Fetched manifest file '${manifestFile.path}' from repository '$repositoryName'."
                )
                ManifestFileDTO(
                    path = fileNameMatchRegex.replace(manifestFile.path, ""),
                    content = content
                )
            } else {
                throw ManifestFileNotFoundException()
            }
        }.also {
            syncLogService.sendInfoLog("Found $numOfManifestFilesFound manifest files in repository $repositoryName.")
        }
    }
}
