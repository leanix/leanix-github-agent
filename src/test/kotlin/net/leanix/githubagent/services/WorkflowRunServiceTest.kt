package net.leanix.githubagent.services

import feign.Request
import feign.Response
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.dto.Artifact
import net.leanix.githubagent.dto.ArtifactsListResponse
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class WorkflowRunServiceTest {

    private val gitHubAuthenticationService: GitHubAuthenticationService = mockk()
    private val webSocketService: WebSocketService = mockk()
    private val gitHubClient: GitHubClient = mockk()
    private val workflowRunService = WorkflowRunService(gitHubAuthenticationService, webSocketService, gitHubClient)

    @Test
    fun `it should send sbom file when the workflow run has sbom artifact`() {
        // given
        val payload = """{
            "action": "completed",
            "workflow_run": {
                "id": 12345678,
                "head_branch": "main",
                "url": "https://api.github.com/repos/leanix/sbom-test/actions/runs/12345678",
                "status": "completed",
                "conclusion": "success",
                "node_id": "node-id"
                },
            "repository":{
                "id": 12345678,
                "node_id": "node-id",
                "name": "sbom-test",
                "full_name": "leanix/sbom-test",
                "private": true,
                "default_branch": "main",
                "owner":{
                    "login": "leanix"
                }
            },
            "installation": {
                "id": 12345678,
                "node_id": "node-id"
            }
        }"""
        every { gitHubAuthenticationService.getInstallationToken(any()) } returns "token"
        every { gitHubClient.listRunArtifacts(any(), any(), any(), any()) } returns ArtifactsListResponse(
            totalCount = 2,
            artifacts = listOf(
                Artifact(
                    id = 1,
                    name = "leanix-sbom-test-sbom",
                    url = "http://download.url",
                    archiveDownloadUrl = "http://download.url"
                ),
                Artifact(
                    id = 2,
                    name = "invalid-name",
                    url = "http://download.url",
                    archiveDownloadUrl = "http://download.url"
                )
            )
        )
        val request = mockk<Request>()

        every { gitHubClient.downloadArtifact(any(), any(), any(), any()) } returns Response
            .builder()
            .request(request)
            .body(readSbomFile()).build()
        every { webSocketService.sendMessage(any(), any()) } returns Unit

        // when
        workflowRunService.consumeWebhookPayload(payload)

        // then
        verify {
            webSocketService.sendMessage(
                "/events/sbom",
                any()
            )
        }
    }

    private fun readSbomFile(): ByteArray {
        val filePath = Paths.get("src/test/resources/sbom/leanix-sbom-test-sbom.zip")
        return Files.readAllBytes(filePath)
    }
}
