package net.leanix.githubagent.handler

import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.dto.Artifact
import net.leanix.githubagent.dto.ArtifactDTO
import net.leanix.githubagent.dto.ArtifactDownloadDTO
import net.leanix.githubagent.services.GitHubAuthenticationService
import net.leanix.githubagent.services.WebSocketService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.stereotype.Component
import java.lang.reflect.Type
import java.util.*

@Component
class ArtifactDownloadHandler(
    private val gitHubClient: GitHubClient,
    @Lazy @Autowired
    private val webSocketService: WebSocketService,
    @Lazy @Autowired
    private val gitHubAuthenticationService: GitHubAuthenticationService
) : StompFrameHandler {

    private val logger = LoggerFactory.getLogger(ArtifactDownloadHandler::class.java)

    override fun getPayloadType(headers: StompHeaders): Type {
        return ArtifactDownloadDTO::class.java
    }

    override fun handleFrame(headers: StompHeaders, payload: Any?) {
        payload?.let {
            val dto = payload as ArtifactDownloadDTO
            logger.info("Received artifact download message from server for repo: ${dto.repositoryName}")
            runCatching {
                val installationToken =
                    "Bearer ${gitHubAuthenticationService.getInstallationToken(dto.installationId)}"

                getValidArtifacts(dto, installationToken)
                    .takeIf { it.isNotEmpty() }
                    ?.let { artifacts ->
                        logger.info("Found ${artifacts.size} artifact(s).")
                        fetchAndProcessArtifacts(artifacts, dto, installationToken)
                    } ?: logger.info("No artifacts found for this repository: ${dto.repositoryName}")
            }
        }
    }
    private fun getValidArtifacts(dto: ArtifactDownloadDTO, token: String): List<Artifact> {
        return gitHubClient.getRunArtifacts(dto.repositoryOwner, dto.repositoryName, dto.runId, token)
            .artifacts
            .filter {
                if (dto.artifactName != null) {
                    it.name.contains(dto.artifactName)
                } else {
                    true
                }
            }
    }

    private fun fetchAndProcessArtifacts(
        artifacts: List<Artifact>,
        dto: ArtifactDownloadDTO,
        installationToken: String
    ) {
        artifacts.forEach { artifact ->
            logger.info("Processing artifact: ${artifact.name}")
            downloadAndSendArtifact(dto, artifact, installationToken)
        }
    }

    private fun downloadAndSendArtifact(dto: ArtifactDownloadDTO, artifact: Artifact, token: String) = runCatching {
        val owner = dto.repositoryOwner
        val repo = dto.repositoryOwner
        gitHubClient.downloadArtifact(owner, repo, artifact.id, token).body()?.use { body ->
            val artifactContent = Base64.getEncoder().encodeToString(body.asInputStream().readAllBytes())
            sendArtifactEvent(dto, artifact.name, artifactContent)
        } ?: logger.error("Failed to download artifact: ${artifact.name}")
    }.onFailure {
        logger.error("Error processing artifact: ${artifact.name}", it)
    }

    private fun sendArtifactEvent(dto: ArtifactDownloadDTO, artifactName: String, artifactContent: String) {
        logger.info("Sending artifacts file: ${dto.repositoryName} - $artifactName")
        webSocketService.sendMessage(
            "/artifact",
            ArtifactDTO(
                repositoryFullName = "${dto.repositoryOwner}/${dto.repositoryName}",
                artifactFileName = artifactName,
                artifactFileContent = artifactContent,
            )
        )
    }
}
