package net.leanix.githubagent.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.leanix.githubagent.config.GitHubEnterpriseProperties
import net.leanix.githubagent.dto.ManifestFileAction
import net.leanix.githubagent.dto.ManifestFileUpdateDto
import net.leanix.githubagent.dto.PushEventPayload
import net.leanix.githubagent.services.GitHubGraphQLService.Companion.MANIFEST_FILE_NAME
import net.leanix.githubagent.shared.TOPIC_PREFIX
import org.springframework.stereotype.Service

@Service
class WebhookService(
    private val webSocketService: WebSocketService,
    private val gitHubGraphQLService: GitHubGraphQLService,
    private val gitHubEnterpriseProperties: GitHubEnterpriseProperties,
    private val cachingService: CachingService
) {
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
        val repositoryFullName = pushEventPayload.repository.fullName
        val headCommit = pushEventPayload.headCommit

        val installationToken = cachingService.get("installationToken:${pushEventPayload.installation.id}")?.toString()
            ?: throw IllegalArgumentException("Installation token not found/ expired")
        // TODO refresh token if expired

        if (ref == "refs/heads/${pushEventPayload.repository.defaultBranch}") {
            when {
                MANIFEST_FILE_NAME in headCommit.added -> {
                    val fileContent = getFileContent(repositoryFullName, installationToken)
                    sendManifestData(repositoryFullName, ManifestFileAction.ADDED, fileContent)
                }
                MANIFEST_FILE_NAME in headCommit.modified -> {
                    val fileContent = getFileContent(repositoryFullName, installationToken)
                    sendManifestData(repositoryFullName, ManifestFileAction.MODIFIED, fileContent)
                }
                MANIFEST_FILE_NAME in headCommit.removed -> {
                    sendManifestData(repositoryFullName, ManifestFileAction.REMOVED, null)
                }
            }
        }
    }

    private fun getFileContent(repositoryFullName: String, token: String): String? {
        return gitHubGraphQLService.getFileContent(
            repositoryFullName,
            "HEAD:${gitHubEnterpriseProperties.manifestFileDirectory}$MANIFEST_FILE_NAME",
            token
        )
    }

    private fun sendManifestData(repositoryFullName: String, action: ManifestFileAction, manifestContent: String?) {
        val manifestFileUpdateDto = ManifestFileUpdateDto(repositoryFullName, action, manifestContent)
        webSocketService.sendMessage("$TOPIC_PREFIX/events/manifestFile", manifestFileUpdateDto)
    }
}
