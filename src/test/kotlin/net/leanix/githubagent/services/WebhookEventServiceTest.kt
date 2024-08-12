package net.leanix.githubagent.services

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import net.leanix.githubagent.dto.ManifestFileAction
import net.leanix.githubagent.dto.ManifestFileDto
import net.leanix.githubagent.dto.ManifestFileUpdateDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class WebhookEventServiceTest {

    @MockkBean
    private lateinit var webSocketService: WebSocketService

    @MockkBean
    private lateinit var gitHubGraphQLService: GitHubGraphQLService

    @MockkBean
    private lateinit var cachingService: CachingService

    @MockkBean
    private lateinit var gitHubAuthenticationService: GitHubAuthenticationService

    @Autowired
    private lateinit var webhookEventService: WebhookEventService

    @BeforeEach
    fun setUp() {
        every { gitHubAuthenticationService.refreshTokens() } returns Unit
        every { webSocketService.sendMessage(any(), any()) } returns Unit
    }

    @Test
    fun `should refresh tokens if expired`() {
        val payload = """{
            "repository": {
                "name": "repo",
                "full_name": "owner/repo",
                "owner": {"name": "owner"},
                "default_branch": "main"
            },
            "head_commit": {
                "added": [],
                "modified": [],
                "removed": []
            },
            "installation": {"id": 1},
            "ref": "refs/heads/main"
        }"""

        every { cachingService.get("installationToken:1") } returns null andThen "token"

        webhookEventService.consumeWebhookEvent("PUSH", payload)

        verify(exactly = 1) { gitHubAuthenticationService.refreshTokens() }
    }

    @Test
    fun `should process push event`() {
        every { cachingService.get(any()) } returns "token"
        every { gitHubGraphQLService.getManifestFileContent(any(), any(), any(), any()) } returns "content"

        val payload = """{
            "repository": {
                "name": "repo",
                "full_name": "owner/repo",
                "owner": {"name": "owner"},
                "default_branch": "main"
            },
            "head_commit": {
                "added": [],
                "modified": ["leanix.yaml"],
                "removed": []
            },
            "installation": {"id": 1},
            "ref": "refs/heads/main"
        }"""

        webhookEventService.consumeWebhookEvent("PUSH", payload)

        verify(exactly = 1) {
            webSocketService.sendMessage(
                "/events/manifestFile",
                ManifestFileUpdateDto(
                    "owner/repo",
                    ManifestFileAction.MODIFIED,
                    ManifestFileDto("leanix.yaml", "content")
                )
            )
        }
    }

    @Test
    fun `should send all events of type other than push to backend without processing`() {
        val payload = """{
            "repository": {
                "name": "repo",
                "full_name": "owner/repo",
                "owner": {"name": "owner"},
                "default_branch": "main"
            },
            "head_commit": {
                "added": [],
                "modified": [],
                "removed": []
            },
            "installation": {"id": 1},
            "ref": "refs/heads/main"
        }"""

        webhookEventService.consumeWebhookEvent("OTHER", payload)

        verify(exactly = 1) { webSocketService.sendMessage("/events/other", payload) }
    }

    @Test
    fun `should send updates for yaml manifest file`() {
        val manifestFilePath = "leanix.yaml"
        val payload = createPushEventPayload(manifestFilePath)
        every { cachingService.get(any()) } returns "token"
        every { gitHubGraphQLService.getManifestFileContent(any(), any(), manifestFilePath, any()) } returns "content"

        webhookEventService.consumeWebhookEvent("PUSH", payload)

        verify { webSocketService.sendMessage(any(), any<ManifestFileUpdateDto>()) }
    }

    @Test
    fun `should send updates for yml manifest file`() {
        val manifestFilePath = "leanix.yml"
        val payload = createPushEventPayload(manifestFilePath)
        every { cachingService.get(any()) } returns "token"
        every { gitHubGraphQLService.getManifestFileContent(any(), any(), manifestFilePath, any()) } returns "content"

        webhookEventService.consumeWebhookEvent("PUSH", payload)

        verify { webSocketService.sendMessage(any(), any<ManifestFileUpdateDto>()) }
    }

    private fun createPushEventPayload(manifestFileName: String): String {
        return """
            {
                "ref": "refs/heads/main",
                "repository": {
                    "name": "repo",
                    "full_name": "org/repo",
                    "default_branch": "main",
                    "owner": {
                        "name": "org"
                    }
                },
                "head_commit": {
                    "added": ["$manifestFileName"],
                    "modified": [],
                    "removed": []
                },
                "installation": {
                    "id": 1
                }
            }
        """
    }
}
