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
import net.leanix.githubagent.dto.RepositoryOrganizationDto
import net.leanix.githubagent.exceptions.JwtTokenNotFound
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GitHubScanningServiceTest {

    private val gitHubClient = mockk<GitHubClient>()
    private val cachingService = mockk<CachingService>()
    private val webSocketService = mockk<WebSocketService>(relaxUnitFun = true)
    private val gitHubGraphQLService = mockk<GitHubGraphQLService>()
    private val gitHubScanningService = GitHubScanningService(
        gitHubClient,
        cachingService,
        webSocketService,
        gitHubGraphQLService
    )

    @BeforeEach
    fun setup() {
        every { cachingService.get(any()) } returns "value"
        every { gitHubClient.getInstallations(any()) } returns listOf(
            Installation(1, Account("testInstallation"))
        )
        every { gitHubClient.createInstallationToken(1, any()) } returns
            InstallationTokenResponse("testToken", "2024-01-01T00:00:00Z", mapOf(), "all")
        every { cachingService.set(any(), any(), any()) } returns Unit
        every { gitHubClient.getOrganizations(any()) } returns listOf(Organization("testOrganization", 1, "testNodeId"))
        every { gitHubGraphQLService.getRepositories(any(), any()) } returns PagedRepositories(
            repositories = emptyList(),
            hasNextPage = false,
            cursor = null
        )
    }

    @Test
    fun `scanGitHubResources should send organizations over WebSocket`() {
        gitHubScanningService.scanGitHubResources()
        verify { webSocketService.sendMessage("/app/ghe/organizations", any()) }
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
        every { gitHubGraphQLService.getRepositories(any(), any()) } returns PagedRepositories(
            repositories = listOf(
                RepositoryDto(
                    id = "repo1",
                    name = "TestRepo",
                    description = "A test repository",
                    url = "https://github.com/testRepo",
                    organization = RepositoryOrganizationDto(id = "org1", name = "TestOrg"),
                    languages = listOf("Kotlin", "Java"),
                    topics = listOf("test", "example"),
                    manifest = "dependencies { implementation 'com.example:example-lib:1.0.0' }"
                )
            ),
            hasNextPage = false,
            cursor = null
        )
        gitHubScanningService.scanGitHubResources()
        verify { webSocketService.sendMessage(eq("/app/ghe/repositories"), any()) }
    }
}
