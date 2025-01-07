package net.leanix.githubagent.services

import io.mockk.every
import io.mockk.mockk
import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.dto.Account
import net.leanix.githubagent.dto.Installation
import net.leanix.githubagent.dto.Organization
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GitHubAPIServiceTest {

    private val gitHubClient = mockk<GitHubClient>()
    private val gitHubAPIService = GitHubAPIService(gitHubClient)

    private val permissions = mapOf("administration" to "read", "contents" to "read", "metadata" to "read")
    private val events = listOf("label", "public", "repository", "push")

    @Test
    fun `test getPaginatedInstallations with one page`() {
        val jwtToken = "test-jwt-token"
        val installationsPage1 = listOf(
            Installation(1, Account("test-account"), permissions, events),
            Installation(2, Account("test-account"), permissions, events)
        )

        every { gitHubClient.getInstallations(any(), any(), any()) } returns installationsPage1

        val installations = gitHubAPIService.getPaginatedInstallations(jwtToken)
        assertEquals(2, installations.size)
        assertEquals(installationsPage1, installations)
    }

    @Test
    fun `test getPaginatedInstallations with multiple pages`() {
        val jwtToken = "test-jwt-token"
        val perPage = 30
        val totalInstallations = 100
        val installations = (1..totalInstallations).map {
            Installation(it.toLong(), Account("test-account-$it"), permissions, events)
        }
        val pages = installations.chunked(perPage)

        every { gitHubClient.getInstallations(any(), any(), any()) } returnsMany pages + listOf(emptyList())

        val result = gitHubAPIService.getPaginatedInstallations(jwtToken)
        assertEquals(totalInstallations, result.size)
        assertEquals(installations, result)
    }

    @Test
    fun `test getPaginatedOrganizations with one page`() {
        val installationToken = "test-installation-token"
        val organizationsPage1 = listOf(
            Organization("org-1", 1),
            Organization("org-2", 2)
        )

        every { gitHubClient.getOrganizations(any(), any(), any()) } returns organizationsPage1

        val organizations = gitHubAPIService.getPaginatedOrganizations(installationToken)
        assertEquals(2, organizations.size)
        assertEquals(organizationsPage1, organizations)
    }

    @Test
    fun `test getPaginatedOrganizations with multiple pages`() {
        val installationToken = "test-installation-token"
        val perPage = 30
        val totalOrganizations = 100
        val organizations = (1..totalOrganizations).map { Organization("org-$it", it) }
        val pages = organizations.chunked(perPage)

        every { gitHubClient.getOrganizations(any(), any(), any()) } returnsMany pages + listOf(emptyList())

        val result = gitHubAPIService.getPaginatedOrganizations(installationToken)
        assertEquals(totalOrganizations, result.size)
        assertEquals(organizations, result)
    }
}
