package net.leanix.githubagent.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.leanix.githubagent.dto.ManifestFileAction
import net.leanix.githubagent.dto.ManifestFileUpdateDto
import net.leanix.githubagent.dto.PushEventCommit
import net.leanix.githubagent.dto.PushEventPayload
import net.leanix.githubagent.shared.MANIFEST_FILE_NAME
import net.leanix.githubagent.shared.fileNameMatchRegex
import net.leanix.githubagent.shared.generateFullPath
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class WebhookEventService(
    private val webSocketService: WebSocketService,
    private val gitHubGraphQLService: GitHubGraphQLService,
    private val cachingService: CachingService,
    private val gitHubAuthenticationService: GitHubAuthenticationService
) {

    private val logger = LoggerFactory.getLogger(WebhookEventService::class.java)
    private val objectMapper = jacksonObjectMapper()

    fun consumeWebhookEvent(eventType: String, payload: String) {
        when (eventType.uppercase()) {
            "PUSH" -> handlePushEvent(payload)
            else -> {
                logger.info("Sending event of type: $eventType")
                webSocketService.sendMessage("/events/other", payload)
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

        val installationToken = getInstallationToken(pushEventPayload.installation.id)

        if (pushEventPayload.ref == "refs/heads/$defaultBranch") {
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

    private fun handleManifestFileChanges(
        defaultBranch: String,
        headCommit: PushEventCommit,
        repositoryFullName: String,
        owner: String,
        repositoryName: String,
        installationToken: String
    ) {
        val addedManifestFiles = headCommit.added.filter { it.contains(MANIFEST_FILE_NAME) }
        val modifiedManifestFiles = headCommit.modified.filter { it.contains(MANIFEST_FILE_NAME) }
        val removedManifestFiles = headCommit.removed.filter { it.contains(MANIFEST_FILE_NAME) }

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

    private fun getInstallationToken(installationId: Int): String {
        var installationToken = cachingService.get("installationToken:$installationId")?.toString()
        if (installationToken == null) {
            gitHubAuthenticationService.refreshTokens()
            installationToken = cachingService.get("installationToken:$installationId")?.toString()
            require(installationToken != null) { "Installation token not found/ expired" }
        }
        return installationToken
    }

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
}
