package net.leanix.githubagent.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.leanix.githubagent.config.GitHubEnterpriseProperties
import net.leanix.githubagent.dto.ManifestFileAction
import net.leanix.githubagent.dto.ManifestFileUpdateDto
import net.leanix.githubagent.dto.PushEventPayload
import net.leanix.githubagent.shared.MANIFEST_FILE_NAME
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class WebhookService(
    private val webSocketService: WebSocketService,
    private val gitHubGraphQLService: GitHubGraphQLService,
    private val gitHubEnterpriseProperties: GitHubEnterpriseProperties,
    private val cachingService: CachingService,
    private val gitHubAuthenticationService: GitHubAuthenticationService
) {

    private val logger = LoggerFactory.getLogger(WebhookService::class.java)
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
        val organizationName = pushEventPayload.repository.owner.name

        var installationToken = cachingService.get("installationToken:${pushEventPayload.installation.id}")?.toString()
        if (installationToken == null) {
            gitHubAuthenticationService.refreshTokens()
            installationToken = cachingService.get("installationToken:${pushEventPayload.installation.id}")?.toString()
            require(installationToken != null) { "Installation token not found/ expired" }
        }

        if (pushEventPayload.ref == "refs/heads/${pushEventPayload.repository.defaultBranch}") {
            when {
                MANIFEST_FILE_NAME in headCommit.added -> {
                    logger.info("Manifest file added to repository $repositoryFullName")
                    val fileContent = getManifestFileContent(organizationName, repositoryName, installationToken)
                    sendManifestData(repositoryFullName, ManifestFileAction.ADDED, fileContent)
                }
                MANIFEST_FILE_NAME in headCommit.modified -> {
                    logger.info("Manifest file modified in repository $repositoryFullName")
                    val fileContent = getManifestFileContent(organizationName, repositoryName, installationToken)
                    sendManifestData(repositoryFullName, ManifestFileAction.MODIFIED, fileContent)
                }
                MANIFEST_FILE_NAME in headCommit.removed -> {
                    logger.info("Manifest file removed from repository $repositoryFullName")
                    sendManifestData(repositoryFullName, ManifestFileAction.REMOVED, null)
                }
            }
        }
    }

    private fun getManifestFileContent(organizationName: String, repositoryName: String, token: String): String {
        return gitHubGraphQLService.getManifestFileContent(
            owner = organizationName,
            repositoryName,
            "HEAD:${gitHubEnterpriseProperties.manifestFileDirectory}$MANIFEST_FILE_NAME",
            token
        )
    }

    private fun sendManifestData(repositoryFullName: String, action: ManifestFileAction, manifestContent: String?) {
        logger.info("Sending manifest file update event for repository $repositoryFullName")
        webSocketService.sendMessage(
            "/events/manifestFile",
            ManifestFileUpdateDto(repositoryFullName, action, manifestContent)
        )
    }
}
