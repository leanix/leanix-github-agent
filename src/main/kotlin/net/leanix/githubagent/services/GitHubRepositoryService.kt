package net.leanix.githubagent.services

import net.leanix.githubagent.dto.Installation
import net.leanix.githubagent.dto.ManifestFileAction
import net.leanix.githubagent.dto.ManifestFileUpdateDto
import net.leanix.githubagent.dto.RateLimitType
import net.leanix.githubagent.dto.RepositoryDto
import net.leanix.githubagent.handler.RateLimitHandler
import net.leanix.githubagent.shared.fileNameMatchRegex
import net.leanix.githubagent.shared.generateFullPath
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class GitHubRepositoryService(
    @Lazy @Autowired
    private val webSocketService: WebSocketService,
    private val gitHubGraphQLService: GitHubGraphQLService,
    @Lazy @Autowired
    private val gitHubScanningService: GitHubScanningService,
    @Lazy @Autowired
    private val rateLimitHandler: RateLimitHandler,
) {
    private val logger = LoggerFactory.getLogger(GitHubRepositoryService::class.java)

    fun fetchAndSendRepositoryAndManifest(
        installation: Installation,
        repositoryName: String,
        repositoryFullName: String,
        installationToken: String
    ) {
        kotlin.runCatching {
            logger.info("Fetching repository details for: $repositoryFullName")
            val repository = rateLimitHandler.executeWithRateLimitHandler(RateLimitType.GRAPHQL) {
                gitHubGraphQLService.getRepository(
                    installation.account.login,
                    repositoryName,
                    installationToken
                )
            }
            if (repository == null) {
                logger.error("Failed to fetch repository details for: $repositoryFullName")
                return
            }
            if (repository.archived) {
                logger.info("Repository ${repository.fullName} is archived, skipping.")
                return
            }
            logger.info("Sending repository details for: ${repository.fullName}")
            webSocketService.sendMessage("/events/repository", repository)
            fetchAndSendManifestFiles(installation, repository)
        }.onFailure {
            logger.error("Failed to process repository event: $repositoryFullName", it)
        }
    }

    private fun fetchAndSendManifestFiles(installation: Installation, repositoryAdded: RepositoryDto) {
        logger.info("Fetching manifest files for repository: ${repositoryAdded.fullName}")
        val manifestFiles = gitHubScanningService.fetchManifestFiles(installation, repositoryAdded.name).getOrThrow()
        val manifestContents = gitHubScanningService.fetchManifestContents(
            installation,
            manifestFiles,
            repositoryAdded.name,
            repositoryAdded.defaultBranch
        ).getOrThrow()

        if (manifestContents.isEmpty()) {
            logger.info("No manifest files found for repository: ${repositoryAdded.fullName}")
            return
        }
        manifestContents.forEach {
            logger.info("Sending manifest file content for: ${it.path} in repository: ${repositoryAdded.fullName}")
            webSocketService.sendMessage(
                "/events/manifestFile",
                ManifestFileUpdateDto(
                    repositoryAdded.fullName,
                    ManifestFileAction.ADDED,
                    it.content,
                    generateFullPath(repositoryAdded.defaultBranch, fileNameMatchRegex.replace(it.path, ""))
                )
            )
        }
    }
}
