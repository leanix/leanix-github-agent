package net.leanix.githubagent.services

import io.mockk.every
import io.mockk.mockk
import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.dto.GitHubAppResponse
import net.leanix.githubagent.exceptions.GitHubAppInsufficientPermissionsException
import net.leanix.githubagent.exceptions.UnableToConnectToGitHubEnterpriseException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class GitHubEnterpriseServiceTest {

    private val githubClient = mockk<GitHubClient>()
    private val service = GitHubEnterpriseService(githubClient)

    @Test
    fun `verifyJwt with valid jwt should not throw exception`() {
        val jwt = "validJwt"
        val githubApp = GitHubAppResponse(
            slug = "validApp",
            permissions = mapOf("administration" to "read", "contents" to "read", "metadata" to "read"),
            events = listOf("label", "public", "repository", "push", "installation")
        )
        every { githubClient.getApp(any()) } returns githubApp

        assertDoesNotThrow { service.verifyJwt(jwt) }
    }

    @Test
    fun `verifyJwt with invalid jwt should throw exception`() {
        val jwt = "invalidJwt"
        every { githubClient.getApp(any()) } throws Exception()

        assertThrows(UnableToConnectToGitHubEnterpriseException::class.java) { service.verifyJwt(jwt) }
    }

    @Test
    fun `validateGithubAppResponse with correct permissions should not throw exception`() {
        val response = GitHubAppResponse(
            slug = "validApp",
            permissions = mapOf("administration" to "read", "contents" to "read", "metadata" to "read"),
            events = listOf("label", "public", "repository", "push", "installation")
        )

        assertDoesNotThrow { service.validateGithubAppResponse(response) }
    }

    @Test
    fun `validateGithubAppResponse with missing permissions should throw exception`() {
        val response = GitHubAppResponse(
            slug = "validApp",
            permissions = mapOf("administration" to "read", "contents" to "read"),
            events = listOf("label", "public", "repository")
        )

        assertThrows(
            GitHubAppInsufficientPermissionsException::class.java
        ) { service.validateGithubAppResponse(response) }
    }

    @Test
    fun `validateGithubAppResponse with missing events should throw exception`() {
        val response = GitHubAppResponse(
            slug = "validApp",
            permissions = mapOf("administration" to "read", "contents" to "read", "metadata" to "read"),
            events = listOf("label", "public")
        )

        assertThrows(
            GitHubAppInsufficientPermissionsException::class.java
        ) { service.validateGithubAppResponse(response) }
    }
}
