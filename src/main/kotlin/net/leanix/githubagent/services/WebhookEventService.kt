package net.leanix.githubagent.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.dto.Installation
import net.leanix.githubagent.dto.InstallationEventPayload
import net.leanix.githubagent.dto.InstallationRepositoriesEventPayload
import net.leanix.githubagent.dto.InstallationRepositoriesRepository
import net.leanix.githubagent.dto.ManifestFileAction
import net.leanix.githubagent.dto.ManifestFileUpdateDto
import net.leanix.githubagent.dto.PushEventCommit
import net.leanix.githubagent.dto.PushEventPayload
import net.leanix.githubagent.dto.RateLimitType
import net.leanix.githubagent.dto.RepositoryDto
import net.leanix.githubagent.dto.toInstallation
import net.leanix.githubagent.exceptions.JwtTokenNotFound
import net.leanix.githubagent.handler.RateLimitHandler
import net.leanix.githubagent.shared.INSTALLATION_LABEL
import net.leanix.githubagent.shared.INSTALLATION_REPOSITORIES
import net.leanix.githubagent.shared.MANIFEST_FILE_NAME
import net.leanix.githubagent.shared.fileNameMatchRegex
import net.leanix.githubagent.shared.generateFullPath
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@SuppressWarnings("TooManyFunctions")
@Service
class WebhookEventService(
    private val webSocketService: WebSocketService,
    private val gitHubGraphQLService: GitHubGraphQLService,
    private val cachingService: CachingService,
    private val gitHubAuthenticationService: GitHubAuthenticationService,
    private val gitHubScanningService: GitHubScanningService,
    private val syncLogService: SyncLogService,
    @Value("\${webhookEventService.waitingTime}") private val waitingTime: Long,
    private val gitHubClient: GitHubClient,
    private val gitHubEnterpriseService: GitHubEnterpriseService,
    private val rateLimitHandler: RateLimitHandler,
) {

    private val logger = LoggerFactory.getLogger(WebhookEventService::class.java)
    private val objectMapper = jacksonObjectMapper()

    fun consumeWebhookEvent(eventType: String, payload: String) {
        when (eventType.uppercase()) {
            "PUSH" -> handlePushEvent(payload)
            "INSTALLATION" -> handleInstallationEvent(payload)
            INSTALLATION_REPOSITORIES -> handleInstallationRepositories(payload)
            else -> {
                logger.info("Sending event of type: $eventType")
                webSocketService.sendMessage("/events/$eventType", payload)
            }
        }
    }

    private fun handlePushEvent(payload: String) {
        val pushEventPayload: PushEventPayload = objectMapper.readValue(payload)
        val repositoryName = pushEventPayload.repository.name
        val repositoryFullName = pushEventPayload.repository.fullName
        val headCommit = pushEventPayload.headCommit
        val owner = pushEventPayload.repository.owner.name
        val defaultBranch = pushEventPayload.repository.defaultBranch

        val installationToken = gitHubAuthenticationService.getInstallationToken(pushEventPayload.installation.id)

        if (headCommit != null && pushEventPayload.ref == "refs/heads/$defaultBranch") {
            handleManifestFileChanges(
                defaultBranch,
                headCommit,
                repositoryFullName,
                owner,
                repositoryName,
                installationToken
            )
        }
    }

    private fun handleInstallationEvent(payload: String) {
        val installationEventPayload: InstallationEventPayload = objectMapper.readValue(payload)
        if (installationEventPayload.action == "created") {
            handleInstallationCreated(installationEventPayload)
        }
    }

    private fun handleInstallationCreated(installationEventPayload: InstallationEventPayload) {
        while (cachingService.get("runId") != null) {
            logger.info("A full scan is already in progress, waiting for it to finish.")
            Thread.sleep(waitingTime)
        }
        syncLogService.sendFullScanStart(installationEventPayload.installation.account.login)
        kotlin.runCatching {
            val jwtToken = cachingService.get("jwtToken") ?: throw JwtTokenNotFound()
            val installation = gitHubClient.getInstallation(
                installationEventPayload.installation.id.toLong(),
                "Bearer $jwtToken"
            )
            gitHubEnterpriseService.validateEnabledPermissionsAndEvents(
                INSTALLATION_LABEL,
                installation.permissions,
                installation.events
            )
            gitHubAuthenticationService.refreshTokens()
            gitHubScanningService.fetchAndSendOrganisationsData(listOf(installation))
            gitHubScanningService.fetchAndSendRepositoriesData(installation).forEach { repository ->
                gitHubScanningService.fetchManifestFilesAndSend(installation, repository)
            }
        }.onSuccess {
            syncLogService.sendFullScanSuccess()
        }.onFailure {
            syncLogService.sendFullScanFailure(it.message)
        }
    }

    private fun handleManifestFileChanges(
        defaultBranch: String,
        headCommit: PushEventCommit,
        repositoryFullName: String,
        owner: String,
        repositoryName: String,
        installationToken: String
    ) {
        val addedManifestFiles = headCommit.added.filter { isLeanixManifestFile(it.lowercase()) }
        val modifiedManifestFiles = headCommit.modified.filter { isLeanixManifestFile(it.lowercase()) }
        val removedManifestFiles = headCommit.removed.filter { isLeanixManifestFile(it.lowercase()) }

        addedManifestFiles.forEach { filePath ->
            handleAddedOrModifiedManifestFile(
                repositoryFullName,
                owner,
                repositoryName,
                installationToken,
                filePath,
                ManifestFileAction.ADDED,
                defaultBranch
            )
        }

        modifiedManifestFiles.forEach { filePath ->
            handleAddedOrModifiedManifestFile(
                repositoryFullName,
                owner,
                repositoryName,
                installationToken,
                filePath,
                ManifestFileAction.MODIFIED,
                defaultBranch
            )
        }

        removedManifestFiles.forEach { filePath ->
            handleRemovedManifestFile(repositoryFullName, filePath, defaultBranch)
        }
    }

    private fun isLeanixManifestFile(it: String) = it == MANIFEST_FILE_NAME || it.endsWith("/$MANIFEST_FILE_NAME")

    @SuppressWarnings("LongParameterList")
    private fun handleAddedOrModifiedManifestFile(
        repositoryFullName: String,
        owner: String,
        repositoryName: String,
        installationToken: String,
        manifestFilePath: String,
        action: ManifestFileAction,
        defaultBranch: String?
    ) {
        val location = getManifestFileLocation(manifestFilePath)

        logger.info("Manifest file {} in repository {} under {}", action, repositoryFullName, location)

        val fileContent = gitHubGraphQLService.getManifestFileContent(
            owner,
            repositoryName,
            manifestFilePath,
            installationToken
        )
        webSocketService.sendMessage(
            "/events/manifestFile",
            ManifestFileUpdateDto(
                repositoryFullName,
                action,
                fileContent,
                generateFullPath(defaultBranch, fileNameMatchRegex.replace(manifestFilePath, ""))
            )
        )
    }

    private fun handleRemovedManifestFile(
        repositoryFullName: String,
        manifestFilePath: String,
        defaultBranch: String?
    ) {
        val location = getManifestFileLocation(manifestFilePath)
        logger.info("Manifest file ${ManifestFileAction.REMOVED} from repository $repositoryFullName under $location")
        webSocketService.sendMessage(
            "/events/manifestFile",
            ManifestFileUpdateDto(
                repositoryFullName,
                ManifestFileAction.REMOVED,
                null,
                generateFullPath(defaultBranch, fileNameMatchRegex.replace(manifestFilePath, ""))
            )
        )
    }

    private fun getManifestFileLocation(manifestFilePath: String): String {
        return if (manifestFilePath.contains('/')) {
            "directory '/${manifestFilePath.substringBeforeLast('/')}'"
        } else {
            "root folder"
        }
    }

    private val handleInstallationRepositories: (String) -> Unit = { payload ->
        logger.info("Handling installation repositories event")
        val installationRepositoriesEventPayload: InstallationRepositoriesEventPayload = objectMapper.readValue(payload)
        val installation = installationRepositoriesEventPayload.installation
        val installationToken = gitHubAuthenticationService.getInstallationToken(installation.id)

        installationRepositoriesEventPayload.repositoriesAdded
            .forEach { repositoryAdded ->
                logger.info("Processing repository: ${repositoryAdded.fullName}")
                processRepository(installation.toInstallation(), repositoryAdded, installationToken)
            }
    }

    private fun processRepository(
        installation: Installation,
        repositoryAdded: InstallationRepositoriesRepository,
        installationToken: String
    ) {
        kotlin.runCatching {
            logger.info("Fetching repository details for: ${repositoryAdded.fullName}")
            val repository = rateLimitHandler.executeWithRateLimitHandler(RateLimitType.GRAPHQL) {
                gitHubGraphQLService.getRepository(
                    installation.account.login,
                    repositoryAdded.name,
                    installationToken
                )
            }
            if (repository == null) {
                logger.error("Failed to fetch repository details for: ${repositoryAdded.fullName}")
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
            logger.error("Failed to process repository event: ${repositoryAdded.fullName}", it)
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
