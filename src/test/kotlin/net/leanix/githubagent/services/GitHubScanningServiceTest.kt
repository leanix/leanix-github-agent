package net.leanix.githubagent.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.dto.Account
import net.leanix.githubagent.dto.Installation
import net.leanix.githubagent.dto.InstallationTokenResponse
import net.leanix.githubagent.dto.Organization
import net.leanix.githubagent.dto.PagedRepositories
import net.leanix.githubagent.dto.RepositoryDto
import net.leanix.githubagent.exceptions.JwtTokenNotFound
import net.leanix.githubagent.graphql.data.enums.RepositoryVisibility
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class GitHubScanningServiceTest {

    private val gitHubClient = mockk<GitHubClient>()
    private val cachingService = mockk<CachingService>()
    private val webSocketService = mockk<WebSocketService>(relaxUnitFun = true)
    private val gitHubGraphQLService = mockk<GitHubGraphQLService>()
    private val gitHubAuthenticationService = mockk<GitHubAuthenticationService>()
    private val syncLogService = mockk<SyncLogService>()
    private val gitHubScanningService = GitHubScanningService(
        gitHubClient,
        cachingService,
        webSocketService,
        gitHubGraphQLService,
        gitHubAuthenticationService,
        syncLogService
    )
    private val runId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        every { cachingService.get(any()) } returns "value"
        every { gitHubClient.getInstallations(any()) } returns listOf(
            Installation(1, Account("testInstallation"))
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
    }

    @Test
    fun `scanGitHubResources should send organizations over WebSocket`() {
        every { cachingService.get("runId") } returns runId
        gitHubScanningService.scanGitHubResources()
        verify { webSocketService.sendMessage(eq("$runId/organizations"), any()) }
        verify(exactly = 2) { syncLogService.sendInfoLog(any()) }
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
        gitHubScanningService.scanGitHubResources()
        verify { webSocketService.sendMessage(eq("$runId/repositories"), any()) }
    }
}
