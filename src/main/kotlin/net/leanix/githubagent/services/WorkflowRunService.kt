package net.leanix.githubagent.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.dto.Artifact
import net.leanix.githubagent.dto.SbomEventDTO
import net.leanix.githubagent.dto.WorkflowRunEventDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class WorkflowRunService(
    private val gitHubAuthenticationService: GitHubAuthenticationService,
    private val webSocketService: WebSocketService,
    private val gitHubClient: GitHubClient,
) {

    private val logger = LoggerFactory.getLogger(WorkflowRunService::class.java)
    private val objectMapper = jacksonObjectMapper()

    fun consumeWebhookPayload(payload: String) {
        runCatching {
            val event = parseEvent(payload) ?: return
            if (!event.isCompleted()) return

            logger.info("Detected workflow event that successfully completed")
            val installationToken = "Bearer ${gitHubAuthenticationService.getInstallationToken(event.installation.id)}"

            getValidArtifacts(event, installationToken)
                .takeIf { it.isNotEmpty() }
                ?.let { artifacts ->
                    logger.info("Found ${artifacts.size} artifact(s).")
                    fetchAndProcessArtifacts(artifacts, event, installationToken)
                } ?: logger.info("No artifacts found for the event")
        }.onFailure {
            logger.error("Failed to consume workflow webhook", it)
        }
    }
    private fun parseEvent(payload: String): WorkflowRunEventDto? {
        return try {
            objectMapper.readValue(payload)
        } catch (e: Exception) {
            logger.error("Failed to parse webhook payload", e)
            null
        }
    }
    private fun getValidArtifacts(event: WorkflowRunEventDto, token: String): List<Artifact> {
        val owner = event.repository.owner.login
        val repo = event.repository.name
        val runId = event.workflowRun.id

        return gitHubClient.getRunArtifacts(owner, repo, runId, token)
            .artifacts
            .filter { it.name.contains("-sbom") }
    }

    private fun fetchAndProcessArtifacts(
        artifacts: List<Artifact>,
        event: WorkflowRunEventDto,
        installationToken: String
    ) {
        val owner = event.repository.owner.login
        val repo = event.repository.name

        artifacts.forEach { artifact ->
            logger.info("Processing artifact: ${artifact.name}")
            downloadAndSendArtifact(owner, repo, artifact, installationToken)
        }
    }

    private fun downloadAndSendArtifact(owner: String, repo: String, artifact: Artifact, token: String) = runCatching {
        gitHubClient.downloadArtifact(owner, repo, artifact.id, token).body()?.use { body ->
            val sbomContent = Base64.getEncoder().encodeToString(body.asInputStream().readAllBytes())
            sendSbomEvent(repo, artifact.name, sbomContent)
        } ?: logger.error("Failed to download artifact: ${artifact.name}")
    }.onFailure {
        logger.error("Error processing artifact: ${artifact.name}", it)
    }

    private fun sendSbomEvent(repo: String, artifactName: String, sbomContent: String) {
        logger.info("Sending sbom file: $repo - $artifactName")
        webSocketService.sendMessage(
            "/events/sbom",
            SbomEventDTO(
                repositoryName = repo,
                sbomFileName = artifactName,
                sbomFileContent = sbomContent
            )
        )
    }
}
