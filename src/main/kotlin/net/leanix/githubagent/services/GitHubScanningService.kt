package net.leanix.githubagent.services

import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.dto.Installation
import net.leanix.githubagent.dto.ItemResponse
import net.leanix.githubagent.dto.ManifestFileDTO
import net.leanix.githubagent.dto.ManifestFilesDTO
import net.leanix.githubagent.dto.Organization
import net.leanix.githubagent.dto.OrganizationDto
import net.leanix.githubagent.dto.RateLimitType
import net.leanix.githubagent.dto.RepositoryDto
import net.leanix.githubagent.exceptions.GitHubAppInsufficientPermissionsException
import net.leanix.githubagent.exceptions.JwtTokenNotFound
import net.leanix.githubagent.exceptions.ManifestFileNotFoundException
import net.leanix.githubagent.handler.RateLimitHandler
import net.leanix.githubagent.shared.INSTALLATION_LABEL
import net.leanix.githubagent.shared.MANIFEST_FILE_NAME
import net.leanix.githubagent.shared.fileNameMatchRegex
import net.leanix.githubagent.shared.generateFullPath
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GitHubScanningService(
    private val gitHubClient: GitHubClient,
    private val cachingService: CachingService,
    private val webSocketService: WebSocketService,
    private val gitHubGraphQLService: GitHubGraphQLService,
    private val gitHubAuthenticationService: GitHubAuthenticationService,
    private val syncLogService: SyncLogService,
    private val rateLimitHandler: RateLimitHandler,
    private val gitHubEnterpriseService: GitHubEnterpriseService,
    private val gitHubAPIService: GitHubAPIService,
) {

    private val logger = LoggerFactory.getLogger(GitHubScanningService::class.java)

    fun scanGitHubResources() {
        val jwtToken = cachingService.get("jwtToken") ?: throw JwtTokenNotFound()
        val installations = getInstallations(jwtToken.toString())
        fetchAndSendOrganisationsData(installations)
        installations.forEach { installation ->
            kotlin.runCatching {
                gitHubEnterpriseService.validateEnabledPermissionsAndEvents(
                    INSTALLATION_LABEL,
                    installation.permissions,
                    installation.events
                )
                fetchAndSendRepositoriesData(installation)
                    .forEach { repository ->
                        fetchManifestFilesAndSend(installation, repository)
                    }
                syncLogService.sendInfoLog("Finished initial full scan for organization ${installation.account.login}.")
            }.onFailure {
                val message = "Failed to scan organization ${installation.account.login}."
                when (it) {
                    is GitHubAppInsufficientPermissionsException -> {
                        syncLogService.sendErrorLog("$message ${it.message}")
                        logger.error("$message ${it.message}")
                    }
                    else -> {
                        syncLogService.sendErrorLog(message)
                        logger.error(message, it)
                    }
                }
            }
        }
        syncLogService.sendInfoLog("Finished full scan for all available organizations.")
    }

    private fun getInstallations(jwtToken: String): List<Installation> {
        val installations = gitHubAPIService.getPaginatedInstallations(jwtToken)
        gitHubAuthenticationService.generateAndCacheInstallationTokens(installations, jwtToken)
        return installations
    }

    fun fetchAndSendOrganisationsData(
        installations: List<Installation>
    ) {
        if (installations.isEmpty()) {
            logger.warn("No installations found, please install the GitHub App on an organization")
            webSocketService.sendMessage("${cachingService.get("runId")}/organizations", emptyList<Organization>())
            return
        }
        val installationToken = cachingService.get("installationToken:${installations.first().id}")
        val organizations = rateLimitHandler.executeWithRateLimitHandler(RateLimitType.REST) {
            gitHubAPIService.getPaginatedOrganizations(installationToken.toString())
                .map { organization ->
                    if (installations.find { it.account.login == organization.login } != null) {
                        OrganizationDto(organization.id, organization.login, true)
                    } else {
                        OrganizationDto(organization.id, organization.login, false)
                    }
                }
        }
        logger.info("Sending organizations data")
        syncLogService.sendInfoLog(
            "The connector found ${organizations.filter { it.installed }.size} " +
                "organizations with GitHub application installed, out of possible ${organizations.size}."
        )
        webSocketService.sendMessage("${cachingService.get("runId")}/organizations", organizations)
    }

    fun fetchAndSendRepositoriesData(installation: Installation): List<RepositoryDto> {
        val installationToken = cachingService.get("installationToken:${installation.id}").toString()
        var cursor: String? = null
        var totalRepos = 0
        var page = 1
        val repositories = mutableListOf<RepositoryDto>()
        do {
            val repositoriesPage = rateLimitHandler.executeWithRateLimitHandler(RateLimitType.GRAPHQL) {
                gitHubGraphQLService.getRepositories(
                    token = installationToken,
                    cursor = cursor
                )
            }
            webSocketService.sendMessage(
                "${cachingService.get("runId")}/repositories",
                repositoriesPage.repositories.filter { !it.archived }
            )
            repositories.addAll(repositoriesPage.repositories)
            cursor = repositoriesPage.cursor
            totalRepos += repositoriesPage.repositories.size
            page++
        } while (repositoriesPage.hasNextPage)
        logger.info("Fetched $totalRepos repositories data from organisation ${installation.account.login}")
        return repositories
    }

    fun fetchManifestFilesAndSend(installation: Installation, repository: RepositoryDto) {
        if (repository.archived) return
        val manifestFiles = fetchManifestFiles(installation, repository.name).getOrThrow()
        val manifestFilesContents = fetchManifestContents(
            installation,
            manifestFiles,
            repository.name,
            repository.defaultBranch
        ).getOrThrow()

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
        val manifestFiles = rateLimitHandler.executeWithRateLimitHandler(RateLimitType.SEARCH) {
            gitHubClient.searchManifestFiles(
                "Bearer $installationToken",
                "" +
                    "repo:${installation.account.login}/$repositoryName filename:$MANIFEST_FILE_NAME"
            )
        }
        manifestFiles.items.filter { it.name.lowercase() == MANIFEST_FILE_NAME }
    }
    private fun fetchManifestContents(
        installation: Installation,
        items: List<ItemResponse>,
        repositoryName: String,
        defaultBranch: String?
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
                    path = generateFullPath(defaultBranch, fileNameMatchRegex.replace(manifestFile.path, "")),
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
