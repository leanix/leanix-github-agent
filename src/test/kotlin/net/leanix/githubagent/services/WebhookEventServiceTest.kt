package net.leanix.githubagent.services

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.dto.Account
import net.leanix.githubagent.dto.GitHubSearchResponse
import net.leanix.githubagent.dto.Installation
import net.leanix.githubagent.dto.ItemResponse
import net.leanix.githubagent.dto.ManifestFileAction
import net.leanix.githubagent.dto.ManifestFileUpdateDto
import net.leanix.githubagent.dto.Organization
import net.leanix.githubagent.dto.RepositoryDto
import net.leanix.githubagent.dto.RepositoryItemResponse
import net.leanix.githubagent.graphql.data.enums.RepositoryVisibility
import net.leanix.githubagent.shared.INSTALLATION_REPOSITORIES
import net.leanix.githubagent.shared.MANIFEST_FILE_NAME
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

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

    @MockkBean
    private lateinit var gitHubClient: GitHubClient

    @MockkBean
    private lateinit var gitHubAPIService: GitHubAPIService

    @MockkBean
    private lateinit var workflowRunService: WorkflowRunService

    @SpykBean
    private lateinit var syncLogService: SyncLogService

    private val permissions = mapOf("administration" to "read", "contents" to "read", "metadata" to "read")
    private val events = listOf("label", "public", "repository", "push")

    @BeforeEach
    fun setUp() {
        val installation = Installation(1, Account("testInstallation"), permissions, events)
        every { gitHubAuthenticationService.refreshTokens() } returns Unit
        every { gitHubAuthenticationService.getInstallationToken(any()) } returns "token"
        every { webSocketService.sendMessage(any(), any()) } returns Unit
        every { cachingService.get(any()) } returns "token"
        every { gitHubGraphQLService.getManifestFileContent(any(), any(), any(), any()) } returns "content"
        every { gitHubAPIService.getPaginatedInstallations(any()) } returns listOf(installation)
        every { gitHubClient.getInstallation(any(), any()) } returns installation
        every { workflowRunService.consumeWebhookPayload(any()) } returns Unit
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
    fun `should not process push event with wrong name`() {
        val payload = """{
            "repository": {
                "name": "repo",
                "full_name": "owner/repo",
                "owner": {"name": "owner"},
                "default_branch": "main"
            },
            "head_commit": {
                "added": [],
                "modified": ["$MANIFEST_FILE_NAME", "a-$MANIFEST_FILE_NAME", "a/$MANIFEST_FILE_NAME", "a/a-$MANIFEST_FILE_NAME"],
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

        verify(exactly = 1) {
            webSocketService.sendMessage(
                "/events/manifestFile",
                ManifestFileUpdateDto(
                    "owner/repo",
                    ManifestFileAction.MODIFIED,
                    "content",
                    "tree/main/a"
                )
            )
        }
    }

    @Test
    fun `should accept manifest files with case ignore`() {
        val payload = """{
            "repository": {
                "name": "repo",
                "full_name": "owner/repo",
                "owner": {"name": "owner"},
                "default_branch": "main"
            },
            "head_commit": {
                "added": [],
                "modified": ["a/$MANIFEST_FILE_NAME", "b/leanIX.yaml", "LEanIX.yaml"],
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
                    "tree/main/b"
                )
            )
        }

        verify(exactly = 1) {
            webSocketService.sendMessage(
                "/events/manifestFile",
                ManifestFileUpdateDto(
                    "owner/repo",
                    ManifestFileAction.MODIFIED,
                    "content",
                    "tree/main/a"
                )
            )
        }

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

    @Test
    fun `should ignore push events without a head commit`() {
        val payload = """{
            "repository": {
                "name": "repo",
                "full_name": "owner/repo",
                "owner": {"name": "owner"},
                "default_branch": "main"
            },
            "installation": {"id": 1},
            "ref": "refs/heads/main"
        }"""

        webhookEventService.consumeWebhookEvent("PUSH", payload)

        verify(exactly = 0) {
            webSocketService.sendMessage(any(), any())
        }
    }

    @Test
    fun `should send the org to the backend when an new installation is created`() {
        val runId = UUID.randomUUID()
        every { cachingService.get("runId") } returnsMany listOf("value", null, runId)
        every { cachingService.set("runId", any(), any()) } just runs
        every { cachingService.remove("runId") } just runs
        every { gitHubAPIService.getPaginatedOrganizations(any()) } returns
            listOf(Organization("testOrganization", 1))

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

        verify {
            webSocketService.sendMessage(
                "$runId/organizations",
                any()
            )
        }
    }

    @Test
    fun `should consume workflow run webhook event`() {
        val eventType = "WORKFLOW_RUN"
        val payload = """{
            "action": "completed",
            "workflow_run": {
                "id": 12345678,
                "head_branch": "main",
                "url": "https://api.github.com/repos/leanix/sbom-test/actions/runs/12345678",
                "status": "completed",
                "conclusion": "success",
                }
        }"""

        webhookEventService.consumeWebhookEvent(eventType, payload)

        verify { workflowRunService.consumeWebhookPayload(payload) }
    }

    @Test
    fun `should handle installation repositories event`() {
        every { syncLogService.sendSyncLog(any(), any(), any(), any()) } just runs
        every { gitHubGraphQLService.getRepository(any(), any(), any()) } returns RepositoryDto(
            id = "1",
            name = "repo",
            organizationName = "owner/repo",
            description = "main",
            url = "content",
            defaultBranch = "main",
            archived = false,
            visibility = RepositoryVisibility.PRIVATE,
            languages = emptyList(),
            topics = emptyList(),
            updatedAt = "2021-09-01"
        )
        every { gitHubClient.searchManifestFiles(any(), any()) } returnsMany listOf(
            GitHubSearchResponse(
                2,
                listOf(
                    createItemResponse("repo-test", "cider-org-3"),
                    createItemResponse("repo-test", "cider-org-3")
                )
            ),
            GitHubSearchResponse(
                3,
                listOf(
                    createItemResponse("demo-repo-1", "cider-org-3"),
                    createItemResponse("demo-repo-1", "cider-org-3")
                )
            )
        )

        val payload = """
        {
          "action": "added",
          "installation": {
            "id": 150,
            "account": {
              "login": "cider-org-3",
              "id": 47
            },
            "repository_selection": "selected",
            "access_tokens_url": "https://cider-gh-enterprise.northeurope.cloudapp.azure.com/api/v3/app/installations/150/access_tokens",
            "repositories_url": "https://cider-gh-enterprise.northeurope.cloudapp.azure.com/api/v3/installation/repositories",
            "html_url": "https://cider-gh-enterprise.northeurope.cloudapp.azure.com/organizations/cider-org-3/settings/installations/150",
            "app_id": 3,
            "app_slug": "test-github-app",
            "target_id": 47,
            "target_type": "Organization",
            "permissions": {
              "checks": "write",
              "members": "read",
              "contents": "read",
              "metadata": "read",
              "administration": "read"
            },
            "events": [
              "check_run",
              "check_suite",
              "label",
              "organization",
              "public",
              "push",
              "repository"
            ]
          },
          "repository_selection": "selected",
          "repositories_added": [
            {
              "id": 30,
              "node_id": "MDEwOlJlcG9zaXRvcnkzMA==",
              "name": "repo-test",
              "full_name": "cider-org-3/kostas-test",
              "private": true
            },
            {
              "id": 1648,
              "node_id": "MDEwOlJlcG9zaXRvcnkxNjQ4",
              "name": "demo-repo-1",
              "full_name": "cider-org-3/demo-repo-1",
              "private": true
            },
            {
              "id": 1649,
              "node_id": "MDEwOlJlcG9zaXRvcnkxNjQ5",
              "name": "demo-repo-2",
              "full_name": "cider-org-3/demo-repo-2",
              "private": true
            }
          ],
          "repositories_removed": []
        }
    """

        webhookEventService.consumeWebhookEvent(INSTALLATION_REPOSITORIES, payload)

        verify(exactly = 3) { webSocketService.sendMessage("/events/repository", any()) }
        verify(exactly = 6) { webSocketService.sendMessage("/events/manifestFile", any()) }
    }

    fun createItemResponse(repoName: String, organization: String): ItemResponse {
        return ItemResponse(
            MANIFEST_FILE_NAME,
            "$organization/$repoName/$MANIFEST_FILE_NAME",
            RepositoryItemResponse(repoName, repoName),
            "url"
        )
    }
}
