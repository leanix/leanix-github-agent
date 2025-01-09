package net.leanix.githubagent.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.dto.Account
import net.leanix.githubagent.dto.GitHubAppResponse
import net.leanix.githubagent.dto.GitHubSearchResponse
import net.leanix.githubagent.dto.Installation
import net.leanix.githubagent.dto.InstallationTokenResponse
import net.leanix.githubagent.dto.ItemResponse
import net.leanix.githubagent.dto.ManifestFilesDTO
import net.leanix.githubagent.dto.Organization
import net.leanix.githubagent.dto.PagedRepositories
import net.leanix.githubagent.dto.RepositoryDto
import net.leanix.githubagent.dto.RepositoryItemResponse
import net.leanix.githubagent.exceptions.JwtTokenNotFound
import net.leanix.githubagent.graphql.data.enums.RepositoryVisibility
import net.leanix.githubagent.handler.RateLimitHandler
import net.leanix.githubagent.shared.MANIFEST_FILE_NAME
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class GitHubScanningServiceTest {

    private val gitHubClient = mockk<GitHubClient>(relaxUnitFun = true)
    private val cachingService = mockk<CachingService>()
    private val webSocketService = mockk<WebSocketService>(relaxUnitFun = true)
    private val gitHubGraphQLService = mockk<GitHubGraphQLService>()
    private val gitHubAuthenticationService = mockk<GitHubAuthenticationService>()
    private val syncLogService = mockk<SyncLogService>(relaxUnitFun = true)
    private val rateLimitHandler = mockk<RateLimitHandler>(relaxUnitFun = true)
    private val gitHubEnterpriseService = GitHubEnterpriseService(gitHubClient, syncLogService)
    private val gitHubScanningService = GitHubScanningService(
        gitHubClient,
        cachingService,
        webSocketService,
        gitHubGraphQLService,
        gitHubAuthenticationService,
        syncLogService,
        rateLimitHandler,
        gitHubEnterpriseService,
    )
    private val runId = UUID.randomUUID()

    private val permissions = mapOf("administration" to "read", "contents" to "read", "metadata" to "read")
    private val events = listOf("label", "public", "repository", "push")

    @BeforeEach
    fun setup() {
        every { cachingService.get(any()) } returns "value"
        every { gitHubClient.getInstallations(any()) } returns listOf(
            Installation(1, Account("testInstallation"), permissions, events)
        )
        every { gitHubClient.createInstallationToken(1, any()) } returns
            InstallationTokenResponse("testToken", "2024-01-01T00:00:00Z", mapOf(), "all")
        every { cachingService.set(any(), any(), any()) } returns Unit
        every { gitHubClient.getOrganizations(any()) } returns listOf(Organization("testOrganization", 1))
        every { gitHubGraphQLService.getRepositories(any(), any()) } returns PagedRepositories(
            repositories = emptyList(),
            hasNextPage = false,
            cursor = null
        )
        every { cachingService.remove(any()) } returns Unit
        every { gitHubAuthenticationService.generateAndCacheInstallationTokens(any(), any()) } returns Unit
        every { syncLogService.sendErrorLog(any()) } returns Unit
        every { syncLogService.sendInfoLog(any()) } returns Unit
        every { rateLimitHandler.executeWithRateLimitHandler(any<() -> Any>()) } answers
            { firstArg<() -> Any>().invoke() }
        every { gitHubClient.getApp(any()) } returns GitHubAppResponse("testApp", permissions, events)
    }

    @Test
    fun `scanGitHubResources should send organizations over WebSocket`() {
        every { cachingService.get("runId") } returns runId
        gitHubScanningService.scanGitHubResources()
        verify { webSocketService.sendMessage(eq("$runId/organizations"), any()) }
        verify {
            syncLogService.sendInfoLog(
                "The connector found 0 organizations with GitHub application installed, out of possible 1."
            )
        }
    }

    @Test
    fun `scanGitHubResources should handle empty installations`() {
        val runId = UUID.randomUUID()

        every { cachingService.get("runId") } returns runId
        every { gitHubClient.getInstallations(any()) } returns emptyList()

        gitHubScanningService.scanGitHubResources()

        verify { webSocketService.sendMessage(eq("$runId/organizations"), eq(emptyList<Organization>())) }
    }

    @Test
    fun `scanGitHubResources should throw JwtTokenNotFound when jwtToken is expired`() {
        every { cachingService.get("jwtToken") } returns null
        assertThrows<JwtTokenNotFound> {
            gitHubScanningService.scanGitHubResources()
        }
    }

    @Test
    fun `scanGitHubResources should send repositories over WebSocket`() {
        every { cachingService.get("runId") } returns runId
        every { gitHubGraphQLService.getRepositories(any(), any()) } returns PagedRepositories(
            repositories = listOf(
                RepositoryDto(
                    id = "repo1",
                    name = "TestRepo",
                    organizationName = "testOrg",
                    description = "A test repository",
                    url = "https://github.com/testRepo",
                    defaultBranch = "main",
                    archived = false,
                    visibility = RepositoryVisibility.PUBLIC,
                    updatedAt = "2024-01-01T00:00:00Z",
                    languages = listOf("Kotlin", "Java"),
                    topics = listOf("test", "example"),
                )
            ),
            hasNextPage = false,
            cursor = null
        )
        every { gitHubClient.searchManifestFiles(any(), any()) } returns GitHubSearchResponse(0, emptyList())
        gitHubScanningService.scanGitHubResources()
        verify { webSocketService.sendMessage(eq("$runId/repositories"), any()) }
    }

    @Test
    fun `scanGitHubResources should send repositories and manifest files over WebSocket`() {
        // given
        every { cachingService.get("runId") } returns runId
        every { gitHubGraphQLService.getRepositories(any(), any()) } returns PagedRepositories(
            repositories = listOf(
                RepositoryDto(
                    id = "repo1",
                    name = "TestRepo",
                    organizationName = "testOrg",
                    description = "A test repository",
                    url = "https://github.com/testRepo",
                    defaultBranch = "main",
                    archived = false,
                    visibility = RepositoryVisibility.PUBLIC,
                    updatedAt = "2024-01-01T00:00:00Z",
                    languages = listOf("Kotlin", "Java"),
                    topics = listOf("test", "example"),
                )
            ),
            hasNextPage = false,
            cursor = null
        )
        every { gitHubClient.searchManifestFiles(any(), any()) } returns GitHubSearchResponse(
            1,
            listOf(
                ItemResponse(
                    name = "leanix.yaml",
                    path = "dir/leanix.yaml",
                    repository = RepositoryItemResponse(
                        name = "TestRepo",
                        fullName = "testOrg/TestRepo"
                    ),
                    url = "http://url"
                )
            )
        )
        every { gitHubGraphQLService.getManifestFileContent(any(), any(), "dir/leanix.yaml", any()) } returns "content"

        // when
        gitHubScanningService.scanGitHubResources()

        // then
        verify { webSocketService.sendMessage(eq("$runId/manifestFiles"), any()) }
        verify { syncLogService.sendInfoLog("Scanning repository TestRepo for manifest files.") }
        verify { syncLogService.sendInfoLog("Fetched manifest file 'dir/leanix.yaml' from repository 'TestRepo'.") }
        verify { syncLogService.sendInfoLog("Found 1 manifest files in repository TestRepo.") }
        verify { syncLogService.sendInfoLog("Finished initial full scan for organization testInstallation.") }
        verify { syncLogService.sendInfoLog("Finished full scan for all available organizations.") }
    }

    @Test
    fun `scanGitHubResources should not send repositories and manifest files over WebSocket for archived repos`() {
        // given
        every { cachingService.get("runId") } returns runId
        every { gitHubGraphQLService.getRepositories(any(), any()) } returns PagedRepositories(
            repositories = listOf(
                RepositoryDto(
                    id = "repo1",
                    name = "TestRepo",
                    organizationName = "testOrg",
                    description = "A test repository",
                    url = "https://github.com/testRepo",
                    defaultBranch = "main",
                    archived = true,
                    visibility = RepositoryVisibility.PUBLIC,
                    updatedAt = "2024-01-01T00:00:00Z",
                    languages = listOf("Kotlin", "Java"),
                    topics = listOf("test", "example"),
                )
            ),
            hasNextPage = false,
            cursor = null
        )
        every { gitHubClient.searchManifestFiles(any(), any()) } returns GitHubSearchResponse(
            1,
            listOf(
                ItemResponse(
                    name = "leanix.yaml",
                    path = "dir/leanix.yaml",
                    repository = RepositoryItemResponse(
                        name = "TestRepo",
                        fullName = "testOrg/TestRepo"
                    ),
                    url = "http://url"
                )
            )
        )
        every { gitHubGraphQLService.getManifestFileContent(any(), any(), "dir/leanix.yaml", any()) } returns "content"

        // when
        gitHubScanningService.scanGitHubResources()

        // then
        verify(exactly = 0) { webSocketService.sendMessage(eq("$runId/manifestFiles"), any()) }
        verify(exactly = 0) { syncLogService.sendInfoLog("Scanning repository TestRepo for manifest files.") }
        verify(exactly = 0) { syncLogService.sendInfoLog("Fetched manifest file 'dir/leanix.yaml' from repository 'TestRepo'.") }
        verify(exactly = 0) { syncLogService.sendInfoLog("Found 1 manifest files in repository TestRepo.") }
        verify { syncLogService.sendInfoLog("Finished initial full scan for organization testInstallation.") }
        verify { syncLogService.sendInfoLog("Finished full scan for all available organizations.") }
    }

    @Test
    fun `scanGitHubResources should send manifest files with empty path if the file is in the root directory`() {
        // given
        every { cachingService.get("runId") } returns runId
        every { gitHubGraphQLService.getRepositories(any(), any()) } returns PagedRepositories(
            repositories = listOf(
                RepositoryDto(
                    id = "repo1",
                    name = "TestRepo",
                    organizationName = "testOrg",
                    description = "A test repository",
                    url = "https://github.com/testRepo",
                    defaultBranch = "main",
                    archived = false,
                    visibility = RepositoryVisibility.PUBLIC,
                    updatedAt = "2024-01-01T00:00:00Z",
                    languages = listOf("Kotlin", "Java"),
                    topics = listOf("test", "example"),
                )
            ),
            hasNextPage = false,
            cursor = null
        )
        every { gitHubClient.searchManifestFiles(any(), any()) } returns GitHubSearchResponse(
            1,
            listOf(
                ItemResponse(
                    name = "leanix.yaml",
                    path = MANIFEST_FILE_NAME,
                    repository = RepositoryItemResponse(
                        name = "TestRepo",
                        fullName = "testOrg/TestRepo"
                    ),
                    url = "http://url"
                )
            )
        )
        every { gitHubGraphQLService.getManifestFileContent(any(), any(), MANIFEST_FILE_NAME, any()) } returns "content"
        val fileSlot = slot<ManifestFilesDTO>()

        // when
        gitHubScanningService.scanGitHubResources()

        // then
        verify { webSocketService.sendMessage(eq("$runId/manifestFiles"), capture(fileSlot)) }
        assertEquals(fileSlot.captured.manifestFiles[0].path, "")
    }

    @Test
    fun `scanGitHubResources should skip organizations without correct permissions and events`() {
        every { cachingService.get("runId") } returns runId
        every { gitHubClient.getInstallations(any()) } returns listOf(
            Installation(1, Account("testInstallation1"), mapOf(), listOf()),
            Installation(2, Account("testInstallation2"), permissions, events),
            Installation(3, Account("testInstallation3"), permissions, events)
        )
        gitHubScanningService.scanGitHubResources()
        verify { webSocketService.sendMessage(eq("$runId/organizations"), any()) }
        verify {
            syncLogService.sendErrorLog(
                "Failed to scan organization testInstallation1. Installation missing " +
                    "the following permissions: [administration, contents, metadata], " +
                    "and the following events: [label, public, repository, push]"
            )
        }
        verify { syncLogService.sendInfoLog("Finished initial full scan for organization testInstallation2.") }
        verify { syncLogService.sendInfoLog("Finished initial full scan for organization testInstallation2.") }
    }
}
