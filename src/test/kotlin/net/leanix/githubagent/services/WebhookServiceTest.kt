package net.leanix.githubagent.services

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import net.leanix.githubagent.dto.ManifestFileAction
import net.leanix.githubagent.dto.ManifestFileUpdateDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class WebhookServiceTest {

    @MockkBean
    private lateinit var webSocketService: WebSocketService

    @MockkBean
    private lateinit var gitHubGraphQLService: GitHubGraphQLService

    @MockkBean
    private lateinit var cachingService: CachingService

    @MockkBean
    private lateinit var gitHubAuthenticationService: GitHubAuthenticationService

    @Autowired
    private lateinit var webhookService: WebhookService

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

        webhookService.consumeWebhookEvent("PUSH", payload)

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

        webhookService.consumeWebhookEvent("PUSH", payload)

        verify(exactly = 1) {
            webSocketService.sendMessage(
                "/events/manifestFile",
                ManifestFileUpdateDto(
                    "owner/repo",
                    ManifestFileAction.MODIFIED,
                    "content"
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

        webhookService.consumeWebhookEvent("OTHER", payload)

        verify(exactly = 1) { webSocketService.sendMessage("/events/other", payload) }
    }
}
