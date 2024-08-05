package net.leanix.githubagent.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.leanix.githubagent.config.GitHubEnterpriseProperties
import net.leanix.githubagent.dto.ManifestFileAction
import net.leanix.githubagent.dto.ManifestFileUpdateDto
import net.leanix.githubagent.dto.PushEventPayload
import net.leanix.githubagent.services.GitHubGraphQLService.Companion.MANIFEST_FILE_NAME
import net.leanix.githubagent.shared.TOPIC_PREFIX
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class WebhookService(
    private val webSocketService: WebSocketService,
    private val gitHubGraphQLService: GitHubGraphQLService,
    private val gitHubEnterpriseProperties: GitHubEnterpriseProperties,
    private val cachingService: CachingService
) {

    private val logger = LoggerFactory.getLogger(WebhookService::class.java)
    private val objectMapper = jacksonObjectMapper()

    fun consumeWebhookEvent(eventType: String, payload: String) {
        when (eventType.uppercase()) {
            "PUSH" -> handlePushEvent(payload)
            else -> webSocketService.sendMessage("$TOPIC_PREFIX/events/other", payload)
        }
    }

    private fun handlePushEvent(payload: String) {
        val pushEventPayload: PushEventPayload = objectMapper.readValue(payload)
        val ref = pushEventPayload.ref
        val repositoryName = pushEventPayload.repository.name
        val repositoryFullName = pushEventPayload.repository.fullName
        val headCommit = pushEventPayload.headCommit
        val organizationName = pushEventPayload.repository.owner.name

        val installationToken = cachingService.get("installationToken:${pushEventPayload.installation.id}")?.toString()
            ?: throw IllegalArgumentException("Installation token not found/ expired")
        // TODO refresh token if expired

        if (ref == "refs/heads/${pushEventPayload.repository.defaultBranch}") {
            when {
                MANIFEST_FILE_NAME in headCommit.added -> {
                    logger.info("Manifest file added to repository $repositoryFullName")
                    val fileContent = getFileContent(organizationName, repositoryName, installationToken)
                    sendManifestData(repositoryFullName, ManifestFileAction.ADDED, fileContent)
                }
                MANIFEST_FILE_NAME in headCommit.modified -> {
                    logger.info("Manifest file modified in repository $repositoryFullName")
                    val fileContent = getFileContent(organizationName, repositoryName, installationToken)
                    sendManifestData(repositoryFullName, ManifestFileAction.MODIFIED, fileContent)
                }
                MANIFEST_FILE_NAME in headCommit.removed -> {
                    logger.info("Manifest file removed from repository $repositoryFullName")
                    sendManifestData(repositoryFullName, ManifestFileAction.REMOVED, null)
                }
            }
        }
    }

    private fun getFileContent(organizationName: String, repositoryName: String, token: String): String? {
        return gitHubGraphQLService.getFileContent(
            owner = organizationName,
            repositoryName,
            "HEAD:${gitHubEnterpriseProperties.manifestFileDirectory}$MANIFEST_FILE_NAME",
            token
        )
    }

    private fun sendManifestData(repositoryFullName: String, action: ManifestFileAction, manifestContent: String?) {
        logger.info("Sending manifest file update event for repository $repositoryFullName")
        webSocketService.sendMessage(
            "$TOPIC_PREFIX/events/manifestFile",
            ManifestFileUpdateDto(repositoryFullName, action, manifestContent)
        )
    }
}
