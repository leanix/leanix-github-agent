package net.leanix.githubagent.services

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import net.leanix.githubagent.dto.ManifestFileAction
import net.leanix.githubagent.dto.ManifestFileUpdateDto
import net.leanix.githubagent.shared.MANIFEST_FILE_NAME
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

const val UNSUPPORTED_MANIFEST_EXTENSION = "leanix.yml"

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
        every { cachingService.get(any()) } returns "token"
        every { gitHubGraphQLService.getManifestFileContent(any(), any(), any(), any()) } returns "content"
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
        val payload = """{
            "repository": {
                "name": "repo",
                "full_name": "owner/repo",
                "owner": {"name": "owner"},
                "default_branch": "main"
            },
            "head_commit": {
                "added": [],
                "modified": ["$MANIFEST_FILE_NAME"],
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
                    "content",
                    ""
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
    fun `should send updates for yml manifest file`() {
        val payload = """
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
                    "added": ["$MANIFEST_FILE_NAME"],
                    "modified": [],
                    "removed": []
                },
                "installation": {
                    "id": 1
                }
            }
        """
        every { gitHubGraphQLService.getManifestFileContent(any(), any(), MANIFEST_FILE_NAME, any()) } returns "content"

        webhookEventService.consumeWebhookEvent("PUSH", payload)

        verify { webSocketService.sendMessage(any(), any<ManifestFileUpdateDto>()) }
    }

    @Test
    fun `should handle manifest file removal in subdirectory`() {
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
                "removed": ["a/b/c/$MANIFEST_FILE_NAME", "a/b/c/some_other_file.yaml"]
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
                    ManifestFileAction.REMOVED,
                    null,
                    "tree/main/a/b/c"
                )
            )
        }
    }

    @Test
    fun `should handle manifest file in subdirectory`() {
        val payload = """{
            "repository": {
                "name": "repo",
                "full_name": "owner/repo",
                "owner": {"name": "owner"},
                "default_branch": "main"
            },
            "head_commit": {
                "added": ["a/b/c/$MANIFEST_FILE_NAME"],
                "modified": [],
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
                    ManifestFileAction.ADDED,
                    "content",
                    "tree/main/a/b/c"
                )
            )
        }
    }

    @Test
    fun `should handle push event with multiple added and modified files`() {
        val payload = """{
            "repository": {
                "name": "repo",
                "full_name": "owner/repo",
                "owner": {"name": "owner"},
                "default_branch": "main"
            },
            "head_commit": {
                "added": ["custom/path/added1/$MANIFEST_FILE_NAME", "custom/path/added2/$MANIFEST_FILE_NAME"],
                "modified": ["custom/path/modified/$MANIFEST_FILE_NAME"],
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
                    ManifestFileAction.ADDED,
                    "content",
                    "tree/main/custom/path/added1"
                )
            )
            webSocketService.sendMessage(
                "/events/manifestFile",
                ManifestFileUpdateDto(
                    "owner/repo",
                    ManifestFileAction.ADDED,
                    "content",
                    "tree/main/custom/path/added2"
                )
            )
            webSocketService.sendMessage(
                "/events/manifestFile",
                ManifestFileUpdateDto(
                    "owner/repo",
                    ManifestFileAction.MODIFIED,
                    "content",
                    "tree/main/custom/path/modified"
                )
            )
        }
    }

    @Test
    fun `should handle push event only with supported YAML extension`() {
        val payload = """{
            "repository": {
                "name": "repo",
                "full_name": "owner/repo",
                "owner": {"name": "owner"},
                "default_branch": "main"
            },
            "head_commit": {
                "added": ["custom/path/added1/$UNSUPPORTED_MANIFEST_EXTENSION", "custom/path/added2/$MANIFEST_FILE_NAME"],
                "modified": [],
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
                    ManifestFileAction.ADDED,
                    "content",
                    "tree/main/custom/path/added2"
                )
            )
        }

        verify(exactly = 0) {
            webSocketService.sendMessage(
                "/events/manifestFile",
                ManifestFileUpdateDto(
                    "owner/repo",
                    ManifestFileAction.ADDED,
                    "content",
                    "tree/main/custom/path/added1/$UNSUPPORTED_MANIFEST_EXTENSION"
                )
            )
        }
    }

    @Test
    fun `should wait for active scan to finish before starting scanning new org`() {
        every { cachingService.get("runId") } returnsMany listOf("value", "value", "value", null)
        every { cachingService.set("runId", any(), any()) } just runs
        every { cachingService.remove("runId") } just runs

        val eventType = "INSTALLATION"
        val payload = """{
          "action": "created",
          "installation": {
            "id": 30,
            "account": {
              "login": "test-org",
              "id": 20
            }
          }
        }"""

        webhookEventService.consumeWebhookEvent(eventType, payload)

        verify(exactly = 6) { cachingService.get("runId") }
    }
}
