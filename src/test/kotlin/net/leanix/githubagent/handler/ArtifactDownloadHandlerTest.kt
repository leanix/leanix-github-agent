package net.leanix.githubagent.handler

import feign.Request
import feign.Response
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.dto.Artifact
import net.leanix.githubagent.dto.ArtifactDownloadDTO
import net.leanix.githubagent.dto.ArtifactsListResponse
import net.leanix.githubagent.services.GitHubAuthenticationService
import net.leanix.githubagent.services.WebSocketService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.simp.stomp.StompHeaders
import java.nio.file.Files
import java.nio.file.Paths

class ArtifactDownloadHandlerTest {

    private val gitHubClient = mockk<GitHubClient>()
    private val webSocketService = mockk<WebSocketService>(relaxed = true)
    private val gitHubAuthenticationService = mockk<GitHubAuthenticationService>()
    private val artifactDownloadHandler = ArtifactDownloadHandler(
        gitHubClient,
        webSocketService,
        gitHubAuthenticationService
    )

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(artifactDownloadHandler)
    }

    @Test
    fun `it should receive message from server and send artifact`() {
        // given
        every { gitHubAuthenticationService.getInstallationToken(any()) } returns "token"
        every { gitHubClient.getRunArtifacts(any(), any(), any(), any()) } returns ArtifactsListResponse(
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

        // when
        artifactDownloadHandler.handleFrame(
            StompHeaders(),
            ArtifactDownloadDTO(
                repositoryName = "repository",
                repositoryOwner = "leanix",
                runId = 1,
                installationId = 1,
                artifactName = "-sbom"
            )
        )

        // then
        verify {
            webSocketService.sendMessage("/artifact", any())
        }
    }

    @Test
    fun `it should receive message from server and send artifact when artifact name is null`() {
        // given
        every { gitHubAuthenticationService.getInstallationToken(any()) } returns "token"
        every { gitHubClient.getRunArtifacts(any(), any(), any(), any()) } returns ArtifactsListResponse(
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

        // when
        artifactDownloadHandler.handleFrame(
            StompHeaders(),
            ArtifactDownloadDTO(
                repositoryName = "repository",
                repositoryOwner = "leanix",
                runId = 1,
                installationId = 1,
            )
        )

        // then
        verify {
            webSocketService.sendMessage("/artifact", any())
        }
    }
    private fun readSbomFile(): ByteArray {
        val filePath = Paths.get("src/test/resources/sbom/leanix-sbom-test-sbom.zip")
        return Files.readAllBytes(filePath)
    }
}
