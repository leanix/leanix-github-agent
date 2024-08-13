package net.leanix.githubagent.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.leanix.githubagent.config.GitHubEnterpriseProperties
import net.leanix.githubagent.dto.ManifestFileAction
import net.leanix.githubagent.dto.ManifestFileUpdateDto
import net.leanix.githubagent.dto.PushEventCommit
import net.leanix.githubagent.dto.PushEventPayload
import net.leanix.githubagent.shared.ManifestFileName
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class WebhookEventService(
    private val webSocketService: WebSocketService,
    private val gitHubGraphQLService: GitHubGraphQLService,
    private val gitHubEnterpriseProperties: GitHubEnterpriseProperties,
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

        var installationToken = cachingService.get("installationToken:${pushEventPayload.installation.id}")?.toString()
        if (installationToken == null) {
            gitHubAuthenticationService.refreshTokens()
            installationToken = cachingService.get("installationToken:${pushEventPayload.installation.id}")?.toString()
            require(installationToken != null) { "Installation token not found/ expired" }
        }

        if (pushEventPayload.ref == "refs/heads/${pushEventPayload.repository.defaultBranch}") {
            handleManifestFileChanges(
                headCommit,
                repositoryFullName,
                owner,
                repositoryName,
                installationToken
            )
        }
    }

    @SuppressWarnings("LongParameterList")
    private fun handleManifestFileChanges(
        headCommit: PushEventCommit,
        repositoryFullName: String,
        owner: String,
        repositoryName: String,
        installationToken: String
    ) {
        val isYAMLFileUpdated =
            isManifestFileUpdated(
                headCommit,
                "${gitHubEnterpriseProperties.manifestFileDirectory}${ManifestFileName.YAML.fileName}"
            )
        val isYMLFileUpdated =
            isManifestFileUpdated(
                headCommit,
                "${gitHubEnterpriseProperties.manifestFileDirectory}${ManifestFileName.YML.fileName}"
            )

        // ignore updates on YML file if YAML file exists
        if (!isYAMLFileUpdated && isYMLFileUpdated) {
            val yamlFileContent = gitHubGraphQLService.getManifestFileContent(
                owner,
                repositoryName,
                "${gitHubEnterpriseProperties.manifestFileDirectory}${ManifestFileName.YAML.fileName}",
                installationToken
            )
            if (yamlFileContent != null) return
        }

        val manifestFilePath = if (isYAMLFileUpdated) {
            "${gitHubEnterpriseProperties.manifestFileDirectory}${ManifestFileName.YAML.fileName}"
        } else if (isYMLFileUpdated) {
            "${gitHubEnterpriseProperties.manifestFileDirectory}${ManifestFileName.YML.fileName}"
        } else {
            return
        }

        when (manifestFilePath) {
            in headCommit.added, in headCommit.modified -> {
                handleAddedOrModifiedManifestFile(
                    headCommit,
                    repositoryFullName,
                    owner,
                    repositoryName,
                    installationToken,
                    manifestFilePath
                )
            }
            in headCommit.removed -> {
                handleRemovedManifestFile(repositoryFullName)
            }
        }
    }

    private fun isManifestFileUpdated(headCommit: PushEventCommit, fileName: String): Boolean {
        return headCommit.added.any { it == fileName } ||
            headCommit.modified.any { it == fileName } ||
            headCommit.removed.any { it == fileName }
    }

    @SuppressWarnings("LongParameterList")
    private fun handleAddedOrModifiedManifestFile(
        headCommit: PushEventCommit,
        repositoryFullName: String,
        owner: String,
        repositoryName: String,
        installationToken: String,
        manifestFilePath: String
    ) {
        val action = if (manifestFilePath in headCommit.added) ManifestFileAction.ADDED else ManifestFileAction.MODIFIED
        logger.info("Manifest file $action in repository $repositoryFullName")
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
                fileContent
            )
        )
    }

    private fun handleRemovedManifestFile(repositoryFullName: String) {
        logger.info("Manifest file ${ManifestFileAction.REMOVED} from repository $repositoryFullName")
        webSocketService.sendMessage(
            "/events/manifestFile",
            ManifestFileUpdateDto(
                repositoryFullName,
                ManifestFileAction.REMOVED,
                null
            )
        )
    }
}
