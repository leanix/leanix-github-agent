package net.leanix.githubagent.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.leanix.githubagent.config.GitHubEnterpriseProperties
import net.leanix.githubagent.dto.ManifestFileAction
import net.leanix.githubagent.dto.ManifestFileDto
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
        val organizationName = pushEventPayload.repository.owner.name

        var installationToken = cachingService.get("installationToken:${pushEventPayload.installation.id}")?.toString()
        if (installationToken == null) {
            gitHubAuthenticationService.refreshTokens()
            installationToken = cachingService.get("installationToken:${pushEventPayload.installation.id}")?.toString()
            require(installationToken != null) { "Installation token not found/ expired" }
        }

        if (pushEventPayload.ref == "refs/heads/${pushEventPayload.repository.defaultBranch}") {
            handleManifestFileChanges(
                "${gitHubEnterpriseProperties.manifestFileDirectory}${ManifestFileName.YAML.fileName}",
                headCommit,
                repositoryFullName,
                organizationName,
                repositoryName,
                installationToken
            )
            handleManifestFileChanges(
                "${gitHubEnterpriseProperties.manifestFileDirectory}${ManifestFileName.YML.fileName}",
                headCommit,
                repositoryFullName,
                organizationName,
                repositoryName,
                installationToken
            )
        }
    }

    @SuppressWarnings("LongParameterList")
    private fun handleManifestFileChanges(
        manifestFilePath: String,
        headCommit: PushEventCommit,
        repositoryFullName: String,
        organizationName: String,
        repositoryName: String,
        installationToken: String
    ) {
        when (manifestFilePath) {
            in headCommit.added, in headCommit.modified -> {
                val action = when (manifestFilePath) {
                    in headCommit.added -> ManifestFileAction.ADDED
                    else -> ManifestFileAction.MODIFIED
                }
                logger.info("Manifest file $action in repository $repositoryFullName")
                val fileContent = gitHubGraphQLService.getManifestFileContent(
                    organizationName,
                    repositoryName,
                    manifestFilePath,
                    installationToken
                )
                webSocketService.sendMessage(
                    "/events/manifestFile",
                    ManifestFileUpdateDto(
                        repositoryFullName,
                        action,
                        ManifestFileDto(manifestFilePath, fileContent)
                    )
                )
            }
            in headCommit.removed -> {
                logger.info("Manifest file ${ManifestFileAction.REMOVED} from repository $repositoryFullName")
                webSocketService.sendMessage(
                    "/events/manifestFile",
                    ManifestFileUpdateDto(
                        repositoryFullName,
                        ManifestFileAction.REMOVED,
                        ManifestFileDto(null, null)
                    )
                )
            }
        }
    }
}
